package io.circe.yaml.common

import cats.data.ValidatedNel
import io.circe.{ Decoder, Error, Json, ParsingFailure }
import java.io.Reader

trait Parser extends io.circe.Parser {

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   *
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json]
  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]]
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]]
  def decode[A: Decoder](input: Reader): Either[Error, A]
  def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[Error, A]
}
