package io.circe.yaml

import io.circe.yaml.common.Printer.StringStyle
import org.snakeyaml.engine.v2.common.ScalarStyle

package object v12 {

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
