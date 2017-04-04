package io.circe.yaml

import Printer._
import io.circe._
import java.io.StringWriter
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.nodes._
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer
import scala.collection.JavaConverters._

final case class Printer(
  dropNullKeys: Boolean = false,
  indent: Int = 2,
  maxScalarWidth: Int = 80,
  splitLines: Boolean = true,
  indicatorIndent: Int = 0,
  tags: Map[String, String] = Map.empty,
  sequenceStyle: FlowStyle = FlowStyle.Block,
  mappingStyle: FlowStyle = FlowStyle.Block,
  stringStyle: StringStyle = StringStyle.Plain,
  lineBreak: LineBreak = LineBreak.Unix,
  explicitStart: Boolean = false,
  explicitEnd: Boolean = false,
  version: YamlVersion = YamlVersion.Auto
) {

  def pretty(json: Json): String = {
    val rootTag = yamlTag(json)
    val writer = new StringWriter()
    val serializer = new Serializer(new Emitter(writer, options), new Resolver, options, rootTag)
    serializer.open()
    serializer.serialize(jsonToYaml(json))
    serializer.close()
    writer.toString
  }

  private lazy val options = {
    val options = new DumperOptions()
    options.setIndent(indent)
    options.setWidth(maxScalarWidth)
    options.setSplitLines(splitLines)
    options.setIndicatorIndent(indicatorIndent)
    options.setTags(tags.asJava)
    options.setDefaultScalarStyle(stringStyle match {
      case StringStyle.Plain        => DumperOptions.ScalarStyle.PLAIN
      case StringStyle.DoubleQuoted => DumperOptions.ScalarStyle.DOUBLE_QUOTED
      case StringStyle.SingleQuoted => DumperOptions.ScalarStyle.SINGLE_QUOTED
      case StringStyle.Literal      => DumperOptions.ScalarStyle.LITERAL
      case StringStyle.Folded       => DumperOptions.ScalarStyle.FOLDED
    })
    options.setLineBreak(lineBreak match {
      case LineBreak.Unix    => DumperOptions.LineBreak.UNIX
      case LineBreak.Windows => DumperOptions.LineBreak.WIN
      case LineBreak.Mac     => DumperOptions.LineBreak.MAC
    })
    options.setVersion(version match {
      case YamlVersion.Auto    => null
      case YamlVersion.Yaml1_0 => DumperOptions.Version.V1_0
      case YamlVersion.Yaml1_1 => DumperOptions.Version.V1_1
    })
    options.setExplicitStart(explicitStart)
    options.setExplicitEnd(explicitEnd)
    options
  }


  private def scalarNode(tag: Tag, value: String) = new ScalarNode(tag, value, null, null, null)
  private def stringNode(value: String) = {
    val style: Character = stringStyle match {
      case StringStyle.DoubleQuoted => '"'
      case StringStyle.SingleQuoted => '''
      case StringStyle.Folded       => '>'
      case StringStyle.Literal      => '|'
      case StringStyle.Plain        => null
    }
    new ScalarNode(Tag.STR, value, null, null, style)
  }

  private def jsonToYaml(json: Json): Node = {

    def convertObject(obj: JsonObject) = {
      val map = if (dropNullKeys)
        obj.filter(!_._2.isNull).toMap
      else
        obj.toMap
      new MappingNode(Tag.MAP, map.map {
        case (k, v) => new NodeTuple(scalarNode(Tag.STR, k), jsonToYaml(v))
      }.toList.asJava, mappingStyle == FlowStyle.Flow)
    }

    json.fold(
      scalarNode(Tag.NULL, "null"),
      bool =>
        scalarNode(Tag.BOOL, bool.toString),
      number => {
        if (number.toLong.nonEmpty)
          scalarNode(Tag.INT, number.toString)
        else
          scalarNode(Tag.FLOAT, number.toString)
      },
      str =>
        stringNode(str),
      arr =>
        new SequenceNode(Tag.SEQ, arr.map(jsonToYaml).asJava, sequenceStyle == FlowStyle.Flow),
      obj =>
        convertObject(obj)
    )
  }
}

object Printer {

  val spaces2 = Printer()
  val spaces4 = Printer(indent = 4)

  sealed trait FlowStyle
  object FlowStyle {
    case object Flow extends FlowStyle
    case object Block extends FlowStyle
  }

  sealed trait StringStyle
  object StringStyle {
    case object Plain extends StringStyle
    case object DoubleQuoted extends StringStyle
    case object SingleQuoted extends StringStyle
    case object Literal extends StringStyle
    case object Folded extends StringStyle
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
    case object Auto extends YamlVersion
  }

  private def yamlTag(json: Json) = json.fold(
    Tag.NULL,
    _ => Tag.BOOL,
    number => if (number.toLong.nonEmpty) Tag.INT else Tag.FLOAT,
    _ => Tag.STR,
    _ => Tag.SEQ,
    _ => Tag.MAP
  )
}
