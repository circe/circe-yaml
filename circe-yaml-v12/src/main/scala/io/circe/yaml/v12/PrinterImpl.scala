/*
 * Copyright 2016 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.yaml.v12

import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject
import io.circe.yaml.common.Printer._
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.StreamDataWriter
import org.snakeyaml.engine.v2.common
import org.snakeyaml.engine.v2.emitter.Emitter
import org.snakeyaml.engine.v2.nodes._
import org.snakeyaml.engine.v2.serializer.Serializer

import java.io.StringWriter
import scala.collection.JavaConverters._

private class PrinterImpl(
  stringStyle: StringStyle,
  preserveOrder: Boolean,
  dropNullKeys: Boolean,
  mappingStyle: FlowStyle,
  sequenceStyle: FlowStyle,
  options: DumpSettings
) extends io.circe.yaml.common.Printer {

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
    else stringStyle.toScalarStyle

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
