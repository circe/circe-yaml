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

package io.circe.yaml.scalayaml

import cats.data.ValidatedNel
import cats.syntax.all._
import io.circe
import io.circe.Decoder
import io.circe.Json
import io.circe.JsonNumber
import io.circe.ParsingFailure
import org.virtuslab.yaml._

import java.io.Reader
import scala.collection.mutable
import scala.util.Try

object Parser extends io.circe.yaml.common.Parser {

  private def readerToString(yaml: Reader): String = {
    val buffer = new Array[Char](4 * 1024)
    val builder = new mutable.StringBuilder(4 * 1024)
    var readBytes = -1
    while ({ readBytes = yaml.read(buffer); readBytes } > 0)
      builder.appendAll(buffer, 0, readBytes)
    builder.result()
  }

  override def parse(yaml: Reader): Either[ParsingFailure, Json] = {
    val string = readerToString(yaml)
    parse(string)
  }

  override def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = {
    val string = readerToString(yaml)
    val parsed = parse(string)
    Stream(parsed)
  }

  override def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = {
    val parsed = parse(yaml)
    Stream(parsed)
  }

  override def decode[A: Decoder](input: Reader): Either[circe.Error, A] = {
    val string = readerToString(input)
    val parsed = parse(string)
    parsed.flatMap(_.as[A])
  }

  override def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[circe.Error, A] =
    decode(input).toValidatedNel

  override def parse(input: String): Either[ParsingFailure, Json] = {
    val node = input.asNode
    node match {
      case Right(node) => nodeToJson(node)
      case Left(error) => Left(errorToFailure(error))
    }
  }

  private def nodeToJson(node: Node): Either[ParsingFailure, Json] = node match {
    case Node.ScalarNode(value, tag) =>
      tag.value match {
        case "tag:yaml.org,2002:str" =>
          Right(Json.fromString(value))
        case "tag:yaml.org,2002:null" =>
          value match {
            case "null" => Right(Json.Null)
            case _      => Left(ParsingFailure(s"Expected 'null', but got $value.", null))
          }
        case "tag:yaml.org,2002:bool" =>
          value.toBooleanOption.map(Json.fromBoolean).toRight(ParsingFailure(s"Can't parse '$value' as bool.", null))
        case "tag:yaml.org,2002:int" =>
          Try(java.lang.Long.decode(value.replaceAll("_", ""))).toEither
            .map(l => Json.fromLong(l))
            .left
            .map(ParsingFailure(s"Can't parse '$value' as int.", _))
        case "tag:yaml.org,2002:float" =>
          Try(java.lang.Double.parseDouble(value.replaceAll("_", ""))).toEither
            .flatMap(d => Json.fromDouble(d).toRight(new Exception("Argument cannot be represented as a JSON number.")))
            .left
            .map(ParsingFailure(s"Can't parse '$value' as float.", _))
        case _ =>
          val nil = Option.when(value == "null")(Json.Null)
          val num = JsonNumber.fromString(value).map(Json.fromJsonNumber)
          val bol = value.toBooleanOption.map(Json.fromBoolean)
          val str = Json.fromString(value)
          val result = nil.orElse(num).orElse(bol).getOrElse(str)
          Right(result)
      }
    case Node.SequenceNode(nodes, _) =>
      val values = nodes.traverse(nodeToJson)
      values.map(Json.fromValues)
    case Node.MappingNode(mappings, _) =>
      val fields = mappings.toList.traverse {
        case (Node.ScalarNode(key, _), value) =>
          nodeToJson(value).map(key -> _)
        case (node, _) =>
          Left(ParsingFailure(s"Unexpected ${node.getClass.getSimpleName} type, expected ScalarNode.", null))
      }
      fields.map(Json.fromFields)
  }

  private def errorToFailure(error: YamlError): ParsingFailure =
    ParsingFailure(s"${error.getClass.getSimpleName}: ${error.msg}", null)
}
