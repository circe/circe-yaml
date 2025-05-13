/*
 * Copyright 2016 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.yaml.v12

import io.circe.Encoder
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.Success
import scala.util.Try

class EscapingTests extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  import io.circe.syntax._
  import io.circe.yaml.v12.Printer.spaces2.pretty
  import io.circe.yaml.v12.parser.parse

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
    val exceptions = Set(0xa, 8232, 8233).map(_.toChar)
    (Char.MinValue to Char.MaxValue).filterNot(exceptions).foreach(test1)
  }

  def test2(s0: String): Unit = {
    val json = s0.asJson
    val s1 = pretty(json)
    s1.forall(isPrintable)
    parse(s1) shouldBe Right(json)
  }

  it should "properly escape JSON string values" in
    forAll { (s0: String) =>
      test2(s0)
    }

  def test3(c: Char): Unit = {
    val m = Map(c.toString -> c.toInt)
    val o = Encoder[Map[String, Int]].apply(m)

    parser.parse(printer.print(o)).right.flatMap(_.as[Map[String, Int]]) shouldBe Right(m)
  }

  it should "properly escape JSON object keys" in {
    // exhaustive test: 65k test cases
    val exceptions = Set(8232, 8233).map(_.toChar)
    (Char.MinValue to Char.MaxValue).filterNot(exceptions).foreach(test3)
  }
}
