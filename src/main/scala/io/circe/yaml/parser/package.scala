package io.circe.yaml

import cats.syntax.either._
import io.circe._
import java.io.{Reader, StringReader}
import java.util.Date
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._
import scala.util.Try

package object parser {


  /**
    * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
    * @param yaml
    * @return
    */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
    parsed <- parseSingle(yaml)
    json   <- yamlToJson(new Flattener)(parsed)
  } yield json

  def parse(yaml: String): Either[ParsingFailure, Json] = parse(new StringReader(yaml))

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] =
    parseStream(yaml).map(yamlToJson(new Flattener))
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(yaml))

  private[this] def parseSingle(reader: Reader) =
    Either.catchNonFatal(new Yaml().compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  private[this] def parseStream(reader: Reader) =
    new Yaml().composeAll(reader).asScala.toStream

  private[this] object CustomTag {
    def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  private[this] class Flattener extends SafeConstructor {
    def flatten(node: MappingNode): MappingNode = {
      flattenMapping(node)
      node
    }

    def toDate(node: ScalarNode): Try[Date] = Try(constructObject(node).asInstanceOf[Date])
  }

  private[this] val timestampConstructor: SafeConstructor.ConstructYamlTimestamp =
    new SafeConstructor.ConstructYamlTimestamp()


  private[this] def yamlToJson(flattener: Flattener)(node: Node): Either[ParsingFailure, Json] = {

    def convertScalarNode(node: ScalarNode) = Either.catchNonFatal(node.getTag match {
      case Tag.INT | Tag.FLOAT => JsonNumber.fromString(node.getValue).map(Json.fromJsonNumber).getOrElse {
        throw new NumberFormatException(s"Invalid numeric string ${node.getValue}")
      }
      case Tag.BOOL => Json.fromBoolean(node.getValue.toBoolean)
      case Tag.NULL => Json.Null
      case Tag.TIMESTAMP =>
        flattener.toDate(node).map(date => Json.fromLong(date.getTime)).getOrElse(Json.fromString(node.getValue))
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
        flattener.flatten(mapping).getValue.asScala.foldLeft(
          Either.right[ParsingFailure, JsonObject](JsonObject.empty)
        ) {
          (objEither, tup) => for {
            obj <- objEither
            key <- convertKeyNode(tup.getKeyNode)
            value <- yamlToJson(flattener)(tup.getValueNode)
          } yield obj.add(key, value)
        }.map(Json.fromJsonObject)
      case sequence: SequenceNode =>
        sequence.getValue.asScala.foldLeft(Either.right[ParsingFailure, List[Json]](List.empty[Json])) {
          (arrEither, node) => for {
            arr <- arrEither
            value <- yamlToJson(flattener)(node)
          } yield value :: arr
        }.map(arr => Json.fromValues(arr.reverse))
      case scalar: ScalarNode => convertScalarNode(scalar)
    }
  }

}
