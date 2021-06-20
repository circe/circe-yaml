package io.circe.yaml

import io.circe.Json
import io.circe.yaml.Printer.{ FlowStyle, LineBreak, StringStyle, YamlVersion }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class PrinterTests extends AnyFreeSpec with Matchers {

  "Flow style" - {
    val json = Json.obj("foo" -> Json.arr((0 until 3).map(_.toString).map(Json.fromString): _*))

    "Block" in {
      val printer = Printer.spaces2.copy(sequenceStyle = FlowStyle.Block, mappingStyle = FlowStyle.Block)
      printer.pretty(json) shouldEqual
        """foo:
          |- '0'
          |- '1'
          |- '2'
          |""".stripMargin
    }

    "Flow" in {
      val printer = Printer.spaces2.copy(sequenceStyle = FlowStyle.Flow, mappingStyle = FlowStyle.Flow)
      printer.pretty(json) shouldEqual
        """{foo: ['0', '1', '2']}
          |""".stripMargin
    }
  }

  "Preserves order" - {
    val kvPairs = Seq("d" -> 4, "a" -> 1, "b" -> 2, "c" -> 3)
    val json = Json.obj(kvPairs.map { case (k, v) => (k -> Json.fromInt(v)) }: _*)
    "true" in {
      val printer = Printer(preserveOrder = true)
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
      val printer = Printer.spaces2.copy(splitLines = false, stringStyle = StringStyle.Plain)
      printer.pretty(json) shouldEqual
        s"""foo: $foosPlain
           |""".stripMargin
    }

    "Double quoted" in {
      val printer = Printer.spaces2.copy(stringStyle = StringStyle.DoubleQuoted)
      printer.pretty(json) shouldEqual
        s"""foo: "${foosSplit.mkString("\\\n  \\ ")}"
           |""".stripMargin
    }

    "Single quoted" in {
      val printer = Printer.spaces2.copy(stringStyle = StringStyle.SingleQuoted)
      printer.pretty(json) shouldEqual
        s"""foo: '${foosSplit.mkString("\n  ")}'
           |""".stripMargin
    }

    "Folded" in {
      val printer = Printer.spaces2.copy(stringStyle = StringStyle.Folded)
      printer.pretty(json) shouldEqual
        s"""foo: >-
           |  $foosFolded
           |""".stripMargin
    }

    "Literal" in {
      val printer = Printer.spaces2.copy(stringStyle = StringStyle.Literal)
      printer.pretty(json) shouldEqual
        s"""foo: |-
           |  $foosPlain
           |""".stripMargin
    }

  }

  "Plain with newlines" in {
    val json = Json.obj("foo" -> Json.fromString("abc\nxyz\n"))
    val printer = Printer.spaces2.copy(stringStyle = StringStyle.Plain)
    printer.pretty(json) shouldEqual
      s"""foo: |
         |  abc
         |  xyz
         |""".stripMargin
  }

  "Drop null keys" in {
    val json = Json.obj("nullField" -> Json.Null, "nonNullField" -> Json.fromString("foo"))
    Printer.spaces2.copy(dropNullKeys = true).pretty(json) shouldEqual "nonNullField: foo\n"
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

  "Version" in {
    val json = Json.fromString("foo")
    Printer.spaces2.copy(version = YamlVersion.Yaml1_1).pretty(json) shouldEqual
      """%YAML 1.1
        |--- foo
        |""".stripMargin
    Printer.spaces2.copy(version = YamlVersion.Yaml1_0).pretty(json) shouldEqual
      """%YAML 1.0
        |--- foo
        |""".stripMargin
  }

  "Line break" - {
    val json = Json.arr(Json.fromString("foo"), Json.fromString("bar"))

    "Unix" in {
      Printer.spaces2.copy(lineBreak = LineBreak.Unix).pretty(json) shouldEqual
        "- foo\n- bar\n"
    }

    "Windows" in {
      Printer.spaces2.copy(lineBreak = LineBreak.Windows).pretty(json) shouldEqual
        "- foo\r\n- bar\r\n"
    }

    "Mac" in {
      Printer.spaces2.copy(lineBreak = LineBreak.Mac).pretty(json) shouldEqual
        "- foo\r- bar\r"
    }
  }

}
