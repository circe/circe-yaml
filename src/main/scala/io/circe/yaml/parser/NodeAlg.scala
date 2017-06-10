package io.circe.yaml.parser

import cats.data.ValidatedNel
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import io.circe.{Json, JsonNumber, JsonObject, ParsingFailure}
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._
import scala.collection.immutable.Queue

abstract class NodeAlg[T] {
  def int(node: ScalarNode): T
  def float(node: ScalarNode): T
  def timestamp(node: ScalarNode): T
  def bool(node: ScalarNode): T
  def yNull(node: ScalarNode): T
  def string(node: ScalarNode): T
  def otherScalar(node: ScalarNode): T
  
  def sequence(node: SequenceNode): T = fromValues {
    node.getValue.asScala.foldLeft(Queue.empty[T]) {
      (accum, next) => accum enqueue any(next)
    }
  }
  
  def mapping(node: MappingNode): T = fromFields {
    node.getValue.asScala.map {
      nodeTuple => nodeTuple.getKeyNode match {
        case keyNode: ScalarNode => keyNode.getValue -> any(nodeTuple.getValueNode)
        case _ => throw ParsingFailure("Only string keys can be represented in JSON", null)
      }
    }
  }

  def fromValues(ts: Iterable[T]): T
  def fromFields(ts: Iterable[(String, T)]): T

  final def any(node: Node): T = node match {
    case node: ScalarNode => node.getTag match {
      case Tag.INT       => int(node)
      case Tag.FLOAT     => float(node)
      case Tag.TIMESTAMP => timestamp(node)
      case Tag.BOOL      => bool(node)
      case Tag.NULL      => yNull(node)
      case Tag.STR       => string(node)
      case _             => otherScalar(node)
    }
    case node: SequenceNode => sequence(node)
    case node: MappingNode  => mapping(node)
  }
}

final class LiftedAlg[A](lifted: NodeAlg[A]) extends NodeAlg[Either[ParsingFailure, A]] {
  private def wrap(what: String)(err: Throwable) = ParsingFailure(s"Failed to parse $what", err)
  def int(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.int(node)).leftMap(wrap("integer value"))

  def float(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.float(node)).leftMap(wrap("float value"))

  def timestamp(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.timestamp(node)).leftMap(wrap("timestamp value"))

  def bool(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.bool(node)).leftMap(wrap("boolean value"))

  def yNull(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.yNull(node)).leftMap(wrap("null value"))

  def string(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.string(node)).leftMap(wrap("string value"))

  def otherScalar(node: ScalarNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.otherScalar(node)).leftMap(wrap("scalar value"))

  override def sequence(node: SequenceNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.sequence(node)).leftMap(wrap("sequence"))

  override def mapping(node: MappingNode): Either[ParsingFailure, A] =
    Either.catchNonFatal(lifted.mapping(node)).leftMap(wrap("mapping"))

  def fromValues(ts: Iterable[Either[ParsingFailure, A]]): Either[ParsingFailure, A] = try {
    Either.right {
      lifted.fromValues {
        ts.map(_.valueOr(throw _))
      }
    }
  } catch {
    case f @ ParsingFailure(_, _) => Either.left(f)
  }
  
  def fromFields(ts: Iterable[(String, Either[ParsingFailure, A])]): Either[ParsingFailure, A] = try {
    Either.right {
      lifted.fromFields {
        ts.map {
          case (key, value) => key -> value.valueOr(throw _)
        }
      }
    }
  } catch {
    case f @ ParsingFailure(_, _) => Either.left(f)
  }
}

final class AccumlatingAlg[A](base: NodeAlg[A]) extends NodeAlg[ValidatedNel[ParsingFailure, A]] {
  private val lifted = new LiftedAlg(base)
  def int(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.int(node).toValidatedNel
  def float(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.float(node).toValidatedNel
  def timestamp(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.timestamp(node).toValidatedNel
  def bool(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.bool(node).toValidatedNel
  def yNull(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.yNull(node).toValidatedNel
  def string(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.string(node).toValidatedNel
  def otherScalar(node: ScalarNode): ValidatedNel[ParsingFailure, A] = lifted.otherScalar(node).toValidatedNel

  def fromFields(ts: Iterable[(String, ValidatedNel[ParsingFailure, A])]): ValidatedNel[ParsingFailure, A] =
    ts.toList.traverseU {
      case (key, value) => value.map(key -> _)
    }.map(base.fromFields)

  def fromValues(ts: Iterable[ValidatedNel[ParsingFailure, A]]): ValidatedNel[ParsingFailure, A] =
    ts.toList.sequenceU.map(base.fromValues)
}

class DefaultAlg extends NodeAlg[Json] {
  protected object Constructor extends SafeConstructor {
    def flatten(node: MappingNode): Unit = flattenMapping(node)
  }

  final protected def number(str: String): Json = JsonNumber.fromString(str).map(Json.fromJsonNumber).getOrElse {
    throw new NumberFormatException(s"Invalid numeric string $str")
  }

  def int(node: ScalarNode): Json = number(node.getValue)
  def float(node: ScalarNode): Json = number(node.getValue)
  def timestamp(node: ScalarNode): Json = Json.fromString(node.getValue)
  def bool(node: ScalarNode): Json = Json.fromBoolean(node.getValue.toBoolean)
  def yNull(node: ScalarNode): Json = Json.Null
  def string(node: ScalarNode): Json = Json.fromString(node.getValue)
  def otherScalar(node: ScalarNode): Json = if (!node.getTag.startsWith(Tag.PREFIX)) {
    Json.fromJsonObject(JsonObject.singleton(node.getTag.getValue.stripPrefix("!"), Json.fromString(node.getValue)))
  } else Json.fromString(node.getValue)

  def fromValues(ts: Iterable[Json]): Json = Json.fromValues(ts)
  def fromFields(ts: Iterable[(String, Json)]): Json = Json.fromFields(ts)

  override def mapping(node: MappingNode): Json = {
    Constructor.flatten(node)
    super.mapping(node)
  }
}

case class ConfiguredAlg(
  numericTimestamps: Boolean
) extends DefaultAlg {
  final override def timestamp(node: ScalarNode): Json = if (!numericTimestamps) {
    super.timestamp(node)
  } else {
    val constructor = new SafeConstructor.ConstructYamlTimestamp()
    constructor.construct(node)
    Json.fromLong(constructor.getCalendar.getTimeInMillis)
  }
}