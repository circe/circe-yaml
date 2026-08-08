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

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject
import io.circe.KeyEncoder
import io.circe.yaml.scalayaml.Parser
import org.virtuslab.yaml.*

import java.util.UUID
import scala.quoted.*

object YamlLiteralMacros {

  def yamlImpl(sc: Expr[StringContext], args: Expr[Seq[Any]])(using Quotes): Expr[Json] = {
    import quotes.reflect._

    val stringParts = sc match {
      case '{ StringContext(${ Varargs(Exprs(parts)) }: _*) } => parts.toList
      case _ => report.errorAndAbort("Expected a string context with literal parts")
    }

    val argExprs: List[Expr[Any]] = args match {
      case Varargs(exprs) => exprs.toList
      case _              => report.errorAndAbort("Expected literal arguments")
    }

    if (argExprs.isEmpty) {
      // No interpolation — just validate and convert at compile time
      val yamlString = applyStripMargin(stringParts.mkString)
      parseAndConvert(yamlString)
    } else {
      // With interpolation — use UUID placeholders
      val replacements = argExprs.map { arg =>
        val placeholder = UUID.randomUUID().toString
        Replacement(placeholder, arg)
      }

      val yamlString = applyStripMargin(
        stringParts.zipAll(replacements.map(_.placeholder), "", "").map { case (part, ph) => part + ph }.mkString
      )

      parseAndConvertWithReplacements(yamlString, replacements)
    }
  }

  private case class Replacement(placeholder: String, arg: Expr[Any])

  private def applyStripMargin(s: String): String = {
    val lines = s.split("\n", -1).toList
    val hasMargin = lines.exists(_.trim.startsWith("|"))
    if (hasMargin) s.stripMargin else s
  }

  private def parseAndConvert(yamlString: String)(using Quotes): Expr[Json] = {
    import quotes.reflect._

    Parser.parse(yamlString) match {
      case Right(json) => liftJson(json)
      case Left(error) => report.errorAndAbort(s"Invalid YAML: ${error.message}")
    }
  }

  private def parseAndConvertWithReplacements(yamlString: String, replacements: List[Replacement])(using
    Quotes
  ): Expr[Json] = {
    import quotes.reflect._

    val placeholderMap = replacements.map(r => r.placeholder -> r).toMap

    yamlString.asNode match {
      case Right(node) =>
        nodeToExprWithReplacements(node, placeholderMap)
      case Left(error) =>
        report.errorAndAbort(s"Invalid YAML: ${error.msg}")
    }
  }

  private def liftJson(json: Json)(using Quotes): Expr[Json] =
    json.fold(
      '{ Json.Null },
      b => if (b) '{ Json.True } else '{ Json.False },
      n =>
        n.toLong match {
          case Some(l) => '{ Json.fromLong(${ Expr(l) }) }
          case None    =>
            n.toBigDecimal match {
              case Some(bd) =>
                val s = bd.toString
                '{ Json.fromBigDecimal(BigDecimal(${ Expr(s) })) }
              case None =>
                '{ Json.fromDoubleOrNull(${ Expr(n.toDouble) }) }
            }
        },
      s => '{ Json.fromString(${ Expr(s) }) },
      arr => {
        val elements = arr.map(liftJson)
        '{ Json.arr(${ Varargs(elements) }: _*) }
      },
      obj => {
        val fields = obj.toList.map { case (k, v) =>
          '{ (${ Expr(k) }, ${ liftJson(v) }) }
        }
        '{ Json.obj(${ Varargs(fields) }: _*) }
      }
    )

  private def nodeToExprWithReplacements(node: Node, placeholders: Map[String, Replacement])(using
    Quotes
  ): Expr[Json] = {
    import quotes.reflect._

    node match {
      case Node.ScalarNode(value, _) =>
        findReplacement(value, placeholders) match {
          case Some(replacement) => encoderApply(replacement.arg)
          case None              => liftJson(Parser.parse(value).getOrElse(Json.fromString(value)))
        }
      case Node.SequenceNode(nodes, _) =>
        val elements = nodes.map(n => nodeToExprWithReplacements(n, placeholders))
        '{ Json.arr(${ Varargs(elements) }: _*) }
      case Node.MappingNode(mappings, _) =>
        val fields = mappings.toList.map {
          case (Node.ScalarNode(key, _), value) =>
            val valueExpr = nodeToExprWithReplacements(value, placeholders)
            val keyExpr = findReplacement(key, placeholders) match {
              case Some(replacement) => keyEncoderApply(replacement.arg)
              case None              => Expr(key)
            }
            '{ ($keyExpr, $valueExpr) }
          case (other, _) =>
            report.errorAndAbort(s"YAML mapping keys must be scalars, got ${other.getClass.getSimpleName}")
        }
        '{ Json.obj(${ Varargs(fields) }: _*) }
      case other =>
        report.errorAndAbort(s"Unsupported YAML node type: ${other.getClass.getSimpleName}")
    }
  }

  private def findReplacement(value: String, placeholders: Map[String, Replacement]): Option[Replacement] =
    placeholders.collectFirst {
      case (ph, r) if value.contains(ph) => r
    }

  private def encoderApply(arg: Expr[Any])(using Quotes): Expr[Json] = {
    import quotes.reflect._

    arg.asTerm.tpe.widen.asType match {
      case '[t] =>
        Expr.summon[Encoder[t]] match {
          case Some(encoder) =>
            '{ $encoder.apply(${ arg.asExprOf[t] }) }
          case None =>
            report.errorAndAbort(
              s"Could not find an implicit Encoder for type ${Type.show[t]}. " +
                "Make sure an Encoder instance is in scope for interpolated variables."
            )
        }
    }
  }

  private def keyEncoderApply(arg: Expr[Any])(using Quotes): Expr[String] = {
    import quotes.reflect._

    arg.asTerm.tpe.widen.asType match {
      case '[t] =>
        Expr.summon[KeyEncoder[t]] match {
          case Some(encoder) =>
            '{ $encoder.apply(${ arg.asExprOf[t] }) }
          case None =>
            report.errorAndAbort(
              s"Could not find an implicit KeyEncoder for type ${Type.show[t]}. " +
                "Make sure a KeyEncoder instance is in scope for interpolated keys."
            )
        }
    }
  }
}
