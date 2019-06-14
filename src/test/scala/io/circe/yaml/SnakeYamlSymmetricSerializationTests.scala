package io.circe.yaml

import io.circe.Json
import io.circe.Json.eqJson
import io.circe.testing.instances._
import org.scalatest.funsuite.AnyFunSuite
import org.typelevel.discipline.scalatest.Discipline

class SnakeYamlSymmetricSerializationTests extends AnyFunSuite with Discipline with SymmetricSerializationTests {
  override val laws: SymmetricSerializationLaws = SymmetricSerializationLaws()

  checkAll("snake.printer", symmetricPrinter[Json](printer.print, parser.parse))
}
