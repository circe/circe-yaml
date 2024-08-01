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

import io.circe.Json
import org.virtuslab.yaml.Node
import org.virtuslab.yaml.NodeOps

import scala.collection.immutable.ListMap

object Printer extends io.circe.yaml.common.Printer {

  override def pretty(json: Json): String = {
    val node = jsonToNode(json)
    node.asYaml
  }

  private def jsonToNode(json: Json): Node = json match {
    case Json.JArray(value) =>
      Node.SequenceNode(value.map(jsonToNode): _*)
    case Json.JObject(value) =>
      val mappings = value.toList.map { case (key, value) => (Node.ScalarNode(key): Node) -> jsonToNode(value) }
      Node.MappingNode(ListMap.from(mappings))
    case json =>
      Node.ScalarNode(json.toString)
  }
}
