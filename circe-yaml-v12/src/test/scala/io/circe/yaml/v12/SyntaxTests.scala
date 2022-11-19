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

package io.circe.yaml.v12

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import syntax._

class SyntaxTests extends AnyFlatSpec with Matchers {

  val json = Json.obj(
    "foo" -> Json.obj(
      "bar" -> Json.fromString("baz")
    )
  )

  "spaces2" should "have double space indent" in {
    json.asYaml.spaces2 shouldEqual
      """foo:
        |  bar: baz
        |""".stripMargin
  }

  "spaces4" should "have quadruple space indent" in {
    json.asYaml.spaces4 shouldEqual
      """foo:
        |    bar: baz
        |""".stripMargin
  }

}
