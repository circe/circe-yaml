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

package io.circe.yaml.scalayaml

import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class PrinterTests extends AnyFreeSpec with Matchers {

  "Flow style" - {
    val json = Json.obj("foo" -> Json.arr((0 until 3).map(_.toString).map(Json.fromString): _*))

    "Block" in {
      val printer = Printer
      printer.pretty(json) shouldEqual
        """foo: 
          |  - "0"
          |  - "1"
          |  - "2"
          |""".stripMargin
    }

  }

  "Preserves order" - {
    val kvPairs = Seq("d" -> 4, "a" -> 1, "b" -> 2, "c" -> 3)
    val json = Json.obj(kvPairs.map { case (k, v) => k -> Json.fromInt(v) }: _*)
    "true" in {
      val printer = Printer
      printer.pretty(json) shouldEqual
        """d: 4
          |a: 1
          |b: 2
          |c: 3
          |""".stripMargin
    }
  }

  "Scalar style" - {
    val foos = Seq.fill(40)("foo")
    val foosPlain = foos.mkString(" ")
    val foosFolded = Seq(foos.take(20), foos.slice(20, 40)).map(_.mkString(" ")).mkString("\n  ")
    val json = Json.obj("foo" -> Json.fromString(foosPlain))

    "Plain" in {
      val printer = Printer
      printer.pretty(json) shouldEqual
        s"""foo: "$foosPlain"
           |""".stripMargin
    }

  }

  "Plain with newlines" in {
    val json = Json.obj("foo" -> Json.fromString("abc\nxyz\n"))
    val printer = Printer
    printer.pretty(json) shouldEqual
      "foo: \"abc\\nxyz\\n\"\n"
  }

  "Drop null keys" in {
    val json = Json.obj("nullField" -> Json.Null, "nonNullField" -> Json.fromString("foo"))
    Printer.pretty(json) shouldEqual "nullField: null\nnonNullField: \"foo\"\n"
  }

  "Root integer" in {
    val json = Json.fromInt(10)
    Printer.pretty(json) shouldEqual "10\n"
  }

  "Root float" in {
    val json = Json.fromDoubleOrNull(22.22)
    Printer.pretty(json) shouldEqual "22.22\n"
  }

  "Root float without decimal part" in {
    val json = Json.fromDoubleOrNull(22.0)
    Printer.pretty(json) shouldEqual "22.0\n"
  }

  "Line break" - {
    val json = Json.arr(Json.fromString("foo"), Json.fromString("bar"))

    "Unix" in {
      Printer.pretty(json) shouldEqual
        "- \"foo\"\n- \"bar\"\n"
    }

  }

}
