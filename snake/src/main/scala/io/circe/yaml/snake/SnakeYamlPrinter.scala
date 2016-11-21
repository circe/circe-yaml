package io.circe.yaml.snake

import io.circe.Json

/**
  * Uses [[org.yaml.snakeyaml.Yaml]] to dump Circe's [[Json]] AST as YAML.
  *
  * @note This class is not thread-safe, you must create one per thread.
  *       See [[org.yaml.snakeyaml.Yaml]] for more information.
  *
  * @note This class is final to prevent extending this with a singleton object.
  *       Also, the implementation is pretty trivial, so it should be easy to make
  *       an alternative.
  *
  * @param configs the configs for building [[org.yaml.snakeyaml.Yaml]] instances
  */
private[snake] final class SnakeYamlPrinter(configs: SnakeYamlConfigs) {

  lazy val defaultYaml = JavaSnakeYaml(configs, SnakeYamlPrinterOptions.default)

  def print(json: Json): String = print(json, SnakeYamlPrinterOptions.default)

  def print(root: Json, opts: SnakeYamlPrinterOptions): String = {
    if (opts == SnakeYamlPrinterOptions.default)
      defaultYaml.dump(root)
    else
      JavaSnakeYaml(configs, opts).dump(root)
  }
}
