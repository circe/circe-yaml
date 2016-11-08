package io.circe.yaml

import io.circe.Json
import io.circe.yaml.printer.Printer

trait YamlPrinter {

  def print(json: Json): String
}

object YamlPrinter extends YamlPrinter {

  override def print(json: Json): String = Printer(json)


}
