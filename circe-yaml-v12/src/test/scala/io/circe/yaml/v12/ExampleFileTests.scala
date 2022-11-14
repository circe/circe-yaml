package io.circe.yaml.v12

import java.io.{ File, InputStreamReader }
import org.scalatest.freespec.AnyFreeSpec
import scala.io.Source

class ExampleFileTests extends AnyFreeSpec {

  "yaml test files" - {

    val testFiles = new File(getClass.getClassLoader.getResource("test-yamls").getPath).listFiles
      .filter(_.getName.endsWith(".yml"))
      .map { file =>
        file.getName -> file.getName.replaceFirst("yml$", "json")
      }

    testFiles.foreach { case (yamlFile, jsonFile) =>
      yamlFile in {
        val jsonStream = getClass.getClassLoader.getResourceAsStream(s"test-yamls/$jsonFile")
        val json = Source.fromInputStream(jsonStream).mkString
        jsonStream.close()
        val parsedJson = io.circe.jawn.parse(json)
        def yamlStream = getClass.getClassLoader.getResourceAsStream(s"test-yamls/$yamlFile")
        def yamlReader = new InputStreamReader(yamlStream)
        val yaml = Source.fromInputStream(yamlStream).mkString
        val parsedYamlString = parser.parse(yaml)
        val parsedStreamString = parser.parseDocuments(yaml)
//        val parsedYamlReader = parser.parse(yamlReader)
        val parsedStreamReader = parser.parseDocuments(yamlReader)
        assert(parsedJson == parsedYamlString)
        assert(parsedJson == parsedStreamString.head)
//        assert(parsedJson == parsedYamlReader)
        assert(parsedJson == parsedStreamReader.head)
      }
    }
  }
}
