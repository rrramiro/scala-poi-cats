package info.folone.scala.poi
import monocle.state.all._
import monocle.Lens
import cats.implicits._
import monocle.Getter
import monocle.macros.GenLens
import org.specs2.mutable._

import eu.timepit.refined._
import eu.timepit.refined.auto._

class PoiLensesSpec extends Specification {
  "Lenses on poi classes" should {
    "cellLens getter" >> {
      stringCellLens.get(StringCell("data")) must beEqualTo("data")
    }

    "cellLens setter" >> {
      stringCellLens.set("newData")(StringCell("data")) must beEqualTo(StringCell("newData"))
    }

    "rowLens contains" >> {
      val row: Row = Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data1")))
      rowLens.contains(row)((2: Pos) -> StringCell("data1")) must beTrue
    }

    "rowLens does not contain" >> {
      val row: Row = Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data")))
      rowLens.contains(row)((2: Pos) -> StringCell("data1")) must beFalse
    }

    "rowLens +=" >> {
      val row: Row = Row(Map((1: Pos) -> StringCell("data"), (3: Pos) -> StringCell("data3")))

      (rowLens += ((2: Pos) -> StringCell("data1"))).run(row).value mustEqual (Row(
        Map(
          (1: Pos) -> StringCell("data"),
          (3: Pos) -> StringCell("data3"),
          (2: Pos) -> StringCell("data1")
        )
      ), ())
    }

    "rowLens &=" >> {
      (rowLens &= ((2: Pos) -> StringCell("data1")))
        .run(Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data1"))))
        .value mustEqual (Row(Map((2: Pos) -> StringCell("data1"))), ())
    }

    "rowLens &~=" >> {
      (rowLens &~= ((2: Pos) -> StringCell("data1")))
        .run(Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data1"))))
        .value mustEqual
        (Row(Map((1: Pos) -> StringCell("data"))), ())
    }

    "rowLens |=" >> {
      (rowLens |= ((2: Pos) -> StringCell("data1")))
        .run(Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data1"))))
        .value mustEqual
        (Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data1"))), ())

      (rowLens |= ((3: Pos) -> StringCell("data1")))
        .run(Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data2"))))
        .value mustEqual
        (Row(
          Map(
            (1: Pos) -> StringCell("data"),
            (2: Pos) -> StringCell("data2"),
            (3: Pos) -> StringCell("data1")
          )
        ), ())
    }

    "rowLens -=" >> {
      (rowLens -= StringCell("data1"))
        .run(Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data1"))))
        .value mustEqual (Row(Map((1: Pos) -> StringCell("data"))), ())
    }

  }
}
