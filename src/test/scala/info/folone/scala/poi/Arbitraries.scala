package info.folone.scala.poi

import org.scalacheck.{Gen, Arbitrary}
import org.scalacheck.Arbitrary._
import org.scalacheck.cats.implicits._

import cats.implicits._

object Arbitraries {
  val positiveInt = Gen.choose(0, Integer.MAX_VALUE).map(Pos.unsafe)

  implicit val arbCell: Arbitrary[Cell] =
    Arbitrary(
      Gen.oneOf(
        arbitrary[String].map(StringCell(_)),
        arbitrary[Double].map(NumericCell(_)),
        arbitrary[Boolean].map(BooleanCell(_)),
        arbitrary[java.util.Date].map(DateCell(_))
        //,arbitrary[String].map(FormulaCell(_))
      )
    )

  implicit val arbRow: Arbitrary[Row] =
    Arbitrary((positiveInt, arbitrary[Cell]).mapN { case (index, cell) => Row(Map(index -> cell)) })

  implicit val arbSheet: Arbitrary[Sheet] =
    Arbitrary((positiveInt, arbitrary[Row]).mapN {
      case (index, value) =>
        Sheet(Map(index -> value))
    })

  implicit val arbWorkbook: Arbitrary[Workbook] =
    Arbitrary((arbitrary[String], arbitrary[Sheet]).mapN { case (name, value) => Workbook(Map(name -> value)) })
}
