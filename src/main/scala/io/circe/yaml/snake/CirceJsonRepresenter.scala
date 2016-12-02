package io.circe.yaml.snake

import java.util.regex.Pattern

import io.circe.{Json, JsonDouble, JsonLong}
import io.circe.Json._
import io.circe.yaml.snake.CirceJsonRepresenter.RepresentCirceJson
import org.yaml.snakeyaml.nodes.{Node, Tag}
import org.yaml.snakeyaml.representer.{Represent, Representer, SafeRepresenter}

import scala.collection.JavaConverters._

/**
  * Represents [[Json]] as Yaml [[Node]]s.
  *
  * @note This extends [[Representer]] because [[org.yaml.snakeyaml.Yaml]] does not accept the
  *       [[org.yaml.snakeyaml.representer.BaseRepresenter]] class. It only accepts this specific class.
  */
class CirceJsonRepresenter extends Representer {
  outer =>

  // Add the only necessary representer to the map and remove all others
  this.representers.clear()
  // Need to represent field names as strings
  this.representers.put(classOf[String], RepresentFieldName)
  this.representers.put(null, representUnexpected)
  this.multiRepresenters.clear()
  this.multiRepresenters.put(classOf[Json], representJson)
  this.nullRepresenter = representJson

  def representJson: RepresentCirceJson = StandardRepresentCirceJson
  def representUnexpected: RepresentUnexpected = RepresentUnexpected

  object RepresentFieldName extends RepresentFieldName {
    val MULTILINE_PATTERN = Pattern.compile("\n|\u0085|\u2028|\u2029")
  }
  class RepresentFieldName extends Represent {
    override def representData(data: scala.Any): Node = {
      val dataString = data.toString
      // if no other scalar style is explicitly set, use literal style for
      // multiline scalars
      val style =
        if (defaultScalarStyle == null && RepresentFieldName.MULTILINE_PATTERN.matcher(dataString).find())
          Character.valueOf('|')
        else
          null
      representScalar(Tag.STR, dataString, style)
    }
  }

  object RepresentUnexpected extends RepresentUnexpected
  class RepresentUnexpected extends Represent {

    def displayData(data: Any): String = {
      val dataAsString = data.toString
      if (dataAsString.length <= 500) dataAsString
      else dataAsString.substring(0, 499) + "…"
    }

    override def representData(data: Any): Node = {
      val className = data.getClass.getName
      throw new IllegalArgumentException(
        s"Cannot represent an instance of '$className' with value '${displayData(data)}'. " +
          s"Please convert this to ${classOf[Json].getName} first using io.circe.Decoder[$className].decode"
      )
    }
  }

  object StandardRepresentCirceJson extends StandardRepresentCirceJson
  class StandardRepresentCirceJson extends RepresentCirceJson {

    override def represent(json: Json): Node = json match {
      case JNull =>
        representScalar(Tag.NULL, "null", null)
      case JBoolean(b) =>
        representScalar(Tag.BOOL, b.toString, null)
      case JNumber(JsonLong(n)) =>
        representScalar(Tag.INT, n.toString, null)
      case JNumber(JsonDouble(n)) =>
        val asString = n match {
          case Double.NegativeInfinity => ".Inf"
          case Double.PositiveInfinity => "-.Inf"
          case Double.NaN => ".NaN"
          case _ => n.toString
        }
        representScalar(Tag.FLOAT, asString, null)
      case JNumber(n) =>
        val big = n.toBiggerDecimal
        representScalar(if (big.isWhole) Tag.INT else Tag.FLOAT, big.toString, null)
      case JString(s) =>
        representScalar(Tag.STR, s, null)
      case JArray(arr) =>
        representSequence(Tag.SEQ, arr.asJava, null)
      case JObject(obj) =>
        val orderedMap = new java.util.LinkedHashMap[String, Json](obj.size)
        for ((key, value) <- obj.toList) {
          orderedMap.put(key, value)
        }
        representMapping(Tag.MAP, orderedMap, null)
    }
  }
}

object CirceJsonRepresenter {

  /**
    * Represents any [[Json]] as a [[Node]].
    */
  abstract class RepresentCirceJson extends Represent {

    /**
      * Convert the given [[Json]] into a representative [[Node]].
      */
    def represent(yaml: Json): Node

    /**
      * Unsafe call to override [[Represent]] method.
      */
    @deprecated("Use represent instead.", "always")
    override def representData(data: Any): Node = {
      represent(data.asInstanceOf[Json])
    }
  }
}