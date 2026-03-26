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

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class YamlInterpolatorSuite extends AnyFlatSpec with Matchers {

  "yaml interpolator" should "parse a string scalar" in {
    yaml"hello" shouldBe Json.fromString("hello")
  }

  it should "parse an integer scalar" in {
    yaml"42" shouldBe Json.fromLong(42L)
  }

  it should "parse a boolean true" in {
    yaml"true" shouldBe Json.True
  }

  it should "parse a boolean false" in {
    yaml"false" shouldBe Json.False
  }

  it should "parse null" in {
    yaml"null" shouldBe Json.Null
  }

  it should "parse a floating point number" in {
    yaml"3.14" shouldBe Json.fromDoubleOrNull(3.14)
  }

  it should "parse a simple mapping" in {
    val result = yaml"""foo: bar"""
    result shouldBe Json.obj("foo" -> Json.fromString("bar"))
  }

  it should "parse a mapping with integer value" in {
    val result = yaml"""count: 42"""
    result shouldBe Json.obj("count" -> Json.fromLong(42L))
  }

  it should "parse a sequence" in {
    val result = yaml"[1, 2, 3]"
    result shouldBe Json.arr(
      Json.fromLong(1L),
      Json.fromLong(2L),
      Json.fromLong(3L)
    )
  }

  it should "parse a nested structure (flow style)" in {
    val result = yaml"{person: {name: Alice, age: 30}}"
    result shouldBe Json.obj(
      "person" -> Json.obj(
        "name" -> Json.fromString("Alice"),
        "age" -> Json.fromLong(30L)
      )
    )
  }

  it should "parse a nested structure (block style)" in {
    val result = yaml"""
person:
  name: Alice
  age: 30
"""
    result shouldBe Json.obj(
      "person" -> Json.obj(
        "name" -> Json.fromString("Alice"),
        "age" -> Json.fromLong(30L)
      )
    )
  }

  it should "parse a nested structure (indented block style)" in {
    val result = yaml"""
      person:
        name: Alice
        age: 30
      """
    result shouldBe Json.obj(
      "person" -> Json.obj(
        "name" -> Json.fromString("Alice"),
        "age" -> Json.fromLong(30L)
      )
    )
  }

  it should "parse block style with strip margin" in {
    val result = yaml"""
      |person:
      |  name: Alice
      |  age: 30
      """
    result shouldBe Json.obj(
      "person" -> Json.obj(
        "name" -> Json.fromString("Alice"),
        "age" -> Json.fromLong(30L)
      )
    )
  }

  it should "parse a mapping with a list value" in {
    val result = yaml"""
fruits:
  - apple
  - banana
"""
    result shouldBe Json.obj(
      "fruits" -> Json.arr(
        Json.fromString("apple"),
        Json.fromString("banana")
      )
    )
  }

  it should "interpolate an integer variable" in {
    val n = 42
    val result = yaml"""count: $n"""
    result shouldBe Json.obj("count" -> Json.fromInt(n))
  }

  it should "interpolate a string variable" in {
    val name = "Alice"
    val result = yaml"""name: $name"""
    result shouldBe Json.obj("name" -> Json.fromString(name))
  }

  it should "interpolate a boolean variable" in {
    val flag = true
    val result = yaml"""active: $flag"""
    result shouldBe Json.obj("active" -> Json.fromBoolean(flag))
  }

  it should "interpolate a variable as mapping key" in {
    val key = "myKey"
    val result = yaml"""$key: value"""
    result shouldBe Json.obj("myKey" -> Json.fromString("value"))
  }

  it should "interpolate multiple variables" in {
    val k = "name"
    val v = "Bob"
    val result = yaml"""$k: $v"""
    result shouldBe Json.obj("name" -> Json.fromString("Bob"))
  }

  it should "parse a complex nested structure with mappings and sequences" in {
    val result = yaml"""
      |server:
      |  host: localhost
      |  port: 8080
      |  ssl: true
      |  endpoints:
      |    - path: /api/users
      |      methods:
      |        - GET
      |        - POST
      |      headers:
      |        Content-Type: application/json
      |        Accept: application/json
      |    - path: /api/health
      |      methods:
      |        - GET
      |      headers:
      |        Accept: text/plain
      |  limits:
      |    maxConnections: 1000
      |    timeout: 30.5
      |    retries: null
      """
    result shouldBe Json.obj(
      "server" -> Json.obj(
        "host" -> Json.fromString("localhost"),
        "port" -> Json.fromLong(8080L),
        "ssl" -> Json.True,
        "endpoints" -> Json.arr(
          Json.obj(
            "path" -> Json.fromString("/api/users"),
            "methods" -> Json.arr(Json.fromString("GET"), Json.fromString("POST")),
            "headers" -> Json.obj(
              "Content-Type" -> Json.fromString("application/json"),
              "Accept" -> Json.fromString("application/json")
            )
          ),
          Json.obj(
            "path" -> Json.fromString("/api/health"),
            "methods" -> Json.arr(Json.fromString("GET")),
            "headers" -> Json.obj(
              "Accept" -> Json.fromString("text/plain")
            )
          )
        ),
        "limits" -> Json.obj(
          "maxConnections" -> Json.fromLong(1000L),
          "timeout" -> Json.fromDoubleOrNull(30.5),
          "retries" -> Json.Null
        )
      )
    )
  }

  it should "parse YAML 1.2 boolean variants" in {
    yaml"true" shouldBe Json.True
    yaml"True" shouldBe Json.True
    yaml"false" shouldBe Json.False
    yaml"False" shouldBe Json.False
  }

  it should "treat yes/no/on/off as strings (YAML 1.2)" in {
    yaml"yes" shouldBe Json.fromString("yes")
    yaml"no" shouldBe Json.fromString("no")
    yaml"on" shouldBe Json.fromString("on")
    yaml"off" shouldBe Json.fromString("off")
  }

  it should "parse tilde as null" in {
    yaml"~" shouldBe Json.Null
  }
}
