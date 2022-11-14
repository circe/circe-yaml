package io.circe.yaml.v12

import io.circe.yaml.common
import io.circe.yaml.common.Printer._
import org.snakeyaml.engine.v2.api.DumpSettings
import scala.collection.JavaConverters._

object Printer {
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

  lazy val spaces2: common.Printer = make()
  lazy val spaces4: common.Printer = make(Config(indent = 4))

}
