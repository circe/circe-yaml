package io.circe

import io.circe.yaml.Printer.StringStyle
import org.yaml.snakeyaml.DumperOptions.ScalarStyle

package object yaml {

  implicit class StringStyleOps(private val style: StringStyle) extends AnyVal {
    def toScalarStyle: ScalarStyle = style match {
      case StringStyle.Plain        => ScalarStyle.PLAIN
      case StringStyle.DoubleQuoted => ScalarStyle.DOUBLE_QUOTED
      case StringStyle.SingleQuoted => ScalarStyle.SINGLE_QUOTED
      case StringStyle.Literal      => ScalarStyle.LITERAL
      case StringStyle.Folded       => ScalarStyle.FOLDED
    }
  }

}
