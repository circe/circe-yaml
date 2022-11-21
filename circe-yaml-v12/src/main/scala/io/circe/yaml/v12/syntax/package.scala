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

/**
 * Provides helpful syntax that is not specific to the SnakeYAML implementation.
 */
package object syntax {

  final class YamlSyntax(val tree: Json) extends AnyVal {
    def spaces2: String = Printer.spaces2.pretty(tree)
    def spaces4: String = Printer.spaces4.pretty(tree)
  }

  /**
   * Call this to serialize a [[Json]] AST into a YAML string using the default options.
   */
  implicit class AsYaml(val tree: Json) {
    def asYaml: YamlSyntax = new YamlSyntax(tree)
  }
}
