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

package io.circe.yaml.common

import io.circe.Json

trait Printer {
  def pretty(json: Json): String
}

object Printer {

  sealed trait FlowStyle
  object FlowStyle {
    case object Flow extends FlowStyle
    case object Block extends FlowStyle
  }

  sealed trait LineBreak
  object LineBreak {
    case object Unix extends LineBreak
    case object Windows extends LineBreak
    case object Mac extends LineBreak
  }

  sealed trait StringStyle
  object StringStyle {
    case object Plain extends StringStyle
    case object DoubleQuoted extends StringStyle
    case object SingleQuoted extends StringStyle
    case object Literal extends StringStyle
    case object Folded extends StringStyle
  }

  sealed trait NonPrintableStyle
  object NonPrintableStyle {
    case object Binary extends NonPrintableStyle
    case object Escape extends NonPrintableStyle
  }
}
