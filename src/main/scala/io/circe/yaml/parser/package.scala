package io.circe.yaml

import cats.syntax.either._
import io.circe._
import java.io.{ Reader, StringReader }
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._

package object parser {

  /**
   * A wrapper class to allow customization of the underlying SnakeYAML parser options.
   *
   * @param loaderOptions
   */
  case class ParserConfig(loaderOptions: LoaderOptions = new LoaderOptions)

  /**
   * The default parser config using the same default options used by SnakeYAML.
   */
  implicit val defaultParserConfig: ParserConfig = ParserConfig()

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader)(implicit parserConfig: ParserConfig): Either[ParsingFailure, Json] = for {
    parsed <- parseSingle(yaml)(parserConfig)
    json <- yamlToJson(parsed)
  } yield json

  def parse(yaml: String)(implicit parserConfig: ParserConfig): Either[ParsingFailure, Json] =
    parse(new StringReader(yaml))(parserConfig)

  def parseDocuments(yaml: Reader)(implicit parserConfig: ParserConfig): Stream[Either[ParsingFailure, Json]] =
    parseStream(yaml)(parserConfig).map(yamlToJson)

  def parseDocuments(yaml: String)(implicit parserConfig: ParserConfig): Stream[Either[ParsingFailure, Json]] =
    parseDocuments(new StringReader(yaml))(parserConfig)

  private[this] def parseSingle(reader: Reader)(implicit parserConfig: ParserConfig) =
    Either
      .catchNonFatal(new Yaml(parserConfig.loaderOptions).compose(reader))
      .leftMap(err => ParsingFailure(err.getMessage, err))

  private[this] def parseStream(reader: Reader)(implicit parserConfig: ParserConfig) =
    new Yaml(parserConfig.loaderOptions).composeAll(reader).asScala.toStream

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

    def construct(node: ScalarNode): Object =
      getConstructor(node).construct(node)
  }

  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = {
    // Isn't thread-safe internally, may hence not be shared
    val flattener: FlatteningConstructor = new FlatteningConstructor

    def convertScalarNode(node: ScalarNode) = Either
      .catchNonFatal(node.getTag match {
        case Tag.INT if node.getValue.startsWith("0x") || node.getValue.contains("_") =>
          Json.fromJsonNumber(flattener.construct(node) match {
            case int: Integer         => JsonLong(int.toLong)
            case long: java.lang.Long => JsonLong(long)
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
        case Tag.NULL => Json.Null
        case CustomTag(other) =>
          Json.fromJsonObject(JsonObject.singleton(other.stripPrefix("!"), Json.fromString(node.getValue)))
        case other => Json.fromString(node.getValue)
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
