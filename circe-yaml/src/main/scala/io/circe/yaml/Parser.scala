package io.circe.yaml

import Parser._
import cats.data.ValidatedNel
import cats.syntax.either._
import io.circe._
import java.io.{ Reader, StringReader }
import org.yaml.snakeyaml.{ LoaderOptions, Yaml }
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._

final case class Parser(
  maxAliasesForCollections: Int = Parser.defaultMaxAliasesForCollections,
  nestingDepthLimit: Int = Parser.defaultNestingDepthLimit,
  codePointLimit: Int = Parser.defaultCodePointLimit
) extends yaml.common.Parser {

  private val loaderOptions = {
    val options = new LoaderOptions()
    options.setMaxAliasesForCollections(maxAliasesForCollections)
    options.setNestingDepthLimit(nestingDepthLimit)
    options.setCodePointLimit(codePointLimit)
    options
  }

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
    parsed <- parseSingle(yaml)
    json <- yamlToJson(parsed, loaderOptions)
  } yield json

  def parse(yaml: String): Either[ParsingFailure, Json] = parse(new StringReader(yaml))

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] =
    parseStream(yaml).map(node => yamlToJson(node, loaderOptions))
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(yaml))

  private[this] def parseSingle(reader: Reader): Either[ParsingFailure, Node] =
    Either.catchNonFatal(new Yaml(loaderOptions).compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  private[this] def parseStream(reader: Reader): Stream[Node] =
    new Yaml(loaderOptions).composeAll(reader).asScala.toStream

  def copy(
    maxAliasesForCollections: Int = this.maxAliasesForCollections,
    nestingDepthLimit: Int = this.nestingDepthLimit,
    codePointLimit: Int = this.codePointLimit
  ): Parser = new Parser(maxAliasesForCollections, nestingDepthLimit, codePointLimit)

  def copy(maxAliasesForCollections: Int): Parser = new Parser(
    maxAliasesForCollections = maxAliasesForCollections,
    nestingDepthLimit = this.nestingDepthLimit,
    codePointLimit = this.codePointLimit
  )

  def this(maxAliasesForCollections: Int) =
    this(maxAliasesForCollections, Parser.defaultNestingDepthLimit, Parser.defaultCodePointLimit)

  final def decode[A: Decoder](input: Reader): Either[Error, A] =
    finishDecode(parse(input))

  final def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[Error, A] =
    finishDecodeAccumulating(parse(input))
}

object Parser {
  val defaultMaxAliasesForCollections: Int = 50 // to prevent YAML at
  // https://en.wikipedia.org/wiki/Billion_laughs_attack
  val defaultNestingDepthLimit: Int = 50
  val defaultCodePointLimit: Int = 3 * 1024 * 1024 // 3MB

  val default: Parser = Parser()

  def apply(maxAliasesForCollections: Int): Parser =
    new Parser(maxAliasesForCollections = maxAliasesForCollections)

  private[yaml] object CustomTag {
    def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  private[yaml] class FlatteningConstructor(val loaderOptions: LoaderOptions) extends SafeConstructor(loaderOptions) {
    def flatten(node: MappingNode): MappingNode = {
      flattenMapping(node)
      node
    }

    def construct(node: ScalarNode): Object =
      getConstructor(node).construct(node)
  }

  private[yaml] def yamlToJson(node: Node, loaderOptions: LoaderOptions): Either[ParsingFailure, Json] = {
    // Isn't thread-safe internally, may hence not be shared
    val flattener: FlatteningConstructor = new FlatteningConstructor(loaderOptions)

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
                value <- yamlToJson(tup.getValueNode, loaderOptions)
              } yield obj.add(key, value)
            }
            .map(Json.fromJsonObject)
        case sequence: SequenceNode =>
          sequence.getValue.asScala
            .foldLeft(Either.right[ParsingFailure, List[Json]](List.empty[Json])) { (arrEither, node) =>
              for {
                arr <- arrEither
                value <- yamlToJson(node, loaderOptions)
              } yield value :: arr
            }
            .map(arr => Json.fromValues(arr.reverse))
        case scalar: ScalarNode => convertScalarNode(scalar)
      }
    }
  }
}
