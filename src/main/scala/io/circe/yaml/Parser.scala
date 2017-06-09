package io.circe.yaml

import cats.Eval
import cats.data.EitherT
import cats.instances.list._
import cats.instances.vector._
import cats.syntax.either._
import cats.syntax.traverse._
import io.circe.{Json, JsonNumber, ParsingFailure}
import java.io.{Reader, StringReader}
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.{MappingNode, Node, ScalarNode, SequenceNode, Tag}
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

final case class Parser(
  useBoolLit: Boolean = true,
  useFloatLit: Boolean = true,
  useIntLit: Boolean = true,
  useTimestampLit: Boolean = true,
  useMergeLit: Boolean = true
) {

  /**
    * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
    * @param yaml
    * @return
    */
  def parse(reader: Reader): Either[ParsingFailure, Json] = {
    val yaml = Parser.createYaml

    parseSingle(yaml)(reader).flatMap(node => convertNode(yaml)(node).value.value)
  }

  def parseDocuments(reader: Reader): Stream[Either[ParsingFailure, Json]] = {
    val yaml = Parser.createYaml

    parseStream(yaml)(reader).map(_.flatMap(node => convertNode(yaml)(node).value.value))
  }

  def parse(input: String): Either[ParsingFailure, Json] = parse(new StringReader(input))
  def parseDocuments(input: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(input))

  private[this] def parseSingle(yaml: Parser.CirceYaml)(reader: Reader): Either[ParsingFailure, Node] = try {
    Right(yaml.compose(reader))
  } catch {
    case NonFatal(err) => Left(ParsingFailure(err.getMessage, err))
  }

  private[this] def parseStream(yaml: Parser.CirceYaml)(reader: Reader): Stream[Either[ParsingFailure, Node]] = {
    val iterator = yaml.composeAll(reader).iterator

    new Iterator[Either[ParsingFailure, Node]] {
      def hasNext: Boolean = iterator.hasNext
      def next(): Either[ParsingFailure, Node] = try Right(iterator.next()) catch {
        case NonFatal(err) => Left(ParsingFailure(err.getMessage, err))
      }
    }.toStream
  }

  private[this] def parseBool(input: String): Either[ParsingFailure, Json] =
    if (input == "true") Right(Json.True) else if (input == "false") Right(Json.False) else Left(
      ParsingFailure(s"Invalid bool: $input", null)
    )

  private[this] def parseNumber(input: String): Either[ParsingFailure, Json] = JsonNumber.fromString(input) match {
    case Some(value) => Right(Json.fromJsonNumber(value))
    case None        => Left(ParsingFailure(s"Invalid number: $input", null))
  }

  private[this] def convertScalarNode(yaml: Parser.CirceYaml)(node: ScalarNode): Either[ParsingFailure, Json] =
    node.getTag match {
      case Tag.BOOL      if useBoolLit      => yaml.boolLit(node)
      case Tag.FLOAT     if useFloatLit     => yaml.floatLit(node)
      case Tag.INT       if useIntLit       => yaml.intLit(node)
      case Tag.TIMESTAMP if useTimestampLit => yaml.timestampLit(node)
      case Tag.BOOL                         => parseBool(node.getValue)
      case Tag.FLOAT | Tag.INT              => parseNumber(node.getValue)
      case Tag.NULL                         => Right(Json.Null)
      case Parser.CustomTag(other)          => Right(Json.obj(other.stripPrefix("!") -> Json.fromString(node.getValue)))
      case other                            => Right(Json.fromString(node.getValue))
    }

  private[this] def convertKeyNode(node: Node): Either[ParsingFailure, String] = node match {
    case scalar: ScalarNode => Right(scalar.getValue)
    case _                  => Left(ParsingFailure("Only string keys can be represented in JSON", null))
  }

  private[this] def convertNode(yaml: Parser.CirceYaml)(node: Node): EitherT[Eval, ParsingFailure, Json] =
    node match {
      case scalar: ScalarNode => EitherT(Eval.now(convertScalarNode(yaml)(scalar)))
      case sequence: SequenceNode =>
        sequence.getValue.iterator.asScala.toVector.traverseU(convertNode(yaml)).map(Json.fromValues)
      case mapping: MappingNode =>
        val m = if (useMergeLit) yaml.mergeNodes(mapping) else mapping

        m.getValue.iterator.asScala.toList.traverseU { pair =>
          for {
            k <- EitherT(Eval.now(convertKeyNode(pair.getKeyNode)))
            v <- convertNode(yaml)(pair.getValueNode)
          } yield (k, v)
        }.map(Json.fromFields)
    }
}

object Parser {
  private[yaml] object CustomTag {
    def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  private[circe] def createYaml: Parser.CirceYaml = new Parser.CirceYaml(new CirceConstructor)

  private[this] final class CirceConstructor extends SafeConstructor {
    def constructNode(node: Node): AnyRef = constructObject(node)
    def flattenNode(node: MappingNode): MappingNode = {
      flattenMapping(node)
      node
    }
  }

  private[yaml] final class CirceYaml(circeConstructor: CirceConstructor) extends Yaml(circeConstructor) {
    def mergeNodes(node: MappingNode): MappingNode = {
      circeConstructor.flattenNode(node)
      node
    }

    def boolLit(node: ScalarNode): Either[ParsingFailure, Json] = try {
      Right(Json.fromBoolean(circeConstructor.constructNode(node).asInstanceOf[java.lang.Boolean]))
    } catch {
      case err: ClassCastException => Left(ParsingFailure("Expected int YAML node", err))
    }

    def floatLit(node: ScalarNode): Either[ParsingFailure, Json] = try {
      Right(Json.fromDoubleOrString(circeConstructor.constructNode(node).asInstanceOf[java.lang.Double]))
    } catch {
      case err: ClassCastException => Left(ParsingFailure("Expected double YAML node", err))
    }

    def intLit(node: ScalarNode): Either[ParsingFailure, Json] = try {
      circeConstructor.constructNode(node) match {
        case value: java.lang.Integer    => Right(Json.fromInt(value))
        case value: java.lang.Long       => Right(Json.fromLong(value))
        case value: java.math.BigInteger => Right(Json.fromBigInt(new BigInt(value)))
      }
    } catch {
      case err: ClassCastException    => Left(ParsingFailure("Expected bool YAML node", err))
      case err: NumberFormatException => Left(ParsingFailure(err.getMessage, err))
    }

    def timestampLit(node: ScalarNode): Either[ParsingFailure, Json] = try {
      Right(Json.fromLong(circeConstructor.constructNode(node).asInstanceOf[java.util.Date].getTime))
    } catch {
      case err: ClassCastException => Left(ParsingFailure("Expected timestamp YAML node", err))
    }
  }
}
