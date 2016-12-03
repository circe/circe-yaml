package io.circe.yaml

import java.io.{Reader, StringReader}

import scala.collection.JavaConverters._

import cats.syntax.either._
import io.circe.numbers.BiggerDecimal
import io.circe._
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes._

package object parser {


  /**
    * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
    * @param yaml
    * @return
    */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
    parsed <- parseSingle(yaml)
    json   <- yamlToJson(parsed)
  } yield json

  def parse(yaml: String): Either[ParsingFailure, Json] = parse(new StringReader(yaml))

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = parseStream(yaml).map(yamlToJson)
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(yaml))

  private[this] def parseSingle(reader: Reader) =
    Either.catchNonFatal(new Yaml().compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  private[this] def parseStream(reader: Reader) =
    new Yaml().composeAll(reader).asScala.toStream

  private[this] object CustomTag {
    def unapply(tag: Tag) = if(!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = {

    def convertScalarNode(node: ScalarNode) = Either.catchNonFatal(node.getTag match {
      case Tag.INT | Tag.FLOAT => JsonNumber.fromString(node.getValue).map(Json.fromJsonNumber).getOrElse {
        throw new NumberFormatException(s"Invalid numeric string ${node.getValue}")
      }
      case Tag.BOOL => Json.fromBoolean(node.getValue.toBoolean)
      case Tag.NULL => Json.Null
      case CustomTag(other) =>
        Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.getValue)))
      case other => Json.fromString(node.getValue)
    }).leftMap {
      err =>
        ParsingFailure(err.getMessage, err)
    }

    def convertKeyNode(node: Node) = node match {
      case scalar: ScalarNode => Right(scalar.getValue)
      case _ => Left(ParsingFailure("Only string keys can be represented in JSON", null))
    }

    node match {
      case mapping: MappingNode =>
        mapping.getValue.asScala.foldLeft(Either.right[ParsingFailure, JsonObject](JsonObject.empty)) {
          (objEither, tup) => for {
            obj <- objEither
            key <- convertKeyNode(tup.getKeyNode)
            value <- yamlToJson(tup.getValueNode)
          } yield obj.add(key, value)
        }.map(Json.fromJsonObject)
      case sequence: SequenceNode =>
        sequence.getValue.asScala.foldLeft(Either.right[ParsingFailure, List[Json]](List.empty[Json])) {
          (arrEither, node) => for {
            arr <- arrEither
            value <- yamlToJson(node)
          } yield value :: arr
        }.map(arr => Json.fromValues(arr.reverse))
      case scalar: ScalarNode => convertScalarNode(scalar)
    }
  }

}
