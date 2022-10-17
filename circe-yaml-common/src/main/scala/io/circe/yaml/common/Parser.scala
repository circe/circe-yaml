package io.circe.yaml.common

import io.circe.{ Json, ParsingFailure }
import java.io.Reader

trait Parser {

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   *
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json]
  def parse(yaml: String): Either[ParsingFailure, Json]
  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]]
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]]
}
