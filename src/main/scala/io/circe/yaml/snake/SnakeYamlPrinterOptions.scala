package io.circe.yaml.snake

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions._

/**
  * An immutable version of [[DumperOptions]] to encourage sharing.
  */
case class SnakeYamlPrinterOptions(
  indent: Int = 2,
  maxScalarWidth: Int = 80,
  splitLines: Boolean = true,
  indicatorIndent: Int = 0,
  tags: Map[String, String] = Map.empty,
  defaultFlowStyle: FlowStyle = FlowStyle.AUTO,
  defaultScalarStyle: ScalarStyle = ScalarStyle.PLAIN,
  lineBreak: LineBreak = LineBreak.UNIX,
  version: Option[Version] = Some(Version.V1_1)
)

object SnakeYamlPrinterOptions {

  val default: SnakeYamlPrinterOptions = SnakeYamlPrinterOptions()
}
