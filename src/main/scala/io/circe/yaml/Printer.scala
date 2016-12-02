package io.circe.yaml

import java.io.StringWriter

import scala.collection.JavaConverters._

import Printer._
import io.circe.Json
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer
import org.yaml.snakeyaml.{DumperOptions, Yaml}

final case class Printer(
  dropNullKeys: Boolean = false,
  indent: Int = 2,
  maxScalarWidth: Int = 80,
  splitLines: Boolean = true,
  indicatorIndent: Int = 0,
  tags: Map[String, String] = Map.empty,
  defaultFlowStyle: FlowStyle = FlowStyle.Auto,
  defaultScalarStyle: ScalarStyle = ScalarStyle.Plain,
  lineBreak: LineBreak = LineBreak.Unix,
  version: YamlVersion = YamlVersion.Yaml1_1
) {
  final def pretty(json: Json): String = {
    val options = {
      val options = new DumperOptions()
      options.setIndent(indent)
      options.setWidth(maxScalarWidth)
      options.setSplitLines(splitLines)
      options.setIndicatorIndent(indicatorIndent)
      if(tags.nonEmpty) options.setTags(tags.asJava)
      options.setDefaultFlowStyle(defaultFlowStyle match {
        case FlowStyle.Flow  => DumperOptions.FlowStyle.FLOW
        case FlowStyle.Block => DumperOptions.FlowStyle.BLOCK
        case FlowStyle.Auto  => DumperOptions.FlowStyle.AUTO
      })
      options.setDefaultScalarStyle(defaultScalarStyle match {
        case ScalarStyle.Plain        => DumperOptions.ScalarStyle.PLAIN
        case ScalarStyle.DoubleQuoted => DumperOptions.ScalarStyle.DOUBLE_QUOTED
        case ScalarStyle.SingleQuoted => DumperOptions.ScalarStyle.SINGLE_QUOTED
        case ScalarStyle.Literal      => DumperOptions.ScalarStyle.LITERAL
        case ScalarStyle.Folded       => DumperOptions.ScalarStyle.FOLDED
      })
      options.setLineBreak(lineBreak match {
        case LineBreak.Unix    => DumperOptions.LineBreak.UNIX
        case LineBreak.Windows => DumperOptions.LineBreak.WIN
        case LineBreak.Mac     => DumperOptions.LineBreak.MAC
      })
      options.setVersion(version match {
        case YamlVersion.Yaml1_0 => DumperOptions.Version.V1_0
        case YamlVersion.Yaml1_1 => DumperOptions.Version.V1_1
      })
      options
    }
    val rootTag = yamlTag(json)
    val writer = new StringWriter()
    val serializer = new Serializer(new Emitter(writer, options), new Resolver, options, rootTag)
    serializer.open()
    serializer.serialize(jsonToYaml(json, dropNullKeys))
    serializer.close()
    writer.toString
  }
}

object Printer {

  val spaces2 = Printer()
  val spaces4 = Printer(indent = 4)

  sealed trait FlowStyle
  object FlowStyle {
    case object Flow extends FlowStyle
    case object Block extends FlowStyle
    case object Auto extends FlowStyle
  }

  sealed trait ScalarStyle
  object ScalarStyle {
    case object Plain extends ScalarStyle
    case object DoubleQuoted extends ScalarStyle
    case object SingleQuoted extends ScalarStyle
    case object Literal extends ScalarStyle
    case object Folded extends ScalarStyle
  }

  sealed trait LineBreak
  object LineBreak {
    case object Unix extends LineBreak
    case object Windows extends LineBreak
    case object Mac extends LineBreak
  }

  sealed trait YamlVersion
  object YamlVersion {
    case object Yaml1_0 extends YamlVersion
    case object Yaml1_1 extends YamlVersion
  }
}