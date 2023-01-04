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

import cats.data.ValidatedNel
import cats.syntax.either._
import io.circe._
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes._

import java.io._
import scala.collection.JavaConverters._

package object parser extends io.circe.yaml.common.Parser {

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = Parser.default.parse(yaml)

  def parse(yaml: String): Either[ParsingFailure, Json] = Parser.default.parse(yaml)

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = Parser.default.parseDocuments(yaml)
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = Parser.default.parseDocuments(yaml)

  final def decode[A: Decoder](input: Reader): Either[Error, A] = Parser.default.decode[A](input)

  final def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[Error, A] =
    Parser.default.decodeAccumulating[A](input)

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private val loaderOptions: LoaderOptions = {
    val options = new LoaderOptions()
    options.setMaxAliasesForCollections(common.Parser.defaultMaxAliasesForCollections)
    options.setNestingDepthLimit(Parser.defaultNestingDepthLimit)
    options.setCodePointLimit(common.Parser.defaultCodePointLimit)
    options
  }

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] def parseSingle(reader: Reader): Either[ParsingFailure, Node] =
    Either.catchNonFatal(new Yaml(loaderOptions).compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] def parseStream(reader: Reader): Stream[Node] =
    new Yaml(loaderOptions).composeAll(reader).asScala.toStream

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] object CustomTag {
    def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] class FlatteningConstructor extends Parser.FlatteningConstructor(loaderOptions)

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = Parser.yamlToJson(node, loaderOptions)
}
