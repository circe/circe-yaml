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

package io.circe.yaml.literal

import org.virtuslab.yaml._
import scala.reflect.macros.blackbox

class YamlLiteralMacros(val c: blackbox.Context) {
  import c.universe._

  def yamlImpl(args: c.Expr[Any]*): c.Expr[io.circe.Json] = {
    val stringParts = c.prefix.tree match {
      case Apply(_, List(Apply(_, parts))) =>
        parts.map {
          case Literal(Constant(part: String)) => part
          case _ => c.abort(c.enclosingPosition, "Expected string literal parts")
        }
      case _ => c.abort(c.enclosingPosition, "Expected a string interpolation")
    }

    if (args.isEmpty) {
      val yamlString = applyStripMargin(stringParts.mkString)
      c.Expr[io.circe.Json](parseAndConvert(yamlString))
    } else {
      val replacements = args.toList.map { arg =>
        val placeholder = java.util.UUID.randomUUID().toString
        Replacement(placeholder, arg.tree)
      }

      val yamlString = applyStripMargin(
        stringParts
          .zipAll(replacements.map(_.placeholder), "", "")
          .map { case (part, ph) => part + ph }
          .mkString
      )

      c.Expr[io.circe.Json](parseAndConvertWithReplacements(yamlString, replacements))
    }
  }

  private case class Replacement(placeholder: String, arg: Tree)

  private def applyStripMargin(s: String): String = {
    val lines = s.split("\n", -1).toList
    val hasMargin = lines.exists(_.trim.startsWith("|"))
    if (hasMargin) s.stripMargin else s
  }

  private def parseAndConvert(yamlString: String): Tree =
    io.circe.yaml.scalayaml.Parser.parse(yamlString) match {
      case Right(json) => liftJson(json)
      case Left(error) => c.abort(c.enclosingPosition, s"Invalid YAML: ${error.message}")
    }

  private def parseAndConvertWithReplacements(yamlString: String, replacements: List[Replacement]): Tree = {
    val placeholderMap = replacements.map(r => r.placeholder -> r).toMap

    yamlString.asNode match {
      case Right(node) => nodeToTreeWithReplacements(node, placeholderMap)
      case Left(error) => c.abort(c.enclosingPosition, s"Invalid YAML: ${error.msg}")
    }
  }

  private def liftJson(json: io.circe.Json): Tree =
    json.fold(
      q"_root_.io.circe.Json.Null",
      b => if (b) q"_root_.io.circe.Json.True" else q"_root_.io.circe.Json.False",
      n =>
        n.toLong match {
          case Some(l) => q"_root_.io.circe.Json.fromLong($l)"
          case None =>
            n.toBigDecimal match {
              case Some(bd) =>
                val s = bd.toString
                q"_root_.io.circe.Json.fromBigDecimal(BigDecimal($s))"
              case None =>
                val d = n.toDouble
                q"_root_.io.circe.Json.fromDoubleOrNull($d)"
            }
        },
      s => q"_root_.io.circe.Json.fromString($s)",
      arr => {
        val elements = arr.map(liftJson).toList
        q"_root_.io.circe.Json.arr(..$elements)"
      },
      obj => {
        val fields = obj.toList.map { case (k, v) =>
          val valueTree = liftJson(v)
          q"(${Literal(Constant(k))}, $valueTree)"
        }
        q"_root_.io.circe.Json.obj(..$fields)"
      }
    )

  private def nodeToTreeWithReplacements(node: Node, placeholders: Map[String, Replacement]): Tree = node match {
    case Node.ScalarNode(value, _) =>
      findReplacement(value, placeholders) match {
        case Some(replacement) => encoderApply(replacement.arg)
        case None              => liftJson(io.circe.yaml.scalayaml.Parser.parse(value).getOrElse(io.circe.Json.fromString(value)))
      }
    case Node.SequenceNode(nodes, _) =>
      val elements = nodes.map(n => nodeToTreeWithReplacements(n, placeholders))
      q"_root_.io.circe.Json.arr(..$elements)"
    case Node.MappingNode(mappings, _) =>
      val fields = mappings.toList.map {
        case (Node.ScalarNode(key, _), value) =>
          val valueTree = nodeToTreeWithReplacements(value, placeholders)
          val keyTree = findReplacement(key, placeholders) match {
            case Some(replacement) => keyEncoderApply(replacement.arg)
            case None              => Literal(Constant(key))
          }
          q"($keyTree, $valueTree)"
        case (other, _) =>
          c.abort(c.enclosingPosition, s"YAML mapping keys must be scalars, got ${other.getClass.getSimpleName}")
      }
      q"_root_.io.circe.Json.obj(..$fields)"
    case other =>
      c.abort(c.enclosingPosition, s"Unsupported YAML node type: ${other.getClass.getSimpleName}")
  }

  private def findReplacement(value: String, placeholders: Map[String, Replacement]): Option[Replacement] =
    placeholders.collectFirst {
      case (ph, r) if value.contains(ph) => r
    }

  private def encoderApply(arg: Tree): Tree = {
    val tpe = c.typecheck(arg).tpe
    val encoder = c.inferImplicitValue(appliedType(typeOf[io.circe.Encoder[_]].typeConstructor, tpe))
    if (encoder.isEmpty)
      c.abort(arg.pos, s"Could not find an implicit Encoder for type $tpe.")
    q"$encoder.apply($arg)"
  }

  private def keyEncoderApply(arg: Tree): Tree = {
    val tpe = c.typecheck(arg).tpe
    val encoder = c.inferImplicitValue(appliedType(typeOf[io.circe.KeyEncoder[_]].typeConstructor, tpe))
    if (encoder.isEmpty)
      c.abort(arg.pos, s"Could not find an implicit KeyEncoder for type $tpe.")
    q"$encoder.apply($arg)"
  }
}
