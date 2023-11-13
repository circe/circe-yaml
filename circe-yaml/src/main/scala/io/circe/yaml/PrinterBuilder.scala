package io.circe.yaml

import io.circe.yaml.Printer.YamlVersion
import io.circe.yaml.common.Printer.*
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.{ScalarStyle, NonPrintableStyle as SnakeNonPrintableStyle}

import scala.collection.JavaConverters.*

final case class PrinterBuilder private (
  preserveOrder: Boolean = false,
  dropNullKeys: Boolean = false,
  indent: Int = 2,
  maxScalarWidth: Int = 80,
  splitLines: Boolean = true,
  indicatorIndent: Int = 0,
  indentWithIndicator: Boolean = false,
  tags: Map[String, String] = Map.empty,
  sequenceStyle: FlowStyle = FlowStyle.Block,
  mappingStyle: FlowStyle = FlowStyle.Block,
  stringStyle: StringStyle = StringStyle.Plain,
  lineBreak: LineBreak = LineBreak.Unix,
  explicitStart: Boolean = false,
  explicitEnd: Boolean = false,
  nonPrintableStyle: NonPrintableStyle = NonPrintableStyle.Escape,
  yamlVersion: YamlVersion = YamlVersion.Auto
) {
  def withPreserveOrder(preserveOrder: Boolean): PrinterBuilder =
    copy(preserveOrder = preserveOrder)

  def withDropNullKeys(dropNullKeys: Boolean): PrinterBuilder =
    copy(dropNullKeys = dropNullKeys)

  def withIndent(indent: Int): PrinterBuilder =
    copy(indent = indent)

  def withMaxScalarWidth(maxScalarWidth: Int): PrinterBuilder =
    copy(maxScalarWidth = maxScalarWidth)

  def withSplitLines(splitLines: Boolean): PrinterBuilder =
    copy(splitLines = splitLines)

  def withIndicatorIndent(indicatorIndent: Int): PrinterBuilder =
    copy(indicatorIndent = indicatorIndent)

  def withIndentWithIndicator(indentWithIndicator: Boolean): PrinterBuilder =
    copy(indentWithIndicator = indentWithIndicator)

  def withTags(tags: Map[String, String]): PrinterBuilder =
    copy(tags = tags)

  def withSequenceStyle(sequenceStyle: common.Printer.FlowStyle): PrinterBuilder =
    copy(sequenceStyle = sequenceStyle)

  def withMappingStyle(mappingStyle: common.Printer.FlowStyle): PrinterBuilder =
    copy(mappingStyle = mappingStyle)

  def withStringStyle(stringStyle: common.Printer.StringStyle): PrinterBuilder =
    copy(stringStyle = stringStyle)

  def withLineBreak(lineBreak: common.Printer.LineBreak): PrinterBuilder =
    copy(lineBreak = lineBreak)

  def withExplicitStart(explicitStart: Boolean): PrinterBuilder =
    copy(explicitStart = explicitStart)

  def withExplicitEnd(explicitEnd: Boolean): PrinterBuilder =
    copy(explicitEnd = explicitEnd)

  def withNonPrintableStyle(nonPrintableStyle: NonPrintableStyle): PrinterBuilder =
    copy(nonPrintableStyle = nonPrintableStyle)

  def withYamlVersion(yamlVersion: YamlVersion): PrinterBuilder =
    copy(yamlVersion = yamlVersion)

  def build(): common.Printer = {
    import PrinterBuilder.*
    val options = new DumperOptions()
    options.setIndent(indent)
    options.setWidth(maxScalarWidth)
    options.setSplitLines(splitLines)
    options.setIndicatorIndent(indicatorIndent)
    options.setIndentWithIndicator(indentWithIndicator)
    options.setTags(tags.asJava)
    options.setDefaultScalarStyle(stringStyle.toScalarStyle)
    options.setExplicitStart(explicitStart)
    options.setExplicitEnd(explicitEnd)
    options.setLineBreak(lineBreak match {
      case LineBreak.Unix => org.yaml.snakeyaml.DumperOptions.LineBreak.UNIX
      case LineBreak.Windows => org.yaml.snakeyaml.DumperOptions.LineBreak.WIN
      case LineBreak.Mac => org.yaml.snakeyaml.DumperOptions.LineBreak.MAC
    })
    options.setNonPrintableStyle(nonPrintableStyle match {
      case NonPrintableStyle.Binary => SnakeNonPrintableStyle.BINARY
      case NonPrintableStyle.Escape => SnakeNonPrintableStyle.ESCAPE
    })
    options.setVersion(yamlVersion match {
      case YamlVersion.Auto => null
      case YamlVersion.Yaml1_0 => DumperOptions.Version.V1_0
      case YamlVersion.Yaml1_1 => DumperOptions.Version.V1_1
    })

    new PrinterImpl(
      stringStyle,
      preserveOrder,
      dropNullKeys,
      mappingStyle,
      sequenceStyle,
      options
    )
  }
}

object PrinterBuilder {
  def apply(): PrinterBuilder = new PrinterBuilder()
  def spaces2: common.Printer = new PrinterBuilder().build()
  def spaces4: common.Printer = new PrinterBuilder(indent = 4).build()

  implicit class SnakeStringStyle(stringStyle: StringStyle) {
    def toScalarStyle: ScalarStyle = stringStyle match {
      case StringStyle.Plain => ScalarStyle.PLAIN
      case StringStyle.DoubleQuoted => ScalarStyle.DOUBLE_QUOTED
      case StringStyle.SingleQuoted => ScalarStyle.SINGLE_QUOTED
      case StringStyle.Literal => ScalarStyle.LITERAL
      case StringStyle.Folded => ScalarStyle.FOLDED
    }
  }
}
