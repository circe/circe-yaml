package io.circe.yaml.snake

import io.circe.{Json, ParsingFailure}

import scala.util.{Failure, Success, Try}

/**
  * Uses [[org.yaml.snakeyaml.Yaml]] to parse YAML into Circe's [[Json]] AST.
  *
  * @note This class is not thread-safe, you must create one per thread.
  *       See [[org.yaml.snakeyaml.Yaml]] for more information.
  *
  * @note This class is final to prevent extending this with a singleton object.
  *       Also, the implementation is pretty trivial, so it should be easy to make
  *       an alternative.
  *
  * @param yaml the [[org.yaml.snakeyaml.Yaml]] instance
  * @param converter a function that converts Any => [[Json]]
  */
private[snake] final class SnakeYamlParser(yaml: JavaSnakeYaml, converter: CirceJsonConverter = CirceJsonConverter.default) {

  def parse(str: String): Either[ParsingFailure, Json] = {
    Try(converter.convert(yaml.load(str))) match {
      case Success(json) => Right(json)
      case Failure(ex) => Left(ParsingFailure(s"Could not parse Yaml:\n$str", ex))
    }
  }
}
