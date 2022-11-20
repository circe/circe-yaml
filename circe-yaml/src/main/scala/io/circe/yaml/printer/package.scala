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

import io.circe.Json

package object printer extends io.circe.yaml.common.Printer {

  /**
   * A default printer implementation using Snake YAML.
   */
  def print(tree: Json): String = pretty(tree)
  def pretty(tree: Json): String = Printer.spaces2.pretty(tree)
}
