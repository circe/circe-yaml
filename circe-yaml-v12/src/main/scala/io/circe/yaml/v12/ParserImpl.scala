/*
 * Copyright 2016 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.yaml.v12

import cats.data.ValidatedNel
import cats.syntax.either._
import io.circe._
import io.circe.yaml.common
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.composer.Composer
import org.snakeyaml.engine.v2.constructor.StandardConstructor
import org.snakeyaml.engine.v2.nodes._
import org.snakeyaml.engine.v2.scanner.StreamReader

import java.io.Reader
import java.io.StringReader
import java.util.Optional
import scala.collection.JavaConverters._

class ParserImpl(settings: LoadSettings) extends common.Parser {

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
    parsed <- parseSingle(yaml)
    json <- yamlToJson(parsed)
  } yield json

  def parse(yaml: String): Either[ParsingFailure, Json] =
    parse(new StringReader(yaml))

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = parseStream(yaml) match {
    case Left(error)   => Stream(Left(error))
    case Right(stream) => stream.map(yamlToJson)
  }
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(yaml))

  private[this] def asScala[T](ot: Optional[T]): Option[T] =
    if (ot.isPresent) Some(ot.get()) else None

  private[this] def createComposer(reader: Reader) =
    new Composer(settings, new org.snakeyaml.engine.v2.parser.ParserImpl(settings, new StreamReader(settings, reader)))

  private[this] def parseSingle(reader: Reader): Either[ParsingFailure, Node] =
    Either.catchNonFatal {
      val composer = createComposer(reader)
      asScala(composer.getSingleNode)
    } match {
      case Left(err)          => Left(ParsingFailure(err.getMessage, err))
      case Right(None)        => Left(ParsingFailure("no document found", new RuntimeException("no document found")))
      case Right(Some(value)) => Right(value)
    }

  private[this] def parseStream(reader: Reader): Either[ParsingFailure, Stream[Node]] =
    Either.catchNonFatal(createComposer(reader).asScala.toStream).leftMap(err => ParsingFailure(err.getMessage, err))

  final def decode[A: Decoder](input: Reader): Either[Error, A] =
    finishDecode(parse(input))

  final def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[Error, A] =
    finishDecodeAccumulating(parse(input))

  private[this] object CustomTag {
    def unapply(tag: Tag): Option[String] = if (!tag.getValue.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  private[this] class FlatteningConstructor(settings: LoadSettings) extends StandardConstructor(settings) {
    def flatten(node: MappingNode): MappingNode = {
      flattenMapping(node)
      node
    }

    def construct(node: ScalarNode): Object = super.construct(node) // to make the method public
  }

  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = {
    // Isn't thread-safe internally, may hence not be shared
    val flattener: FlatteningConstructor = new FlatteningConstructor(settings)

    def convertScalarNode(node: ScalarNode) = Either
      .catchNonFatal(node.getTag match {
        case Tag.INT if node.getValue.startsWith("0x") || node.getValue.contains("_") =>
          Json.fromJsonNumber(flattener.construct(node) match {
            case int: Integer                 => JsonLong(int.toLong)
            case long: java.lang.Long         => JsonLong(long)
            case bigint: java.math.BigInteger =>
              JsonDecimal(bigint.toString)
            case other => throw new NumberFormatException(s"Unexpected number type: ${other.getClass}")
          })
        case Tag.INT | Tag.FLOAT =>
          JsonNumber.fromString(node.getValue).map(Json.fromJsonNumber).getOrElse {
            throw new NumberFormatException(s"Invalid numeric string ${node.getValue}")
          }
        case Tag.BOOL =>
          Json.fromBoolean(flattener.construct(node) match {
            case b: java.lang.Boolean => b
            case _                    => throw new IllegalArgumentException(s"Invalid boolean string ${node.getValue}")
          })
        case Tag.NULL         => Json.Null
        case CustomTag(other) =>
          Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.getValue)))
        case _ => Json.fromString(node.getValue)
      })
      .leftMap { err =>
        ParsingFailure(err.getMessage, err)
      }

    def convertKeyNode(node: Node) = node match {
      case scalar: ScalarNode => Right(scalar.getValue)
      case _                  => Left(ParsingFailure("Only string keys can be represented in JSON", null))
    }

    if (node == null) {
      Right(Json.False)
    } else {
      node match {
        case mapping: MappingNode =>
          flattener
            .flatten(mapping)
            .getValue
            .asScala
            .foldLeft(
              Either.right[ParsingFailure, JsonObject](JsonObject.empty)
            ) { (objEither, tup) =>
              for {
                obj <- objEither
                key <- convertKeyNode(tup.getKeyNode)
                value <- yamlToJson(tup.getValueNode)
              } yield obj.add(key, value)
            }
            .map(Json.fromJsonObject)
        case sequence: SequenceNode =>
          sequence.getValue.asScala
            .foldLeft(Either.right[ParsingFailure, List[Json]](List.empty[Json])) { (arrEither, node) =>
              for {
                arr <- arrEither
                value <- yamlToJson(node)
              } yield value :: arr
            }
            .map(arr => Json.fromValues(arr.reverse))
        case scalar: ScalarNode => convertScalarNode(scalar)
      }
    }
  }
}
