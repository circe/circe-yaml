package io.circe.yaml.parser

import java.io.{BufferedReader, File, InputStreamReader}
import org.scalatest.{FlatSpec, FreeSpec}
import io.circe.yaml.printer._

class ParserTests extends FreeSpec {

  "yaml test files" - {

    val testFiles = new File(getClass.getClassLoader.getResource("test-yamls").getPath)
      .listFiles.filter(_.getName endsWith ".yml").map {
        file => file.getName -> file.getName.replaceFirst("yml$", "json")
      }

    testFiles foreach {
      case (yamlFile, jsonFile) => yamlFile in {
        val jsonStream = getClass.getClassLoader.getResourceAsStream(s"test-yamls/$jsonFile")
        val json = new BufferedReader(new InputStreamReader(jsonStream)).lines.toArray.mkString("\n")
        val parsedJson = io.circe.parser.parse(json)
        val parsedYaml = Parser.parse(
          new InputStreamReader(getClass.getClassLoader.getResourceAsStream(s"test-yamls/$yamlFile"))
        )
        assert(parsedJson == parsedYaml)
        val printedYaml = parsedYaml.map(_.asYaml)
        assert(printedYaml.isRight)
      }
    }
  }
}
