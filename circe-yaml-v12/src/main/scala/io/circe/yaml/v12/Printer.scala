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

import scala.collection.JavaConverters._

object Printer {
  @deprecated("Use Printer.builder instead", since = "1.15.2")
  final case class Config(
    preserveOrder: Boolean = false,
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
    explicitEnd: Boolean = false
  )

  @deprecated("Use Printer.builder instead", since = "1.15.2")
  def make(config: Config = Config()): common.Printer = {
    import config._
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

  def builder: PrinterBuilder = PrinterBuilder()
  lazy val spaces2: common.Printer = builder.withIndent(2).build()
  lazy val spaces4: common.Printer = builder.withIndent(4).build()
}
