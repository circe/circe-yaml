package io.circe.yaml

import io.circe.Json
import org.yaml.snakeyaml.DumperOptions

package object printer {

  type Printer = Json => String

  val print: Printer = (ast: Json) => Printer(ast)

  implicit class AsYamlSyntax(val json: Json) extends AnyVal {

    @deprecated("Use import io.circe.yaml.syntax._ instead. This will be removed in 0.3.0.", "0.2.2")
    def asYaml: String = Printer(json)
    @deprecated("Use import io.circe.yaml.syntax._ instead. This will be removed in 0.3.0.", "0.2.2")
    def asYaml(dumperOptions: DumperOptions) = Printer(json, dumperOptions)

  }

}
