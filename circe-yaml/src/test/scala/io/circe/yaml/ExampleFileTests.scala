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

import org.scalatest.freespec.AnyFreeSpec

import java.io.File
import java.io.InputStreamReader
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
