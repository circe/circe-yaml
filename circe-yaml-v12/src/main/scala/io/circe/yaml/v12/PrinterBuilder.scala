package io.circe.yaml.v12

import io.circe.yaml.common
import io.circe.yaml.common.Printer._
import org.snakeyaml.engine.v2.api.DumpSettings

import scala.collection.JavaConverters._

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
  explicitEnd: Boolean = false
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

  def build(): common.Printer =
    new PrinterImpl(
      stringStyle,
      preserveOrder,
      dropNullKeys,
      mappingStyle,
      sequenceStyle,
      DumpSettings
        .builder()
        .setIndent(indent)
        .setWidth(maxScalarWidth)
        .setSplitLines(splitLines)
        .setIndicatorIndent(indicatorIndent)
        .setIndentWithIndicator(indentWithIndicator)
        .setTagDirective(tags.asJava)
        .setDefaultScalarStyle(stringStyle.toScalarStyle)
        .setExplicitStart(explicitStart)
        .setExplicitEnd(explicitEnd)
        .setBestLineBreak {
          lineBreak match {
            case LineBreak.Unix    => "\n"
            case LineBreak.Windows => "\r\n"
            case LineBreak.Mac     => "\r"
          }
        }
        .build()
    )
}

object PrinterBuilder {
  def default = PrinterBuilder()
  def spaces2 = default
  def spaces4 = default.withIndent(4)
}
