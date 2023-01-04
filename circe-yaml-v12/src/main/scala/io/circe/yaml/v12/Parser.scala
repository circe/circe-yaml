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

import io.circe.yaml.common
import org.snakeyaml.engine.v2.api.LoadSettings

object Parser {
  final case class Config(
    allowDuplicateKeys: Boolean = false,
    allowRecursiveKeys: Boolean = false,
    bufferSize: Int = 1024,
    codePointLimit: Int = common.Parser.defaultCodePointLimit,
    label: String = "reader",
    maxAliasesForCollections: Int = common.Parser.defaultMaxAliasesForCollections,
    parseComments: Boolean = false,
    useMarks: Boolean = true
  )

  def make(config: Config = Config()): common.Parser = {
    import config._
    new ParserImpl(
      LoadSettings.builder
        .setAllowDuplicateKeys(allowDuplicateKeys)
        .setAllowRecursiveKeys(allowRecursiveKeys)
        .setBufferSize(bufferSize)
        .setCodePointLimit(codePointLimit)
        .setLabel(label)
        .setMaxAliasesForCollections(maxAliasesForCollections)
        .setParseComments(parseComments)
        .setUseMarks(useMarks)
        .build
    )
  }

  lazy val default: common.Parser = make()
}
