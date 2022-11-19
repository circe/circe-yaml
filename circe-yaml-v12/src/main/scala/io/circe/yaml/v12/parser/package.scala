package io.circe.yaml.v12

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
  def parse(yaml: Reader): Either[ParsingFailure, Json] = Parser.default.parse(yaml)

  def parse(yaml: String): Either[ParsingFailure, Json] = Parser.default.parse(yaml)

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = Parser.default.parseDocuments(yaml)
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = Parser.default.parseDocuments(yaml)

  final def decode[A: Decoder](input: Reader): Either[Error, A] = Parser.default.decode[A](input)
  final def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[Error, A] =
    Parser.default.decodeAccumulating[A](input)
}
