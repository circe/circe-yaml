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

import io.circe.Json
import io.circe.yaml.common.Printer.FlowStyle
import io.circe.yaml.common.Printer.LineBreak
import io.circe.yaml.common.Printer.StringStyle
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class PrinterTests extends AnyFreeSpec with Matchers {

  "Flow style" - {
    val json = Json.obj("foo" -> Json.arr((0 until 3).map(_.toString).map(Json.fromString): _*))

    "Block" in {
      val printer = Printer.builder.withSequenceStyle(FlowStyle.Block).withMappingStyle(FlowStyle.Block).build()
      printer.pretty(json) shouldEqual
        """foo:
          |- '0'
          |- '1'
          |- '2'
          |""".stripMargin
    }

    "Flow" in {
      val printer = Printer.builder.withSequenceStyle(FlowStyle.Block).withMappingStyle(FlowStyle.Flow).build()
      printer.pretty(json) shouldEqual
        """{foo: ['0', '1', '2']}
          |""".stripMargin
    }
  }

  "Preserves order" - {
    val kvPairs = Seq("d" -> 4, "a" -> 1, "b" -> 2, "c" -> 3)
    val json = Json.obj(kvPairs.map { case (k, v) => k -> Json.fromInt(v) }: _*)
    "true" in {
      val printer = Printer.builder.withPreserveOrder(true).build()
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
    val foosSplit = Seq(foos.take(19), foos.slice(19, 39), foos.slice(39, 40)).map(_.mkString(" "))
    val foosPlain = foos.mkString(" ")
    val foosFolded = Seq(foos.take(20), foos.slice(20, 40)).map(_.mkString(" ")).mkString("\n  ")
    val json = Json.obj("foo" -> Json.fromString(foosPlain))

    "Plain" in {
      val printer = Printer.builder.withSplitLines(false).withStringStyle(StringStyle.Plain).build()
      printer.pretty(json) shouldEqual
        s"""foo: $foosPlain
           |""".stripMargin
    }

    "Double quoted" in {
      val printer = Printer.builder.withStringStyle(StringStyle.DoubleQuoted).build()
      printer.pretty(json) shouldEqual
        s"""foo: "${foosSplit.mkString("\\\n  \\ ")}"
           |""".stripMargin
    }

    "Single quoted" in {
      val printer = Printer.builder.withStringStyle(StringStyle.SingleQuoted).build()
      printer.pretty(json) shouldEqual
        s"""foo: '${foosSplit.mkString("\n  ")}'
           |""".stripMargin
    }

    "Folded" in {
      val printer = Printer.builder.withStringStyle(StringStyle.Folded).build()
      printer.pretty(json) shouldEqual
        s"""foo: >-
           |  $foosFolded
           |""".stripMargin
    }

    "Literal" in {
      val printer = Printer.builder.withStringStyle(StringStyle.Literal).build()
      printer.pretty(json) shouldEqual
        s"""foo: |-
           |  $foosPlain
           |""".stripMargin
    }

  }

  "Plain with newlines" in {
    val json = Json.obj("foo" -> Json.fromString("abc\nxyz\n"))
    val printer = Printer.builder.withStringStyle(StringStyle.Plain).build()
    printer.pretty(json) shouldEqual
      s"""foo: |
         |  abc
         |  xyz
         |""".stripMargin
  }

  "Drop null keys" in {
    val json = Json.obj("nullField" -> Json.Null, "nonNullField" -> Json.fromString("foo"))
    val printer = Printer.builder.withDropNullKeys(true).build()
    printer.pretty(json) shouldEqual "nonNullField: foo\n"
  }

  "Root integer" in {
    val json = Json.fromInt(10)
    Printer.spaces2.pretty(json) shouldEqual "10\n"
  }

  "Root float" in {
    val json = Json.fromDoubleOrNull(22.22)
    Printer.spaces2.pretty(json) shouldEqual "22.22\n"
  }

  "Root float without decimal part" in {
    val json = Json.fromDoubleOrNull(22.0)
    Printer.spaces2.pretty(json) shouldEqual "22.0\n"
  }

  "Line break" - {
    val json = Json.arr(Json.fromString("foo"), Json.fromString("bar"))

    "Unix" in {
      Printer.builder.withLineBreak(LineBreak.Unix).build().pretty(json) shouldEqual
        "- foo\n- bar\n"
    }

    "Windows" in {
      Printer.builder.withLineBreak(LineBreak.Windows).build().pretty(json) shouldEqual
        "- foo\r\n- bar\r\n"
    }

    "Mac" in {
      Printer.builder.withLineBreak(LineBreak.Mac).build().pretty(json) shouldEqual
        "- foo\r- bar\r"
    }
  }

  "Indicator indent" - {
    val firstEl = Json.obj("a" -> Json.fromString("b"), "c" -> Json.fromString("d"))
    val secondEl = Json.obj("aa" -> Json.fromString("bb"), "cc" -> Json.fromString("dd"))
    val json = Json.obj("root" -> Json.arr(firstEl, secondEl))

    "Default" in {
      Printer.spaces2.pretty(json) shouldEqual
        """root:
          |- a: b
          |  c: d
          |- aa: bb
          |  cc: dd
          |""".stripMargin
    }

    "Indent without indentWithIndicator" in {
      Printer.builder.withIndicatorIndent(2).build().pretty(json) shouldEqual
        """root:
          |  -
          |  a: b
          |  c: d
          |  -
          |  aa: bb
          |  cc: dd
          |""".stripMargin
    }

    "Indent with indentWithIndicator" in {
      Printer.builder.withIndentWithIndicator(true).withIndicatorIndent(2).build().pretty(json) shouldEqual
        """root:
          |  - a: b
          |    c: d
          |  - aa: bb
          |    cc: dd
          |""".stripMargin
    }
  }
}
