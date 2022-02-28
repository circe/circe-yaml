package io.circe.yaml

import cats.syntax.either._
import io.circe._
import java.io._
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._

package object parser extends Parser {

  /**
   * Parse YAML from the given [[Reader]], returning either [[ParsingFailure]] or [[Json]]
   * @param yaml
   * @return
   */
  def parse(yaml: Reader): Either[ParsingFailure, Json] = Parser.default.parse(yaml)

  def parse(yaml: String): Either[ParsingFailure, Json] = Parser.default.parse(yaml)

  def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = Parser.default.parseDocuments(yaml)
  def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = Parser.default.parseDocuments(yaml)

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private val loaderOptions = {
    val options = new LoaderOptions()
    options.setMaxAliasesForCollections(50)
    options
  }

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] def parseSingle(reader: Reader): Either[ParsingFailure, Node] =
    Either.catchNonFatal(new Yaml(loaderOptions).compose(reader)).leftMap(err => ParsingFailure(err.getMessage, err))

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] def parseStream(reader: Reader): Stream[Node] =
    new Yaml(loaderOptions).composeAll(reader).asScala.toStream

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] object CustomTag {
    def unapply(tag: Tag): Option[String] = if (!tag.startsWith(Tag.PREFIX))
      Some(tag.getValue)
    else
      None
  }

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] class FlatteningConstructor extends Parser.FlatteningConstructor

  @deprecated("moved to Parser.CustomTag", since = "0.14.2")
  private[this] def yamlToJson(node: Node): Either[ParsingFailure, Json] = Parser.yamlToJson(node)
}
