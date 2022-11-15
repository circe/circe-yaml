package io.circe.yaml.v12

import io.circe.yaml.common
import org.snakeyaml.engine.v2.api.LoadSettings

object Parser {
  final case class Config(
    allowDuplicateKeys: Boolean = false,
    allowRecursiveKeys: Boolean = false,
    bufferSize: Int = 1024,
    label: String = "reader",
    maxAliasesForCollections: Int = 50,
    parseComments: Boolean = false,
    useMarks: Boolean = true
  )

  def make(config: Config = Config()): common.Parser = {
    import config._
    new ParserImpl(
      LoadSettings.builder
        .setAllowDuplicateKeys(allowDuplicateKeys)
        .setAllowRecursiveKeys(allowRecursiveKeys)
        .setBufferSize(bufferSize)
        .setLabel(label)
        .setMaxAliasesForCollections(maxAliasesForCollections)
        .setParseComments(parseComments)
        .setUseMarks(useMarks)
        .build
    )
  }

  lazy val default: common.Parser = make()
}
