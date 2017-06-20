package io.circe.yaml

import io.circe.yaml.parser._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.yaml.snakeyaml.nodes.ScalarNode
import scala.collection.JavaConverters._

class NodeAlgSpec extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks with MockFactory {

  import Generators._

  "mocked" - {
    val mockAlg = mock[NodeAlg[String]]
    val subject = new NodeAlg[String] {
      def int(node: ScalarNode): String = mockAlg.int(node)
      def float(node: ScalarNode): String = mockAlg.float(node)
      def timestamp(node: ScalarNode): String = mockAlg.timestamp(node)
      def bool(node: ScalarNode): String = mockAlg.bool(node)
      def yNull(node: ScalarNode): String = mockAlg.yNull(node)
      def string(node: ScalarNode): String = mockAlg.string(node)
      def otherScalar(node: ScalarNode): String = mockAlg.otherScalar(node)
      def fromValues(ts: Iterable[String]): String = mockAlg.fromValues(ts)
      def fromFields(ts: Iterable[(String, String)]): String = mockAlg.fromFields(ts)
    }

    "plain" - {
      "ints" in forAll(intNode) { node =>
        mockAlg.int _ expects node returning ""
        subject.any(node)
      }
      "floats" in forAll(floatNode) { node =>
        mockAlg.float _ expects node returning ""
        subject.any(node)
      }
      "timestamps" in forAll(timestampNode) { node =>
        mockAlg.timestamp _ expects node returning ""
        subject.any(node)
      }
      "booleans" in forAll(boolNode) { node =>
        mockAlg.bool _ expects node returning ""
        subject.any(node)
      }
      "nulls" in {
        mockAlg.yNull _ expects nullNode returning ""
        subject.any(nullNode)
      }
      "strings" in forAll(strNode) { node =>
        mockAlg.string _ expects node returning ""
        subject.any(node)
      }
      "other scalars" in forAll(otherScalar) { node =>
        mockAlg.otherScalar _ expects node returning ""
        subject.any(node)
      }
      "sequences" in forAll(flatSeqNode) { node =>
        inAnyOrder {
          mockAlg.int _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.float _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.timestamp _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.bool _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.yNull _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.string _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.otherScalar _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
        }

        mockAlg.fromValues _ expects where {
          i: Iterable[String] => i.toList == node.getValue.asScala.toList.map(subject.any)
        } returning ""

        subject.any(node)
      }

      "maps" in forAll(flatMappingNode) { node =>
        inAnyOrder {
          mockAlg.int _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.float _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.timestamp _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.bool _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.yNull _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.string _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
          mockAlg.otherScalar _ expects * onCall { node: ScalarNode => node.getValue } anyNumberOfTimes()
        }

        mockAlg.fromFields _ expects where {
          i: Iterable[(String, String)] => i.toList == node.getValue.asScala.toList.map {
            nodeTuple => nodeTuple.getKeyNode.asInstanceOf[ScalarNode].getValue -> subject.any(nodeTuple.getValueNode)
          }
        } returning ""

        subject.any(node)
      }
    }
  }

}
