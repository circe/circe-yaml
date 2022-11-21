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
