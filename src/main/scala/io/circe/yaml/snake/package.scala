package io.circe.yaml

import io.circe.Json
import org.yaml.snakeyaml
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.DumperOptions.{FlowStyle, Version}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

/**
  * This package provides implementations of [[printer.Printer]] and [[parser.Parser]]
  * using [[org.yaml.snakeyaml.Yaml]].
  *
  * You should use the [[printer.print]] and [[parser.parse]] methods for a more generic implementation
  * that will be easier to refactor if other implementations are used in the future.
  *
  * However, if you need access to SnakeYAML specific features, you can import io.circe.yaml.snake._
  * just note that this is incompatible with import io.circe.yaml.syntax._ as it will provide SnakeYAML
  * specific syntax.
  */
package object snake {

  implicit class SnakeYamlSyntax(val tree: Json) extends AnyVal {
    def asYamlString: String = printer.print(tree)
    def asYamlString(opts: SnakeYamlPrinterOptions): String = printer.print(tree, opts)
  }

  /**
    * a simple cache from immutable [[SnakeYamlPrinterOptions]] to a [[org.yaml.snakeyaml.Yaml]] instance
    * on the current thread.
    */
  private object CurrentThreadSnakeYamlInstances extends ThreadLocal[Map[SnakeYamlPrinterOptions, JavaSnakeYaml]] {
    override def initialValue(): Map[SnakeYamlPrinterOptions, JavaSnakeYaml] = Map.empty
  }

  type JavaSnakeYaml = snakeyaml.Yaml
  object JavaSnakeYaml {
    def apply(
      config: SnakeYamlConfigs = SnakeYamlConfigs.default,
      printerOptions: SnakeYamlPrinterOptions = SnakeYamlPrinterOptions.default
    ): JavaSnakeYaml = {
      val instances = CurrentThreadSnakeYamlInstances.get()
      instances.getOrElse(printerOptions, {
        val yaml = new JavaSnakeYaml(config.constructor, config.representer, printerOptions, config.resolver)
        // Drop LRU cache items. Using max size of 4 to insure insert order of immutable map instances.
        val updated = instances.drop(instances.size - 3) + (printerOptions -> yaml)
        CurrentThreadSnakeYamlInstances.set(updated)
        yaml
      })
    }
  }

  implicit def fromDumperOptions(opts: DumperOptions): SnakeYamlPrinterOptions = {
    // TODO: Support the following?
    // dumperOptions.getAnchorGenerator
    // dumperOptions.getTimeZone
    SnakeYamlPrinterOptions(
      opts.getIndent,
      opts.getWidth,
      opts.getSplitLines,
      opts.getIndicatorIndent,
      Option(opts.getTags).map(_.asScala.toMap).getOrElse(SnakeYamlPrinterOptions.default.tags),
      Option(opts.getDefaultFlowStyle).getOrElse(SnakeYamlPrinterOptions.default.defaultFlowStyle),
      opts.getDefaultScalarStyle,
      opts.getLineBreak,
      Option(opts.getVersion)
    )
  }

  implicit def toDumperOptions(opts: SnakeYamlPrinterOptions): DumperOptions = {
    val dumperOptions = new DumperOptions
    dumperOptions.setIndent(opts.indent)
    dumperOptions.setWidth(opts.maxScalarWidth)
    dumperOptions.setSplitLines(opts.splitLines)
    dumperOptions.setIndicatorIndent(opts.indicatorIndent)
    dumperOptions.setTags(opts.tags.asJava)
    dumperOptions.setDefaultFlowStyle(opts.defaultFlowStyle)
    dumperOptions.setDefaultScalarStyle(opts.defaultScalarStyle)
    dumperOptions.setLineBreak(opts.lineBreak)
    opts.version foreach dumperOptions.setVersion
    dumperOptions
  }

  def printer: SnakeYamlPrinter = new SnakeYamlPrinter(SnakeYamlConfigs.default)

  def parser: SnakeYamlParser = new SnakeYamlParser(JavaSnakeYaml())
}
