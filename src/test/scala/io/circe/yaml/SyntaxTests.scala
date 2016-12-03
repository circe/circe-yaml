package io.circe.yaml

import io.circe.{Json, JsonObject}
import org.scalatest.{FlatSpec, Matchers}
import syntax._

class SyntaxTests extends FlatSpec with Matchers {

  val json = Json.obj(
    "foo" -> Json.obj(
      "bar" -> Json.fromString("baz")
    )
  )

  "spaces2" should "have double space indent" in {
    json.asYaml.spaces2 shouldEqual
      """foo:
        |  bar: baz
        |""".stripMargin
  }

  "spaces4" should "have quadruple space indent" in {
    json.asYaml.spaces4 shouldEqual
      """foo:
        |    bar: baz
        |""".stripMargin
  }

}
