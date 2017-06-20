package io.circe.yaml

import io.circe.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import org.scalacheck.Gen
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class ConfiguredParserTests extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  "ConfiguredParser" should "parse timestamps as longs" in forAll(Gen.calendar) { cal =>
      val dateStr = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SX").format(cal.getTime)
      whenever(cal.get(Calendar.YEAR) <= 9999 && cal.get(Calendar.YEAR) >= -9999 ) {
        parser.configured(numericTimestamps = true).parse(
              s"""
               |timestamp: !!timestamp $dateStr
             """.stripMargin
          ) shouldEqual Right(Json.obj("timestamp" -> Json.fromLong(cal.getTimeInMillis)))
      }
    }

}
