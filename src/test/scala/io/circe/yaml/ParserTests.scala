package io.circe.yaml

import io.circe.Json
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import io.circe.syntax._
import org.scalatest.matchers.should.Matchers

class ParserTests extends AnyFlatSpec with Matchers with EitherValues {
  // the laws should do a pretty good job of surfacing errors; these are mainly to ensure test coverage

  "Parser" should "fail on invalid tagged numbers" in {
    assert(parser.parse("!!int 12foo").isLeft)
  }

  it should "fail to parse complex keys" in {
    assert(
      parser
        .parse("""
        |? - foo
        |  - bar
        |: 1
      """.stripMargin)
        .isLeft
    )
  }

  it should "fail to parse invalid YAML" in {
    assert(
      parser
        .parse(
          """foo: - bar"""
        )
        .isLeft
    )
  }

  it should "parse yes as true" in {
    assert(
      parser
        .parse(
          """foo: yes"""
        )
        .isRight
    )
  }

  it should "parse hexadecimal" in {
    assert(
      parser
        .parse(
          """[0xFF, 0xff, 0xab_cd]"""
        )
        .contains(Seq(0xff, 0xff, 0xabcd).asJson)
    )
  }

  it should "parse decimal with underscore breaks" in {
    assert(
      parser
        .parse(
          """foo: 1_000_000"""
        )
        .contains(Map("foo" -> 1000000).asJson)
    )
  }

  it should "parse empty string as false" in {
    assert(
      parser
        .parse(
          ""
        )
        .right
        .value == Json.False
    )
  }

  it should "parse blank string as false" in {
    assert(
      parser
        .parse(
          "   "
        )
        .right
        .value == Json.False
    )
  }
}
