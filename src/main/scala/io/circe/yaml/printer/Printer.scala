package io.circe.yaml.printer

import java.io.StringWriter

import scala.collection.JavaConverters._
import io.circe.Json
import io.circe.Json.{JArray, JBoolean, JNull, JNumber, JObject, JString}
import org.yaml.snakeyaml.{DumperOptions, emitter}
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.nodes._
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer

/**
  * This can't hook into any circe Printer stuff, because that's a sealed case class
  * So it's a separate thing.
  */
@deprecated("Use io.circe.yaml.YamlPrinter instead. This will be removed in 0.3.0.", "0.2.0")
class Printer {


  private def toYamlNode(json: Json): Node = json match {
    case JNull => new ScalarNode(Tag.NULL, "null", null, null, '\0')
    case JBoolean(b) => new ScalarNode(Tag.BOOL, b.toString, null, null, '\0')
    case JNumber(n) =>
      val tag = n.toInt.map(_ => Tag.INT) orElse
        n.toShort.map(_ => Tag.INT) orElse
        n.toLong.map(_ => Tag.INT) orElse
        n.toByte.map(_ => Tag.INT) getOrElse
        Tag.FLOAT
      new ScalarNode(tag, n.toString, null, null, '\0')
    case JString(s) =>
      new ScalarNode(Tag.STR, s, null, null, null)
    case JArray(values) =>
      val v = values.map(toYamlNode)
      new SequenceNode(Tag.SEQ, v.asJava, false)
    case JObject(jo) =>
      val vs = jo.toList.map {
        case (k, v) => new NodeTuple(new ScalarNode(Tag.STR, k, null, null, null), toYamlNode(v))
      }
      new MappingNode(Tag.MAP, vs.asJava, false)
  }

  @deprecated("Use io.circe.yaml.YamlPrinter.print instead. This will be removed in 0.3.0.", "0.2.0")
  def apply(json: Json, dumperOptions: DumperOptions = new DumperOptions): String = {
    val yaml = toYamlNode(json)
    val writer = new StringWriter()
    val serializer = new Serializer(new Emitter(writer, dumperOptions), new Resolver, dumperOptions, null)
    try {
      serializer.open()
      serializer.serialize(yaml)
      writer.toString
    }
    finally serializer.close()
  }

}

@deprecated("Use io.circe.yaml.YamlPrinter instead. This will be removed in 0.3.0.", "0.2.0")
object Printer extends Printer
