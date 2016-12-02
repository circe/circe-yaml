package io.circe

import scala.collection.JavaConverters._

import cats.implicits._
import org.yaml.snakeyaml.nodes._

package object yaml {

  private[this] def scalarNode(tag: Tag, value: String) = new ScalarNode(tag, value, null, null, '\0')

  private[circe] def yamlTag(json: Json) = json.fold(
    Tag.NULL,
    _ => Tag.BOOL,
    number =>
      number match {
        case JsonDecimal(_) | JsonBigDecimal(_) | JsonBiggerDecimal(_) | JsonDouble(_) =>
          Tag.FLOAT
        case JsonLong(_) =>
          Tag.INT
      },
    _ => Tag.STR,
    _ => Tag.SEQ,
    _ => Tag.MAP
  )

  private[circe] def jsonToYaml(json: Json, dropNullKeys: Boolean = false): Node = {

    def convertObject(obj: JsonObject) = {
      val map = if(dropNullKeys)
        obj.filter(!_._2.isNull).toMap
      else
        obj.toMap
      new MappingNode(Tag.MAP, map.map {
        case (k, v) => new NodeTuple(scalarNode(Tag.STR, k), jsonToYaml(v, dropNullKeys))
      }.toList.asJava, false)
    }

    json.fold(
      scalarNode(Tag.NULL, "null"),
      bool =>
        scalarNode(Tag.BOOL, bool.toString),
      number =>
        number match {
          case JsonDecimal(_) | JsonBigDecimal(_) | JsonBiggerDecimal(_) | JsonDouble(_) =>
            scalarNode(Tag.FLOAT, number.toString)
          case JsonLong(_) =>
            scalarNode(Tag.INT, number.toString)
        },
      str =>
        scalarNode(Tag.STR, str),
      arr =>
        new SequenceNode(Tag.SEQ, arr.map(j => jsonToYaml(j, dropNullKeys)).asJava, false),
      obj =>
        convertObject(obj)
    )
  }

  private[circe] def yamlToJson(node: Node): ParsingFailure Either Json = {

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

    def convertScalarNode(node: ScalarNode) = Either.catchNonFatal(node.getTag match {
      case Tag.INT => Json.fromJsonNumber(JsonNumber.fromIntegralStringUnsafe(node.getValue))
      case Tag.FLOAT => Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(node.getValue))
      case Tag.BOOL => Json.fromBoolean(node.getValue.toBoolean)
      case Tag.NULL => Json.Null
      case CustomTag(other) =>
        Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.getValue)))
      case other => Json.fromString(node.getValue)
    }).leftMap {
      err => ParsingFailure(err.getMessage, err)
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
