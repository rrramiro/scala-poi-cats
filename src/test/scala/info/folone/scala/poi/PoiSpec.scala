package info.folone.scala.poi

import cats._, syntax.monoid._

import org.apache.poi.hssf.usermodel.HSSFSheet
import org.specs2.specification.Scope
import org.specs2.ScalaCheck
import org.specs2.mutable._
import org.specs2.matcher._
import org.scalacheck._
import Arbitrary._

import eu.timepit.refined._
import eu.timepit.refined.auto._

class PoiSpec extends Specification with ScalaCheck with TemporaryFolder {
  "Poi" should {
    "create workbook" in {
      val wb = Workbook {
        Map(
          "name" -> Sheet({
            Map(
              (1: Pos) -> Row(
                Map((1: Pos) -> NumericCell(-13.0 / 5), (2: Pos) -> FormulaCell("ABS(B2)"))
              ),
              (2: Pos) -> Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data2")))
            )
          }),
          "name2" -> Sheet({
            Map((2: Pos) -> Row({
              Map((1: Pos) -> BooleanCell(true), (2: Pos) -> NumericCell(2.4))
            }))
          })
        )
      }
      val path = tempDirPath + "/book.xls"
      val io   = wb.safeToFile(path)
      io.unsafeRunSync()
      val actualWb = Workbook.fromPath(path).unsafeRunSync()
      actualWb === wb
    }
  }

  "Workbook" should {
    "have sheets in it" in new WorkbookFixture {
      book.asPoi.getSheet("test") must beAnInstanceOf[HSSFSheet]
    }

    val wb1 = Workbook {
      Map(
        "name" -> Sheet({
          Map(
            (1: Pos) -> Row({
              Map((1: Pos) -> NumericCell(-13.0 / 5), (2: Pos) -> FormulaCell("ABS(B2)"))
            }),
            (2: Pos) -> Row({
              Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data2"))
            })
          )
        }),
        "name" -> Sheet({
          Map((2: Pos) -> Row({
            Map((1: Pos) -> BooleanCell(true), (2: Pos) -> NumericCell(2.4))
          }))
        })
      )
    }
    val wb2 = Workbook {
      Map(
        "name" -> Sheet({
          Map(
            (1: Pos) -> Row(
              Map((1: Pos) -> NumericCell(-13.0 / 5), (2: Pos) -> FormulaCell("ABS(B2)"))
            ),
            (2: Pos) -> Row(
              Map((2: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data2"))
            )
          )
        }),
        "name22" -> Sheet({
          Map(
            (2: Pos) -> Row(
              Map((1: Pos) -> BooleanCell(true), (2: Pos) -> NumericCell(2.4))
            )
          )
        })
      )
    }
    val wb3 = Workbook {
      Map(
        "name3" -> Sheet(
          Map(
            (1: Pos) -> Row(
              Map((1: Pos) -> NumericCell(-13.0 / 5), (2: Pos) -> FormulaCell("ABS(B2)"))
            ),
            (2: Pos) -> Row(Map((1: Pos) -> StringCell("data"), (2: Pos) -> StringCell("data2")))
          )
        ),
        "name32" -> Sheet({
          Map(
            (2: Pos) -> Row(
              Map((1: Pos) -> BooleanCell(true), (2: Pos) -> NumericCell(2.4))
            )
          )
        })
      )
    }

    "be associative" in {
      ((wb1 |+| wb2) |+| wb3) must_== (wb1 |+| (wb2 |+| wb3))
    }

    "satisfy right identity" in {
      (wb1 |+| wbInstance.empty) must_== wb1
    }

    "satisfy left identity" in {
      (wbInstance.empty |+| wb1) must_== wb1
    }
  }

  "Sheet" can {
    "have filled cells" in new WorkbookFixture {
      val cellText = book.asPoi.getSheet("test").getRow(0).getCell(0).getStringCellValue
      cellText must beEqualTo("theCell")
    }
  }

  trait WorkbookFixture extends Scope {
    val book = Workbook(Map("test" -> Sheet(Map((0: Pos) -> Row(Map((0: Pos) -> StringCell("theCell")))))))
  }
}
