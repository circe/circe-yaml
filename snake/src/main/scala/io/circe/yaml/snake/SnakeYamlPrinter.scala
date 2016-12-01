package io.circe.yaml.snake

import io.circe.Json
import io.circe.yaml.printer.Printer

/**
  * Uses [[org.yaml.snakeyaml.Yaml]] to dump Circe's [[Json]] AST as YAML.
  *
  * @note This class is final to prevent extending this with a singleton object.
  *       Also, the implementation is pretty trivial, so it should be easy to make
  *       an alternative.
  *
  * @param configs the configs for building [[org.yaml.snakeyaml.Yaml]] instances
  */
final class SnakeYamlPrinter private[snake] (configs: SnakeYamlConfigs) {

  val print: Printer = (root: Json) => print(root, SnakeYamlPrinterOptions.auto)

  def print(root: Json, opts: SnakeYamlPrinterOptions): String = {
    JavaSnakeYaml(configs, opts).dump(root).trim
  }

  val printDocument: Printer = (root: Json) => printDocument(root, SnakeYamlPrinterOptions.document)

  def printDocument(root: Json, opts: SnakeYamlPrinterOptions): String = {
    JavaSnakeYaml(configs, opts).dump(root)
  }
}
