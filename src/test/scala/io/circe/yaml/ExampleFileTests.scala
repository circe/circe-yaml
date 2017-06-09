package io.circe.yaml

import java.io.{File, InputStreamReader}

import org.scalatest.FreeSpec
import scala.io.Source

class ExampleFileTests extends FreeSpec {

  "yaml test files" - {

    val testFiles = new File(getClass.getClassLoader.getResource("test-yamls").getPath)
      .listFiles.filter(_.getName endsWith ".yml").map {
        file => file.getName -> file.getName.replaceFirst("yml$", "json")
      }

    val parser = Parser(useTimestampLit = false)

    testFiles foreach {
      case (yamlFile, jsonFile) => yamlFile in {
        val jsonStream = getClass.getClassLoader.getResourceAsStream(s"test-yamls/$jsonFile")
        val json = Source.fromInputStream(jsonStream).mkString
        jsonStream.close()
        val parsedJson = io.circe.parser.parse(json)
        def yamlStream = getClass.getClassLoader.getResourceAsStream(s"test-yamls/$yamlFile")
        def yamlReader = new InputStreamReader(yamlStream)
        val yaml = Source.fromInputStream(yamlStream).mkString
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
