package io.circe.yaml

import io.circe._

import java.io.Reader

package object parser {
  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = Parser.defaultParser.parse(yaml)

  def parse(yaml: String): Either[ParsingFailure, Json] = Parser.defaultParser.parse(yaml)

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = Parser.defaultParser.parseDocuments(yaml)
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = Parser.defaultParser.parseDocuments(yaml)
}
