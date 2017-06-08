package io.circe.yaml

import cats.instances.either._
import cats.instances.vector._
import cats.syntax.either._
import cats.syntax.traverse._
import io.circe._
import java.io.{Reader, StringReader}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._

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
    def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  private[this] class FlatteningConstructor extends SafeConstructor {
    def flatten(node: MappingNode): MappingNode = {
      flattenMapping(node)
      node
    }
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
        mapping.getValue.asScala.foldLeft(
          Either.right[ParsingFailure, JsonObject](JsonObject.empty)
        ) {
          (objEither, tup) => for {
            obj    <- objEither
            key    <- convertKeyNode(tup.getKeyNode)
            value   = tup.getValueNode
            result <- tup.getKeyNode.getTag match {
              case Tag.MERGE => mergeNode(obj, value)
              case _ => yamlToJson(value).map(obj.add(key, _))
            }
          } yield result
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

  private[this] def mergeNode(obj: JsonObject, merge: Node) = yamlToJson(merge).flatMap {
    valueJson => valueJson.asArray.map {
      arr =>
        arr.map(json => Either.fromOption(json.asObject, ParsingFailure("Only mappings can be merged", null)))
          .sequenceU
          .map {
            objs => objs.foldLeft(obj)(mergeObject)
          }
    } orElse valueJson.asObject.map {
      valueObj => Either.right(mergeObject(obj, valueObj))
    } getOrElse Either.left(ParsingFailure("Only mappings can be merged", null))
  }

  private[this] def mergeObject(obj: JsonObject, valueObj: JsonObject) = {
    JsonObject.fromMap(valueObj.toMap ++ obj.toMap)
  }

}
