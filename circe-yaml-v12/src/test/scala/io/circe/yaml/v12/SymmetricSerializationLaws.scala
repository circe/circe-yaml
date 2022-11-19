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

import cats.Eq
import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.ParsingFailure
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Shrink
import org.typelevel.discipline.Laws

trait SymmetricSerializationLaws {

  def printerRoundTrip[A: Eq: Encoder: Decoder](
    parse: String => Either[ParsingFailure, Json],
    print: Json => String,
    a: A
  ): IsEq[Either[io.circe.Error, A]] =
    parse(print(Encoder[A].apply(a))).right.flatMap(_.as[A]) <-> Right(a)

}

object SymmetricSerializationLaws {

  def apply(): SymmetricSerializationLaws = new SymmetricSerializationLaws {}
}

trait SymmetricSerializationTests extends Laws {
  def laws: SymmetricSerializationLaws

  def symmetricPrinter[A: Eq: Arbitrary: Shrink: Encoder: Decoder](
    print: Json => String,
    parse: String => Either[ParsingFailure, Json]
  ): RuleSet =
    new DefaultRuleSet(
      name = "printer",
      parent = None,
      "roundTrip" -> Prop.forAll { (a: A) =>
        laws.printerRoundTrip(parse, print, a)
      }
    )
}

object SymmetricSerializationTests {
  def apply[A: Eq: Arbitrary: Decoder: Encoder](
    print: Json => String,
    parse: String => Either[ParsingFailure, Json]
  ): SymmetricSerializationTests =
    new SymmetricSerializationTests {
      val laws: SymmetricSerializationLaws = SymmetricSerializationLaws()
      symmetricPrinter[A](print, parse)
    }
}
