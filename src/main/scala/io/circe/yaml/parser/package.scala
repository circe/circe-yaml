package io.circe.yaml

import io.circe.{ParsingFailure, Json}

package object parser {

  type Parser = String => Either[ParsingFailure, Json]

  val parse: Parser = (yaml: String) => Parser.parse(yaml).toEither
}
