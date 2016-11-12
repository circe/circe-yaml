package io.circe.yaml

import io.circe.{ParsingFailure, Json}

package object parser {

  type Parser = String => Either[ParsingFailure, Json]
}
