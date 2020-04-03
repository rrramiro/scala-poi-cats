package info.folone.scala.poi

import java.io.File

import org.specs2._
import eu.timepit.refined._
import eu.timepit.refined.auto._

class PoiLoadFileSpec extends Specification with TemporaryFolder {
  def is =
    "LoadWorkbook" ^
      """
|   | A  | B  | C  |
|:-:|:--:|:--:|:--:|
| 1 | A1 | B1 | C1 |
| 2 | A2 | B2 | C2 |
| 3 | A3 | B3 | C3 |
| 4 | A4 | B4 | C4 |
""" ^
      "is load" ^ loadWorkbookTest ^ end

  def loadWorkbookTest = s2"""
    rows have size 4        $e1
    cell have size 12       $e2
    and contains [A1...C4]  $e3
  """

  val testBook = Workbook(
    Map(
      "test" -> Sheet(
        Map(
          (0: Pos) -> Row(
            Map(
              (0: Pos) -> StringCell("A1"),
              (1: Pos) -> StringCell("B1"),
              (2: Pos) -> StringCell("C1")
            )
          ),
          (1: Pos) -> Row(
            Map(
              (0: Pos) -> StringCell("A2"),
              (1: Pos) -> StringCell("B2"),
              (2: Pos) -> StringCell("C2")
            )
          ),
          (2: Pos) -> Row(
            Map(
              (0: Pos) -> StringCell("A3"),
              (1: Pos) -> StringCell("B3"),
              (2: Pos) -> StringCell("C3")
            )
          ),
          (3: Pos) -> Row(
            Map(
              (0: Pos) -> StringCell("A4"),
              (1: Pos) -> StringCell("B4"),
              (2: Pos) -> StringCell("C4")
            )
          )
        )
      )
    ),
    XSSF
  )

  val testBookPath = tempDirPath + "/testBook.xlsx"

  val targetWorksheet = {
    new File(testBookPath).delete()
    testBook.safeToFile(testBookPath).unsafeRunSync()
    val wb = Workbook.fromPath(testBookPath).unsafeRunSync()
    wb.sheetMap.values.head
  }

  def e1 = targetWorksheet.rowMap must have size (4)
  def e2 = targetWorksheet.rowMap.values.toList.map { _.cellMap.size }.sum === 12
  def e3 = {
    val expect = Set("A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4", "C1", "C2", "C3", "C4")
    val actual = {
      for {
        (_, row)                 <- targetWorksheet.rowMap
        (_, StringCell(cell, _)) <- row.cellMap
      } yield cell
    }.toSet

    actual === expect
  }
}
