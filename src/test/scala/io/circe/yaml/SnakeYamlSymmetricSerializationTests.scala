package io.circe.yaml

import io.circe.Json
import io.circe.Json.eqJson
import io.circe.testing.instances._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.Laws

class SnakeYamlSymmetricSerializationTests extends AnyFunSuite with Checkers with SymmetricSerializationTests {
  override val laws: SymmetricSerializationLaws = SymmetricSerializationLaws()

  def checkAll(name: String, ruleSet: Laws#RuleSet): Unit =
    for ((id, prop) <- ruleSet.all.properties)
      test(name + "." + id) {
        check(prop)
      }

  checkAll("snake.printer", symmetricPrinter[Json](printer.print, parser.parse))
}
