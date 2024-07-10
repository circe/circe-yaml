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

package io.circe.yaml.v12

import io.circe.yaml.common
import io.circe.yaml.common.Printer._
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.common.{ NonPrintableStyle => SnakeNonPrintableStyle }

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
  nonPrintableStyle: NonPrintableStyle = NonPrintableStyle.Escape
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
    nonPrintableStyle: NonPrintableStyle = this.nonPrintableStyle
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
      nonPrintableStyle = nonPrintableStyle
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
        .setNonPrintableStyle {
          nonPrintableStyle match {
            case NonPrintableStyle.Binary => SnakeNonPrintableStyle.BINARY
            case NonPrintableStyle.Escape => SnakeNonPrintableStyle.ESCAPE
          }
        }
        .build()
    )
}

object PrinterBuilder {
  def apply(): PrinterBuilder = new PrinterBuilder()
}
