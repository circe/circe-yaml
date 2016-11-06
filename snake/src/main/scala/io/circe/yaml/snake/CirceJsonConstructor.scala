package io.circe.yaml.snake

import io.circe.{Json, JsonNumber, JsonObject}
import io.circe.Json._
import io.circe.yaml.snake.CirceJsonConstructor.ConstructCirceJson
import org.yaml.snakeyaml.constructor.{Construct, SafeConstructor}
import org.yaml.snakeyaml.nodes._

import scala.collection.JavaConverters._

/**
  * Constructs [[Json]] from Yaml [[Node]]s.
  *
  * @note This class extends [[SafeConstructor]] because [[org.yaml.snakeyaml.Yaml]] does not accept
  *       [[org.yaml.snakeyaml.constructor.BaseConstructor]], only this specific constructor.
  */
class CirceJsonConstructor extends SafeConstructor {

  // clear all the constructors from the parent class, we don't need them.
  this.yamlConstructors.clear()
  // add all supported Node types
  for (tag <- constructYaml.supportedTags) {
    this.yamlConstructors.put(tag, constructYaml)
  }
  // put this in for error message when passing unknown types
  this.yamlConstructors.put(null, SafeConstructor.undefinedConstructor)

  def constructYaml: ConstructCirceJson = DefaultConstructCirceJson

  object DefaultConstructCirceJson extends DefaultConstructCirceJson
  class DefaultConstructCirceJson extends ConstructCirceJson {

    override val supportedTags: Set[Tag] = Set(
      Tag.STR, Tag.TIMESTAMP, Tag.BINARY, // as JString
      Tag.INT, Tag.FLOAT,                 // as JNumber
      Tag.BOOL,                           // as JBoolean
      Tag.SEQ, Tag.SET,                   // as JArray
      Tag.MAP, Tag.OMAP,                  // as JObject
      Tag.NULL                            // as JNull
    )

    override def fromNode(node: Node): Json = {
      @inline def scalarValue(node: Node): String = node.asInstanceOf[ScalarNode].getValue
      val tag = node.getTag
      if (tag.startsWith(Tag.PREFIX)) tag match {
        case Tag.STR | Tag.TIMESTAMP | Tag.BINARY => JString(scalarValue(node))
        case Tag.INT => JNumber(JsonNumber.fromIntegralStringUnsafe(scalarValue(node)))
        case Tag.FLOAT => JNumber(JsonNumber.fromDecimalStringUnsafe(scalarValue(node)))
        case Tag.BOOL => JBoolean(scalarValue(node).toBoolean)
        case Tag.SEQ | Tag.SET =>
          val children = node.asInstanceOf[SequenceNode].getValue.asScala
          // Using Array for faster indexed lookup and smaller heap-size
          val nodes = new Array[Json](children.size)
          for ((child, i) <- children.zipWithIndex) {
            nodes(i) = fromNode(child)
          }
          JArray(nodes)
        case Tag.MAP | Tag.OMAP =>
          val children = node.asInstanceOf[MappingNode].getValue.asScala
          val fields = children.map { child =>
            val key = scalarValue(child.getKeyNode)
            val value = fromNode(child.getValueNode)
            key -> value
          }
          JObject(JsonObject.fromIterable(fields))
        case Tag.NULL => JNull
        case _ => throw new UnsupportedOperationException(s"Cannot construct Json from unsupported Yaml tag: '$tag'")
      } else {
        // custom tag
        Json.fromJsonObject(JsonObject.singleton(tag.getValue.stripPrefix("!"), Json.fromString(scalarValue(node))))
      }
    }
  }
}

object CirceJsonConstructor {

  trait ConstructCirceJson extends Construct {

    /**
      * The set of all supported tags to bind to this constructor.
      */
    def supportedTags: Set[Tag]

    /**
      * Construct a [[Json]] from a given [[Node]].
      */
    def fromNode(node: Node): Json

    @deprecated("Use constructYamlValue", "always")
    final override def construct2ndStep(node: Node, `object`: AnyRef): Unit = {}

    @deprecated("Use constructYamlValue", "always")
    final override def construct(node: Node): AnyRef = fromNode(node)
  }
}
