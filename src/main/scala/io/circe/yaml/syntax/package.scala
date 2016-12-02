package io.circe.yaml

import io.circe.Json

/**
  * Provides helpful syntax that is not specific to the SnakeYAML implementation.
  */
package object syntax {

  /**
    * Call this to serialize a [[Json]] AST into a YAML string using the default options.
    */
  implicit class AsYaml(val tree: Json) extends AnyVal {
    def asYamlString: String = printer.print(tree)
  }
}
