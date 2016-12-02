package io.circe.yaml

import java.io.{Reader, StringReader}
import scala.collection.JavaConverters._

import cats.syntax.either._
import io.circe.{Json, ParsingFailure}
import org.yaml.snakeyaml.Yaml

package object parser {

  private def parseSingle(reader: Reader) =
    Either.catchNonFatal(new Yaml().compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  private def parseStream(reader: Reader) =
    new Yaml().composeAll(reader).asScala.toStream

  /**
    * A default parser implementation using Snake YAML.
    */
  def parse(yaml: Reader): ParsingFailure Either Json = for {
    parsed <- parseSingle(yaml)
    json   <- yamlToJson(parsed)
  } yield json

  def parse(yaml: String): ParsingFailure Either Json = parse(new StringReader(yaml))

  def parseDocuments(yaml: Reader): Stream[ParsingFailure Either Json] = parseStream(yaml).map(yamlToJson)
  def parseDocuments(yaml: String): Stream[ParsingFailure Either Json] = parseDocuments(new StringReader(yaml))
}
