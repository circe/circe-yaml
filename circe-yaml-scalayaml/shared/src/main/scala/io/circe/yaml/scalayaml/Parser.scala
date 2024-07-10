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

package io.circe.yaml.scalayaml

import cats.data.ValidatedNel
import cats.syntax.all._
import io.circe
import io.circe.Decoder
import io.circe.Json
import io.circe.ParsingFailure
import org.virtuslab.yaml._

import java.io.Reader
import scala.collection.mutable

object Parser extends io.circe.yaml.common.Parser {

  private def readerToString(yaml: Reader): String = {
    val buffer = new Array[Char](4 * 1024)
    val builder = new mutable.StringBuilder(4 * 1024)
    var readBytes = -1
    while ({ readBytes = yaml.read(buffer); readBytes } > 0)
      builder.appendAll(buffer, 0, readBytes)
    builder.result()
  }

  override def parse(yaml: Reader): Either[ParsingFailure, Json] = {
    val string = readerToString(yaml)
    parse(string)
  }

  override def parseDocuments(yaml: Reader): Stream[Either[ParsingFailure, Json]] = {
    val string = readerToString(yaml)
    val parsed = parse(string)
    Stream(parsed)
  }

  override def parseDocuments(yaml: String): Stream[Either[ParsingFailure, Json]] = {
    val parsed = parse(yaml)
    Stream(parsed)
  }

  override def decode[A: Decoder](input: Reader): Either[circe.Error, A] = {
    val string = readerToString(input)
    val parsed = parse(string)
    parsed.flatMap(_.as[A])
  }

  override def decodeAccumulating[A: Decoder](input: Reader): ValidatedNel[circe.Error, A] =
    decode(input).toValidatedNel

  override def parse(input: String): Either[ParsingFailure, Json] = {
    val node = input.asNode
    node match {
      case Right(node) => nodeToJson(node)
      case Left(error) => Left(errorToFailure(error))
    }
  }

  private def scalarNodeToJson(node: Node.ScalarNode): Either[ParsingFailure, Json] = {
    val parsed = YamlDecoder.forAny.construct(node).left.map(errorToFailure)
    parsed.flatMap {
      case null | None   => Json.Null.asRight
      case value: String => Json.fromString(value).asRight
      case value: Int    => Json.fromInt(value).asRight
      case double: Double =>
        Json.fromDouble(double).toRight(ParsingFailure(s"${node.value} cannot be represented as a JSON number.", null))
      case value: Boolean => Json.fromBoolean(value).asRight
      case value: Long    => Json.fromLong(value).asRight
      case float: Float =>
        Json.fromFloat(float).toRight(ParsingFailure(s"${node.value} cannot be represented as a JSON number.", null))
      case value: BigDecimal => Json.fromBigDecimal(value).asRight
      case value: BigInt     => Json.fromBigInt(value).asRight
      case value: Byte       => Json.fromInt(value.toInt).asRight
      case value: Short      => Json.fromInt(value.toInt).asRight
      case value => ParsingFailure(s"Can't map ${value.getClass.getSimpleName} (${node.value}) to JSON.", null).asLeft
    }
  }

  private def nodeToJson(node: Node): Either[ParsingFailure, Json] = node match {
    case node: Node.ScalarNode =>
      scalarNodeToJson(node)
    case Node.SequenceNode(nodes, _) =>
      val values = nodes.traverse(nodeToJson)
      values.map(Json.fromValues)
    case Node.MappingNode(mappings, _) =>
      val fields = mappings.toList.traverse {
        case (Node.ScalarNode(key, _), value) =>
          nodeToJson(value).map(key -> _)
        case (node, _) =>
          Left(ParsingFailure(s"Unexpected ${node.getClass.getSimpleName} type, expected ScalarNode.", null))
      }
      fields.map(Json.fromFields)
  }

  private def errorToFailure(error: YamlError): ParsingFailure =
    ParsingFailure(s"Parsing failed: ${error.msg}", error)
}
