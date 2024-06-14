/*
 * Copyright 2016 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.yaml

import io.circe.yaml.Printer.YamlVersion
import io.circe.yaml.common.Printer._
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.ScalarStyle
import org.yaml.snakeyaml.DumperOptions.{ NonPrintableStyle => SnakeNonPrintableStyle }

import scala.collection.JavaConverters._

final class PrinterBuilder private (
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
  private def copy(
    preserveOrder: Boolean = this.preserveOrder,
    dropNullKeys: Boolean = this.dropNullKeys,
    indent: Int = this.indent,
    maxScalarWidth: Int = this.maxScalarWidth,
    splitLines: Boolean = this.splitLines,
    indicatorIndent: Int = this.indicatorIndent,
    indentWithIndicator: Boolean = this.indentWithIndicator,
    tags: Map[String, String] = this.tags,
    sequenceStyle: FlowStyle = this.sequenceStyle,
    mappingStyle: FlowStyle = this.mappingStyle,
    stringStyle: StringStyle = this.stringStyle,
    lineBreak: LineBreak = this.lineBreak,
    explicitStart: Boolean = this.explicitStart,
    explicitEnd: Boolean = this.explicitEnd,
    nonPrintableStyle: NonPrintableStyle = this.nonPrintableStyle,
    yamlVersion: YamlVersion = this.yamlVersion
  ): PrinterBuilder =
    new PrinterBuilder(
      preserveOrder = preserveOrder,
      dropNullKeys = dropNullKeys,
      indent = indent,
      maxScalarWidth = maxScalarWidth,
      splitLines = splitLines,
      indicatorIndent = indicatorIndent,
      indentWithIndicator = indentWithIndicator,
      tags = tags,
      sequenceStyle = sequenceStyle,
      mappingStyle = mappingStyle,
      stringStyle = stringStyle,
      lineBreak = lineBreak,
      explicitStart = explicitStart,
      explicitEnd = explicitEnd,
      nonPrintableStyle = nonPrintableStyle,
      yamlVersion = yamlVersion
    )

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
      case LineBreak.Unix    => org.yaml.snakeyaml.DumperOptions.LineBreak.UNIX
      case LineBreak.Windows => org.yaml.snakeyaml.DumperOptions.LineBreak.WIN
      case LineBreak.Mac     => org.yaml.snakeyaml.DumperOptions.LineBreak.MAC
    })
    options.setNonPrintableStyle(nonPrintableStyle match {
      case NonPrintableStyle.Binary => SnakeNonPrintableStyle.BINARY
      case NonPrintableStyle.Escape => SnakeNonPrintableStyle.ESCAPE
    })
    options.setVersion(yamlVersion match {
      case YamlVersion.Auto    => null
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
      case StringStyle.Plain        => ScalarStyle.PLAIN
      case StringStyle.DoubleQuoted => ScalarStyle.DOUBLE_QUOTED
      case StringStyle.SingleQuoted => ScalarStyle.SINGLE_QUOTED
      case StringStyle.Literal      => ScalarStyle.LITERAL
      case StringStyle.Folded       => ScalarStyle.FOLDED
    }
  }
}
