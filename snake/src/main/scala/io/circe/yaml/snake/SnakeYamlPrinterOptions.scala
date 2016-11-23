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
  prettyFlow: Boolean = false,
  lineBreak: LineBreak = LineBreak.getPlatformLineBreak,
  version: Option[Version] = None
)

object SnakeYamlPrinterOptions {

  val document: SnakeYamlPrinterOptions = SnakeYamlPrinterOptions(
    defaultFlowStyle = FlowStyle.BLOCK,
    prettyFlow = true,
    version = Some(Version.V1_1)
  )

  val auto: SnakeYamlPrinterOptions = SnakeYamlPrinterOptions()

  @deprecated("Use auto or document depending on whether you want to pretty print a fragment or a full document")
  val default: SnakeYamlPrinterOptions = SnakeYamlPrinterOptions(
    version = Some(Version.V1_1)
  )
}
