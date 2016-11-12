package io.circe.yaml.parser

import io.circe.{ParsingFailure, Json}

object parse extends Parser {
  override def apply(yaml: String): Either[ParsingFailure, Json] = {
    Parser.parse(yaml).toEither
  }
}
