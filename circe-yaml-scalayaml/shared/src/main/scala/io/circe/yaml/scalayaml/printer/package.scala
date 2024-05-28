package io.circe.yaml.scalayaml

import io.circe.Json

package object printer extends io.circe.yaml.common.Printer {

  /**
   * A default printer implementation using Snake YAML.
   */
  def print(tree: Json): String = pretty(tree)
  def pretty(tree: Json): String = Printer.pretty(tree)
}
