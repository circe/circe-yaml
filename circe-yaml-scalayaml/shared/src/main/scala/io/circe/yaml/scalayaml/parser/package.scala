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

package io.circe.yaml.scalayaml

import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.Error
import io.circe.Json
import io.circe.ParsingFailure

import java.io.Reader

package object parser extends io.circe.yaml.common.Parser {

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = Parser.parse(yaml)

  def parse(yaml: String): Either[ParsingFailure, Json] = Parser.parse(yaml)

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = Parser.parseDocuments(yaml)
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = Parser.parseDocuments(yaml)

  final def decode[A: Decoder](input: Reader): Either[Error, A] = Parser.decode[A](input)
  final def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[Error, A] =
    Parser.decodeAccumulating[A](input)
}
