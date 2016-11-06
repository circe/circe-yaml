package io.circe.yaml

import io.circe.{Json, ParsingFailure}

package object parser {

  type Parser = String => Either[ParsingFailure, Json]

  /**
    * A default parser implementation using Snake YAML.
    */
  val parse: Parser = (yaml: String) => snake.parser.parse(yaml)
}
