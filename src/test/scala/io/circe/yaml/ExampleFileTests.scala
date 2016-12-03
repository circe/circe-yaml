package io.circe.yaml

import java.io.{BufferedReader, File, InputStreamReader}

import org.scalatest.FreeSpec

class ExampleFileTests extends FreeSpec {

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
        def yamlReader = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(s"test-yamls/$yamlFile"))
        val yaml = new BufferedReader(yamlReader).lines.toArray.mkString("\n")
        val parsedYamlString = parser.parse(yaml)
        val parsedStreamString = parser.parseDocuments(yaml)
        val parsedYamlReader = parser.parse(yamlReader)
        val parsedStreamReader = parser.parseDocuments(yamlReader)
        assert(parsedJson == parsedYamlString)
        assert(parsedJson == parsedStreamString.head)
        assert(parsedJson == parsedYamlReader)
        assert(parsedJson == parsedStreamReader.head)
      }
    }
  }
}
