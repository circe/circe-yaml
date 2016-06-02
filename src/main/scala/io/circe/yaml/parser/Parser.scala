package io.circe.yaml.parser

import java.io.{Reader, StringReader}

import scala.collection.JavaConverters._
import cats.data.Xor
import Xor._
import io.circe.{Json, JsonNumber, JsonObject, ParsingFailure}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes._

object Parser {

  object ScalarTag {
    def unapply(tag: Tag) = if(tag.startsWith(Tag.PREFIX))
      Some(tag.getClassName)
    else
      None
  }

  object CustomTag {
    def unapply(tag: Tag) = if(!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  // Typecasing.  Because Java :(
  private def convertYamlNode(node: Node): Xor[ParsingFailure, Json] = node match {
    case anchor: AnchorNode => convertYamlNode(anchor.getRealNode)
    case mapping: MappingNode =>
      mapping.getValue.asScala.foldLeft(Right(JsonObject.empty) : Xor[ParsingFailure, JsonObject]) {
        (objXor, tup) => for {
          obj <- objXor
          key <- convertKeyNode(tup.getKeyNode)
          value <- convertYamlNode(tup.getValueNode)
        } yield obj.add(key, value)
      }.map(Json.fromJsonObject)
    case sequence: SequenceNode =>
      sequence.getValue.asScala.foldLeft(Right(List.empty[Json]) : Xor[ParsingFailure, List[Json]]) {
        (arrXor, node) => for {
          arr <- arrXor
          value <- convertYamlNode(node)
        } yield value :: arr
      }.map(arr => Json.fromValues(arr.reverse))
    case scalar: ScalarNode => convertScalarNode(scalar)
  }

  private def convertKeyNode(node: Node): Xor[ParsingFailure, String] = node match {
    case scalar: ScalarNode => Right(scalar.getValue)
    case _ => Left(ParsingFailure("Only string keys can be represented in JSON", null))
  }

  private def convertScalarNode(node: ScalarNode) = Xor.catchNonFatal(node.getTag match {
    case ScalarTag("int") => Json.fromJsonNumber(JsonNumber.unsafeIntegral(node.getValue))
    case ScalarTag("float") => Json.fromJsonNumber(JsonNumber.unsafeDecimal(node.getValue))
    case ScalarTag("bool") => Json.fromBoolean(node.getValue.toBoolean)
    case ScalarTag("null") => Json.Null
    case CustomTag(other) =>
      Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.getValue)))
    case other => Json.fromString(node.getValue)
  }).leftMap {
    err => ParsingFailure(err.getMessage, err)
  }

  private def parseYaml(reader: Reader) = for {
    node <- Xor.catchNonFatal(new Yaml().compose(reader))
    json <- convertYamlNode(node)
  } yield json

  private def parseYamlStream(reader: Reader) =
    new Yaml().composeAll(reader).asScala.toStream.map(convertYamlNode)

  def parse(reader: Reader) : Xor[ParsingFailure, Json] = parseYaml(reader).leftMap {
    case err @ ParsingFailure(msg, underlying) => err
    case err => ParsingFailure(err.getMessage, err)
  }

  def parse(string: String) : Xor[ParsingFailure, Json] = parse(new StringReader(string))

  def parseDocuments(string: String) : Stream[Xor[ParsingFailure, Json]] = parseDocuments(new StringReader(string))
  def parseDocuments(reader: Reader) : Stream[Xor[ParsingFailure, Json]] = parseYamlStream(reader)

}
