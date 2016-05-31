package io.circe.yaml

import io.circe.Json
import org.yaml.snakeyaml.DumperOptions

package object printer {

  implicit class AsYamlSyntax(val json: Json) extends AnyVal {

    def asYaml: String = Printer(json)
    def asYaml(dumperOptions: DumperOptions) = Printer(json, dumperOptions)

  }

}
