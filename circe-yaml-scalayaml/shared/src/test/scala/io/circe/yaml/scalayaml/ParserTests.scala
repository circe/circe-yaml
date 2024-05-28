package io.circe.yaml.scalayaml

import io.circe.Json
import io.circe.syntax._
import java.io.StringReader
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ParserTests extends AnyFlatSpec with Matchers with EitherValues {
  // the laws should do a pretty good job of surfacing errors; these are mainly to ensure test coverage

  "Parser.parse" should "fail on invalid tagged numbers" in {
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
          """[0xFF, 0xff, 0xabcd]"""
        )
        .contains(Seq(0xff, 0xff, 0xabcd).asJson)
    )
  }

  it should "parse decimal with underscore breaks" in {
    assert(
      parser
        .parse(
          """foo: !!int 1_000_000"""
        )
        .contains(Map("foo" -> 1000000).asJson)
    )
  }

  it should "parse aliases" in {
    assert(
      Parser
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

  "Parser.parseDocuments" should "fail on invalid tagged numbers" in {
    val result = parser.parseDocuments(new StringReader("!!int 12foo")).toList
    assert(result.size == 1)
    assert(result.head.isLeft)
  }

  it should "fail to parse complex keys" in {
    val result = parser
      .parseDocuments(new StringReader("""
                                         |? - foo
                                         |  - bar
                                         |: 1""".stripMargin))
      .toList
    assert(result.size == 1)
    assert(result.head.isLeft)
  }

  it should "fail to parse invalid YAML" in {
    val result = parser.parseDocuments(new StringReader("""foo: - bar""")).toList
    assert(result.size == 1)
    assert(result.head.isLeft)
    assert(result.head.isInstanceOf[Either[io.circe.ParsingFailure, Json]])
  }

  it should "parse yes as true" in {
    val result = parser.parseDocuments(new StringReader("""foo: yes""")).toList
    assert(result.size == 1)
    assert(result.head.isRight)
  }

  it should "parse hexadecimal" in {
    val result = parser.parseDocuments(new StringReader("""[0xFF, 0xff, 0xabcd]""")).toList
    assert(result.size == 1)
    assert(result.head.contains(Seq(0xff, 0xff, 0xabcd).asJson))
  }

  it should "parse decimal with underscore breaks" in {
    val result = parser.parseDocuments(new StringReader("""foo: !!int 1_000_000""")).toList
    assert(result.size == 1)
    assert(result.head.contains(Map("foo" -> 1000000).asJson))
  }

  it should "parse aliases" in {
    val result = parser
      .parseDocuments(
        new StringReader(
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
      )
      .toList
    assert(result.size == 1)
    assert(result.head.isRight)
  }

  it should "parse when within depth limits" in {
    assert(
      Parser
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

  it should "parse when within code point limit" in {
    assert(
      Parser // 1MB
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

}
