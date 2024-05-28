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
