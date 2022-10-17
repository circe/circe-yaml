package io.circe.yaml.v12

import io.circe._
import io.circe.yaml.v12.Printer._
import java.io.StringWriter
import org.snakeyaml.engine.v2.api.{ DumpSettings, StreamDataWriter }
import org.snakeyaml.engine.v2.common
import org.snakeyaml.engine.v2.emitter.Emitter
import org.snakeyaml.engine.v2.nodes._
import org.snakeyaml.engine.v2.serializer.Serializer
import scala.collection.JavaConverters._

trait Printer {
  def pretty(json: Json): String
}

class PrinterImpl(
  stringStyle: StringStyle,
  preserveOrder: Boolean,
  dropNullKeys: Boolean,
  mappingStyle: FlowStyle,
  sequenceStyle: FlowStyle,
  options: DumpSettings
) extends Printer {

  import PrinterImpl._

  def pretty(json: Json): String = {
    val writer = new StreamToStringWriter
    val serializer = new Serializer(options, new Emitter(options, writer))
    serializer.emitStreamStart()
    serializer.serializeDocument(jsonToYaml(json))
    serializer.emitStreamEnd()
    writer.toString
  }

  private def isBad(s: String): Boolean = s.indexOf('\u0085') >= 0 || s.indexOf('\ufeff') >= 0
  private def hasNewline(s: String): Boolean = s.indexOf('\n') >= 0

  private def scalarStyle(value: String): common.ScalarStyle =
    if (isBad(value)) common.ScalarStyle.DOUBLE_QUOTED else common.ScalarStyle.PLAIN

  private def stringScalarStyle(value: String): common.ScalarStyle =
    if (isBad(value)) common.ScalarStyle.DOUBLE_QUOTED
    else if (stringStyle == StringStyle.Plain && hasNewline(value)) common.ScalarStyle.LITERAL
    else StringStyle.toScalarStyle(stringStyle)

  private def scalarNode(tag: Tag, value: String) = new ScalarNode(tag, value, scalarStyle(value))
  private def stringNode(value: String) = new ScalarNode(Tag.STR, value, stringScalarStyle(value))
  private def keyNode(value: String) = new ScalarNode(Tag.STR, value, scalarStyle(value))

  private def jsonToYaml(json: Json): Node = {

    def convertObject(obj: JsonObject) = {
      val fields = if (preserveOrder) obj.keys else obj.keys.toSet
      val m = obj.toMap
      val childNodes = fields.flatMap { key =>
        val value = m(key)
        if (!dropNullKeys || !value.isNull) Some(new NodeTuple(keyNode(key), jsonToYaml(value)))
        else None
      }
      new MappingNode(
        Tag.MAP,
        childNodes.toList.asJava,
        if (mappingStyle == FlowStyle.Flow) common.FlowStyle.FLOW else common.FlowStyle.BLOCK
      )
    }

    json.fold(
      scalarNode(Tag.NULL, "null"),
      bool => scalarNode(Tag.BOOL, bool.toString),
      number => scalarNode(numberTag(number), number.toString),
      str => stringNode(str),
      arr =>
        new SequenceNode(
          Tag.SEQ,
          arr.map(jsonToYaml).asJava,
          if (sequenceStyle == FlowStyle.Flow) common.FlowStyle.FLOW else common.FlowStyle.BLOCK
        ),
      obj => convertObject(obj)
    )
  }
}

object PrinterImpl {
  private def numberTag(number: JsonNumber): Tag =
    if (number.toString.contains(".")) Tag.FLOAT else Tag.INT

  private class StreamToStringWriter extends StringWriter with StreamDataWriter {
    override def flush(): Unit = super.flush() // to fix "conflicting members"
  }
}

object Printer {
  final case class Config(
    preserveOrder: Boolean = false,
    dropNullKeys: Boolean = false,
    indent: Int = 2,
    maxScalarWidth: Int = 80,
    splitLines: Boolean = true,
    indicatorIndent: Int = 0,
    tags: Map[String, String] = Map.empty,
    sequenceStyle: FlowStyle = FlowStyle.Block,
    mappingStyle: FlowStyle = FlowStyle.Block,
    stringStyle: StringStyle = StringStyle.Plain,
    lineBreak: LineBreak = LineBreak.Unix,
    explicitStart: Boolean = false,
    explicitEnd: Boolean = false
  )

  def make(config: Config = Config()): Printer = {
    import config._
    new PrinterImpl(
      stringStyle,
      preserveOrder,
      dropNullKeys,
      mappingStyle,
      sequenceStyle,
      DumpSettings
        .builder()
        .setIndent(indent)
        .setWidth(maxScalarWidth)
        .setSplitLines(splitLines)
        .setIndicatorIndent(indicatorIndent)
        .setTagDirective(tags.asJava)
        .setDefaultScalarStyle(StringStyle.toScalarStyle(stringStyle))
        .setExplicitStart(explicitStart)
        .setExplicitEnd(explicitEnd)
        .setBestLineBreak {
          lineBreak match {
            case LineBreak.Unix    => "\n"
            case LineBreak.Windows => "\r\n"
            case LineBreak.Mac     => "\r"
          }
        }
        .build()
    )
  }

  lazy val spaces2: Printer = make()
  lazy val spaces4: Printer = make(Config(indent = 4))

  sealed trait FlowStyle
  object FlowStyle {
    case object Flow extends FlowStyle
    case object Block extends FlowStyle
  }

  sealed trait StringStyle
  object StringStyle {
    case object Plain extends StringStyle
    case object DoubleQuoted extends StringStyle
    case object SingleQuoted extends StringStyle
    case object Literal extends StringStyle
    case object Folded extends StringStyle

    def toScalarStyle(style: StringStyle): common.ScalarStyle = style match {
      case StringStyle.Plain        => common.ScalarStyle.PLAIN
      case StringStyle.DoubleQuoted => common.ScalarStyle.DOUBLE_QUOTED
      case StringStyle.SingleQuoted => common.ScalarStyle.SINGLE_QUOTED
      case StringStyle.Literal      => common.ScalarStyle.LITERAL
      case StringStyle.Folded       => common.ScalarStyle.FOLDED
    }
  }

  sealed trait LineBreak
  object LineBreak {
    case object Unix extends LineBreak
    case object Windows extends LineBreak
    case object Mac extends LineBreak
  }
}
