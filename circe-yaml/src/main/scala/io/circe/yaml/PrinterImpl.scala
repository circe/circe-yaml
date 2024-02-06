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

package io.circe.yaml

import io.circe.yaml.PrinterBuilder.SnakeStringStyle
import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.yaml.common.Printer.*
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.nodes.{ MappingNode, Node, NodeTuple, ScalarNode, SequenceNode, Tag }
import org.yaml.snakeyaml.resolver.Resolver
import org.yaml.snakeyaml.serializer.Serializer

import java.io.StringWriter

import scala.collection.JavaConverters.*

class PrinterImpl(
  stringStyle: StringStyle,
  preserveOrder: Boolean,
  dropNullKeys: Boolean,
  mappingStyle: FlowStyle,
  sequenceStyle: FlowStyle,
  options: DumperOptions
) extends io.circe.yaml.common.Printer {

  import PrinterImpl.*

  def pretty(json: Json): String = {
    val rootTag = yamlTag(json)
    val writer = new StringWriter()
    val serializer = new Serializer(new Emitter(writer, options), new Resolver, options, rootTag)
    serializer.open()
    serializer.serialize(jsonToYaml(json))
    serializer.close()
    writer.toString
  }

  private def isBad(s: String): Boolean = s.indexOf('\u0085') >= 0 || s.indexOf('\ufeff') >= 0

  private def hasNewline(s: String): Boolean = s.indexOf('\n') >= 0

  private def scalarStyle(value: String): DumperOptions.ScalarStyle =
    if (isBad(value)) DumperOptions.ScalarStyle.DOUBLE_QUOTED else DumperOptions.ScalarStyle.PLAIN

  private def stringScalarStyle(value: String): DumperOptions.ScalarStyle =
    if (isBad(value)) DumperOptions.ScalarStyle.DOUBLE_QUOTED
    else if (stringStyle == StringStyle.Plain && hasNewline(value)) DumperOptions.ScalarStyle.LITERAL
    else stringStyle.toScalarStyle

  private def scalarNode(tag: Tag, value: String) = new ScalarNode(tag, value, null, null, scalarStyle(value))

  private def stringNode(value: String) = new ScalarNode(Tag.STR, value, null, null, stringScalarStyle(value))

  private def keyNode(value: String) = new ScalarNode(Tag.STR, value, null, null, scalarStyle(value))

  private def jsonToYaml(json: Json): Node = {

    def convertObject(obj: JsonObject): MappingNode = {
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
        if (mappingStyle == FlowStyle.Flow) DumperOptions.FlowStyle.FLOW else DumperOptions.FlowStyle.BLOCK
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
          if (sequenceStyle == FlowStyle.Flow) DumperOptions.FlowStyle.FLOW else DumperOptions.FlowStyle.BLOCK
        ),
      obj => convertObject(obj)
    )
  }

}

object PrinterImpl {
  private def numberTag(number: JsonNumber): Tag =
    if (number.toString.contains(".")) Tag.FLOAT else Tag.INT

  private def yamlTag(json: Json): Tag = json.fold(
    Tag.NULL,
    _ => Tag.BOOL,
    number => numberTag(number),
    _ => Tag.STR,
    _ => Tag.SEQ,
    _ => Tag.MAP
  )
}
