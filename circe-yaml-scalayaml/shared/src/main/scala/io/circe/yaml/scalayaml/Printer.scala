package io.circe.yaml.scalayaml
import io.circe.Json
import org.virtuslab.yaml.Node

object Printer extends io.circe.yaml.common.Printer {

  override def pretty(json: Json): String = {
    val node = jsonToNode(json)
    node.asYaml
  }

  private def jsonToNode(json: Json): Node = json match {
    case Json.JNull          => Node.ScalarNode("null")
    case json: Json.JBoolean => Node.ScalarNode(json.toString)
    case json: Json.JNumber  => Node.ScalarNode(json.toString)
    case json: Json.JString  => Node.ScalarNode(json.toString)
    case Json.JArray(value)  => Node.SequenceNode(value.map(jsonToNode): _*)
    case Json.JObject(value) =>
      Node.MappingNode(value.toMap.map { case (key, value) => (Node.ScalarNode(key): Node) -> jsonToNode(value) })
  }
}
