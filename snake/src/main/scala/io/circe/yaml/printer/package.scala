package io.circe.yaml

import io.circe.Json

package object printer {

  type Printer = Json => String

  /**
    * A simple printer implementation using Snake YAML.
    */
  val print: Printer = (tree: Json) => snake.printer.print(tree)

  /**
    * A document printer implementation using Snake YAML.
    */
  val printDocument: Printer = (tree: Json) => snake.printer.printDocument(tree)
}
