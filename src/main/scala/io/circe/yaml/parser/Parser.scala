package io.circe.yaml.parser


import cats.data.ValidatedNel
import cats.syntax.either._
import io.circe._
import java.io.{Reader, StringReader}
import org.yaml.snakeyaml.Yaml
import scala.collection.JavaConverters._

class Parser(algebra: NodeAlg[Json] = new DefaultAlg) {

  /**
    * Configure the parser
    * @param numericTimestamps if true, timestamps will be returned as epoch millisecond [[Long]]s
    * @return A configured parser
    */
  def configured(
    numericTimestamps: Boolean = false
  ): Parser = new Parser(ConfiguredAlg(
    numericTimestamps = numericTimestamps
  ))


  /**
    * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
    */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = for {
    parsed <- parseSingle(yaml)
    json   <- Either.catchNonFatal(algebra.any(parsed)).leftMap {
      case p @ ParsingFailure(_, _) => p
      case err => ParsingFailure(err.getMessage, err)
    }
  } yield json

  /**
    * Parse YAML from the given [[Reader]], accumulating errors and returning either a list of [[ParsingFailure]]s
    * or a [[Json]]
    */
  def parseAccumulating(yaml: Reader): ValidatedNel[ParsingFailure, Json] = parseSingle(yaml).toValidatedNel andThen {
    parsed => new AccumulatingAlg(algebra).any(parsed)
  }

  /**
    * Parse YAML from the given string, returning either [[ParsingFailure]] or [[Json]]
    */
  def parse(yaml: String): Either[ParsingFailure, Json] = parse(new StringReader(yaml))

  /**
    * Parse YAML from the given string, accumulating errors and returning either a list of [[ParsingFailure]]s
    * or a [[Json]]
    */
  def parseAccumulating(yaml: String): ValidatedNel[ParsingFailure, Json] = parseAccumulating(new StringReader(yaml))

  /**
    * Parse a succession of documents from the given [[Reader]], returning the result as a [[Stream]] of [[Either]]
    */
  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = {
    val alg = new LiftedAlg(algebra)
    parseStream(yaml).map(alg.any)
  }

  /**
    * Parse a succession of documents from the given [[Reader]], accumulating errors within each document and
    * returning the result as a [[Stream]] of [[ValidatedNel]]
    */
  def parseDocumentsAccumulating(yaml: Reader): Stream[ValidatedNel[ParsingFailure, Json]] = {
    val alg = new AccumulatingAlg(algebra)
    parseStream(yaml).map(alg.any)
  }

  /**
    * Parse a succession of documents from the given string, returning the result as a [[Stream]] of [[Either]]
    */
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = parseDocuments(new StringReader(yaml))

  /**
    * Parse a succession of documents from the given string, accumulating errors within each document and
    * returning the result as a [[Stream]] of [[ValidatedNel]]
    */
  def parseDocumentsAccumulating(yaml: String): Stream[ValidatedNel[ParsingFailure, Json]] =
    parseDocumentsAccumulating(new StringReader(yaml))

  private[this] def parseSingle(reader: Reader) =
    Either.catchNonFatal(new Yaml().compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  private[this] def parseStream(reader: Reader) =
    new Yaml().composeAll(reader).asScala.toStream

}
