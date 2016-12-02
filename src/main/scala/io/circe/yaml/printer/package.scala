package io.circe.yaml

import io.circe.Json

package object printer {

  type Printer = Json => String

  /**
    * A default printer implementation using Snake YAML.
    */
  val print: Printer = (tree: Json) => snake.printer.print(tree)
}
