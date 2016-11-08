package io.circe.yaml

import java.io.Reader

import io.circe.yaml.parser.Parser
import io.circe.{Json, ParsingFailure}

trait YamlParser {

  def parse(yaml: String): Either[ParsingFailure, Json]

  def stream(reader: Reader): Stream[Either[ParsingFailure, Json]]
}

object YamlParser extends YamlParser {

  override def parse(yaml: String): Either[ParsingFailure, Json] = Parser.parse(yaml).toEither

  override def stream(reader: Reader): Stream[Either[ParsingFailure, Json]] = Parser.parseDocuments(reader).map(_.toEither)
}
