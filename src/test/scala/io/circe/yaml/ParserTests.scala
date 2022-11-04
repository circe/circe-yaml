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

  it should "parse aliases" in {
    assert(
      Parser(maxAliasesForCollections = 2)
        .parse(
          """
          | aliases: 
          |   - &alias1
          |     foo:
          |       bar
          | baz:
          |  - *alias1
          |  - *alias1
          |""".stripMargin
        )
        .isRight
    )
  }

  it should "fail to parse too many aliases" in {
    assert(
      Parser(maxAliasesForCollections = 1)
        .parse(
          """
          | aliases: 
          |   - &alias1
          |     foo:
          |       bar
          | baz:
          |  - *alias1
          |  - *alias1
          |""".stripMargin
        )
        .isLeft
    )
  }

  it should "parse when within depth limits" in {
    assert(
      Parser(nestingDepthLimit = 3)
        .parse(
          """
            | foo:
            |   bar:
            |     baz
            |""".stripMargin
        )
        .isRight
    )
  }

  it should "fail to parse when depth limit is exceeded" in {
    assert(
      Parser(nestingDepthLimit = 1)
        .parse(
          """
            | foo:
            |   bar:
            |     baz
            |""".stripMargin
        )
        .isLeft
    )
  }

  it should "parse when within code point limit" in {
    assert(
      Parser(codePointLimit = 1 * 1024 * 1024) // 1MB
        .parse(
          """
            | foo:
            |   bar:
            |     baz
            |""".stripMargin
        )
        .isRight
    )
  }

  it should "fail to parse when code point limit is exceeded" in {
    assert(
      Parser(codePointLimit = 13) // 13B
        .parse(
          """
            | foo:
            |   bar
            |""".stripMargin
        )
        .isLeft
    )
  }
}
