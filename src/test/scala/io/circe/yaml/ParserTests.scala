package io.circe.yaml

import io.circe.Json
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class ParserTests extends FlatSpec with Matchers with EitherValues {
  // the laws should do a pretty good job of surfacing errors; these are mainly to ensure test coverage

  "Parser" should "fail on invalid tagged numbers" in {
    assert(parser.parse("!!int 12foo").isLeft)
  }

  it should "fail to parse complex keys" in {
    assert(parser.parse(
      """
        |? - foo
        |  - bar
        |: 1
      """.stripMargin).isLeft)
  }

  it should "fail to parse invalid YAML" in {
    assert(parser.parse(
      """foo: - bar"""
    ).isLeft)
  }

  it should "parse yes as true" in {
    assert(parser.parse(
      """foo: yes"""
    ).isRight)
  }

  it should "parse empty string as false" in {
    assert(parser.parse(
      ""
    ).right.value == Json.False)
  }

  it should "parse blank string as false" in {
    assert(parser.parse(
      "   "
    ).right.value == Json.False)
  }
}
