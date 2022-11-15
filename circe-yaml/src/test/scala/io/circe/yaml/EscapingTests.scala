package io.circe.yaml

import io.circe.Encoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scala.util.{ Success, Try }
import org.scalatest.matchers.should.Matchers

class EscapingTests extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  import io.circe.syntax._
  import io.circe.yaml.Printer.spaces2.pretty
  import io.circe.yaml.parser.parse

  // according to the YAML spec (section 5.1: character set)
  def isPrintable(c: Char): Boolean =
    ('\t' == c) ||
      ('\n' == c) ||
      ('\r' == c) ||
      (' ' <= c && c <= '~') ||
      ('\u0085' == c) ||
      ('\u00a0' <= c && c <= '\ud7ff') ||
      ('\ue000' <= c && c <= '\ufffd')

  def test1(c: Char): Unit = {
    val r = "'\\u%04X'".format(c.toInt)
    def repr[A](a: A): (String, A) = (r, a)

    val json = c.toString.asJson
    val s = pretty(json)

    if (s.contains(c)) repr(isPrintable(c)) shouldBe repr(true)
    else () // we do not enforce that printable chars are never escaped

    repr(s.forall(isPrintable)) shouldBe repr(true)
    repr(Try(parse(s))) shouldBe repr(Success(Right(json)))
  }

  "Escaping" should "properly escape JSON string values (all chars)" in {
    // exhaustive test: 65k test cases
    (Char.MinValue to Char.MaxValue).map(_.toChar).foreach(test1)
  }

  def test2(s0: String): Unit = {
    val json = s0.asJson
    val s1 = pretty(json)
    s1.forall(isPrintable)
    parse(s1) shouldBe Right(json)
  }

  it should "properly escape JSON string values" in {
    forAll { (s0: String) =>
      test2(s0)
    }
  }

  def test3(c: Char): Unit = {
    val m = Map(c.toString -> c.toInt)
    val o = Encoder[Map[String, Int]].apply(m)

    parser.parse(printer.print(o)).right.flatMap(_.as[Map[String, Int]]) shouldBe Right(m)
  }

  it should "properly escape JSON object keys" in {
    // exhaustive test: 65k test cases
    (Char.MinValue to Char.MaxValue).map(_.toChar).foreach(test3)
  }
}
