package io.circe.yaml

import io.circe.Json
import scala.language.experimental.macros

package object literal {
  implicit final class YamlStringContext(val sc: StringContext) extends AnyVal {
    def yaml(args: Any*): Json = macro YamlLiteralMacros.yamlImpl
  }
}
