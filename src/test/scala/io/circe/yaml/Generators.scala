package io.circe.yaml

import java.text.SimpleDateFormat
import org.scalacheck.{Arbitrary, Gen}, Arbitrary.arbitrary
import org.yaml.snakeyaml.error.Mark
import org.yaml.snakeyaml.nodes._
import scala.collection.JavaConverters._

object Generators {

  private def scalarNode(tag: Tag, value: String, style: Option[Character] = None) = new ScalarNode(
    tag,
    value,
    new Mark("", 0, 0, 0, value, 0),
    new Mark("", 0, 0, value.length, value, 0),
    style.orNull
  )

  val intNode: Gen[ScalarNode] = for {
    i <- arbitrary[Int]
  } yield scalarNode(Tag.INT, i.toString)

  val floatNode: Gen[ScalarNode] = for {
    d <- arbitrary[Double]
  } yield scalarNode(Tag.FLOAT, d.toString)

  val boolNode: Gen[ScalarNode] = for {
    b <- Gen.oneOf("y","Y","yes","Yes","YES","n","N","no","No","NO",
                   "true","True","TRUE","false","False","FALSE",
                   "on","On","ON","off","Off","OFF")
  } yield scalarNode(Tag.BOOL, b)

  val nullNode: ScalarNode = scalarNode(Tag.NULL, "null")

  val timestampNode: Gen[ScalarNode] = for {
    t      <- Gen.calendar
    fmtStr <- Gen.oneOf(
      "yyyy-MM-dd hh:mm:ss.SX",
      "yyyy-MM-dd't'hh:mm:ss.SX",
      "yyyy-MM-dd'T'hh:mm:ss.SX"
    )
  } yield scalarNode(Tag.TIMESTAMP, new SimpleDateFormat(fmtStr).format(t.getTime))

  val strNode: Gen[ScalarNode] = for {
    str <- arbitrary[String]
  } yield scalarNode(Tag.STR, str)

  val otherScalar: Gen[ScalarNode] = for {
    tag <- Gen.identifier
    value <- arbitrary[String]
  } yield scalarNode(new Tag(tag), value)

  val anyScalar: Gen[ScalarNode] = Gen.oneOf(intNode, floatNode, boolNode, timestampNode, strNode, Gen.const(nullNode))

  lazy val anyNode: Gen[Node] = Gen.sized { size =>
    if (size > 1) {
      Gen.frequency(
        5 -> anyScalar,
        1 -> Gen.resize(size >> 1, Gen.lzy(mappingNode)),
        1 -> Gen.resize(size >> 1, Gen.lzy(seqNode))
      )
    } else otherScalar
  }

  val validTuple: Gen[NodeTuple] = for {
    key   <- strNode
    value <- anyNode
  } yield new NodeTuple(key, value)

  val validScalarTuple: Gen[NodeTuple] = for {
    key   <- strNode
    value <- anyScalar
  } yield new NodeTuple(key, value)

  val mappingNode: Gen[MappingNode] = for {
    nodes  <- Gen.nonEmptyListOf(validTuple)
    tag    <- Gen.oneOf(Tag.MAP, Tag.OMAP)
    flow   <- arbitrary[Boolean]
    mapping  = if (tag == Tag.OMAP) nodes.map(n => n.getKeyNode -> n).toMap.toList.map(_._2) else nodes
  } yield new MappingNode(tag, mapping.asJava, flow)

  val flatMappingNode: Gen[MappingNode] = for {
    nodes  <- Gen.nonEmptyListOf(validScalarTuple)
    tag    <- Gen.oneOf(Tag.MAP, Tag.OMAP)
    flow   <- arbitrary[Boolean]
    mapping  = if (tag == Tag.OMAP) nodes.map(n => n.getKeyNode -> n).toMap.toList.map(_._2) else nodes
  } yield new MappingNode(tag, mapping.asJava, flow)

  val seqNode: Gen[SequenceNode] = for {
    nodes <- Gen.nonEmptyListOf(anyNode)
    tag   <- Gen.oneOf(Tag.SEQ, Tag.SET)
    flow  <- arbitrary[Boolean]
    seq    = if (tag == Tag.SET) nodes.toSet.toList else nodes
  } yield new SequenceNode(tag, seq.asJava, flow)

  val flatSeqNode: Gen[SequenceNode] = for {
    nodes <- Gen.nonEmptyListOf(anyScalar)
    tag   <- Gen.oneOf(Tag.SEQ, Tag.SET)
    flow  <- arbitrary[Boolean]
    seq    = if (tag == Tag.SET) nodes.toSet.toList else nodes
  } yield new SequenceNode(tag, (seq: List[Node]).asJava, flow)

}
