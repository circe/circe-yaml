package io.circe.yaml

import cats.Eq
import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Decoder, Encoder, Json, ParsingFailure }
import org.scalacheck.{ Arbitrary, Prop, Shrink }
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
