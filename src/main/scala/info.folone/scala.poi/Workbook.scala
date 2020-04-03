package info.folone.scala.poi

import org.apache.poi.ss.usermodel.{
  DateUtil,
  WorkbookFactory,
  Cell      => POICell,
  CellStyle => POICellStyle,
  Row       => POIRow,
  Workbook  => POIWorkbook,
  Font      => POIFont
}

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import cats.effect.{IO, Resource}
import cats.instances.map._
import cats.instances.list._
import cats.instances.option._
import cats.syntax.semigroup._
import cats.syntax.flatMap._

final case class Workbook(sheetMap: Map[String, Sheet], format: WorkbookVersion = HSSF) {

  private def setPoiCell(defaultRowHeight: Short, row: POIRow, cell: Cell, poiCell: POICell): Unit = {
    cell match {
      case StringCell(data, _) =>
        poiCell.setCellValue(data)
        val cellHeight = data.split("\n").length * defaultRowHeight
        if (cellHeight > row.getHeight) {
          row.setHeight(cellHeight.asInstanceOf[Short])
        }
      case BooleanCell(data, _) => poiCell.setCellValue(data)
      case DateCell(data, _)    => poiCell.setCellValue(data)
      case NumericCell(data, _) => poiCell.setCellValue(data)
      case FormulaCell(data, _) => poiCell.setCellFormula(data.value) //TODO remove "="
    }
  }

  private lazy val book = {
    val workbook = format match {
      case HSSF  => new org.apache.poi.hssf.usermodel.HSSFWorkbook
      case XSSF  => new org.apache.poi.xssf.usermodel.XSSFWorkbook
      case SXSSF => new org.apache.poi.xssf.streaming.SXSSFWorkbook(-1)
    }
    sheetMap.foreach {
      case (name, Sheet(rowMap)) =>
        val sheet = workbook.createSheet(name)
        rowMap.foreach {
          case (index, Row(cellMap)) =>
            val row = sheet.createRow(index.value)
            cellMap.foreach {
              case (index, cl) =>
                val poiCell = row.createCell(index.value)
                setPoiCell(sheet.getDefaultRowHeight, row, cl, poiCell)
            }
        }
    }
    workbook
  }

  private def applyStyling(wb: POIWorkbook, styles: Map[CellStyle, List[CellAddr]]) = {
    def fontAppliedTo(font: Font)(pf: POIFont): POIFont = {
      pf.setFontName(font.name)
      pf.setBoldweight(if (font.bold) POIFont.BOLDWEIGHT_BOLD else POIFont.BOLDWEIGHT_NORMAL)
      pf.setColor(font.color match {
        case FontColor.Normal => POIFont.COLOR_NORMAL
        case FontColor.Red    => POIFont.COLOR_RED
      })
      pf
    }

    def pStyle(cs: CellStyle): POICellStyle = {
      val pStyle = wb.createCellStyle()
      pStyle.setFont(fontAppliedTo(cs.font)(wb.createFont))
      pStyle.setDataFormat(wb.createDataFormat.getFormat(cs.dataFormat.format))
      pStyle
    }

    styles.keys.foreach { s =>
      val cellAddresses = styles(s)
      val cellStyle     = pStyle(s)
      cellAddresses.foreach { addr =>
        val cell = wb.getSheet(addr.sheet).getRow(addr.row.value).getCell(addr.col.value)
        cell setCellStyle cellStyle
      }
    }
    wb
  }

  def styled(styles: Map[CellStyle, List[CellAddr]]): Workbook = {
    applyStyling(book, styles)
    this
  }

  def styled: Workbook = {
    val styles: Map[CellStyle, List[CellAddr]] = sheetMap.foldRight(Map.empty[CellStyle, List[CellAddr]]) {
      case ((name, sheet), map) => map |+| sheetStyles(name)(sheet)
    }
    styled(styles)
  }

  private def sheetStyles(name: String)(sheet: Sheet): Map[CellStyle, List[CellAddr]] =
    sheet.rowMap.foldRight(Map[CellStyle, List[CellAddr]]()) {
      case ((index, row), map) => map |+| rowStyles(name, index)(row)
    }

  private def rowStyles(sheet: String, row: Pos)(rowInst: Row): Map[CellStyle, List[CellAddr]] =
    rowInst.cellMap.foldRight(Map[CellStyle, List[CellAddr]]()) {
      case ((col, cell), map) => map |+| cellStyles(sheet, row, col)(cell)
    }

  private def cellStyles(sheet: String, row: Pos, col: Pos)(cell: Cell): Map[CellStyle, List[CellAddr]] =
    cell.style match {
      case None    => Map.empty[CellStyle, List[CellAddr]]
      case Some(s) => Map(s -> List(CellAddr(sheet, row, col)))
    }

  /**
    * Fits column's width to maximum width of non-empty cell at cell address.
    * Quite expensive. Use as late as possible.
    *
    * @param addrs addresses of cells, which columns size should fit cells content
    */
  def autosizeColumns(addrs: Set[CellAddr]): Workbook = {
    addrs foreach { a =>
      book.getSheet(a.sheet).autoSizeColumn(a.col.value)
    }
    this
  }

  def safeToFile(path: String): IO[Unit] =
    Resource
      .fromAutoCloseable(IO(new FileOutputStream(new File(path))))
      .use(out => IO(book.write(out)))

  def safeToStream(stream: OutputStream): IO[Unit] =
    IO(book.write(stream))

  def asPoi: POIWorkbook = book

}

object Workbook {

  def fromPath(path: String): IO[Workbook] =
    (IO(new File(path)) >>= fromFile).map(readWorkbook)

  def fromInputStream(is: InputStream): IO[Workbook] =
    fromInputStreamInternal(is).map(readWorkbook)

  private def fromFile(f: File) = IO(WorkbookFactory.create(f))

  private def fromInputStreamInternal(is: InputStream) = IO(WorkbookFactory.create(is))

  private val poiCellToCell: PartialFunction[POICell, Cell] = {
    case cell if cell.getCellType == POICell.CELL_TYPE_NUMERIC && DateUtil.isCellDateFormatted(cell) =>
      DateCell(cell.getDateCellValue)
    case cell if cell.getCellType == POICell.CELL_TYPE_NUMERIC =>
      NumericCell(cell.getNumericCellValue)
    case cell if cell.getCellType == POICell.CELL_TYPE_BOOLEAN =>
      BooleanCell(cell.getBooleanCellValue)
    case cell if cell.getCellType == POICell.CELL_TYPE_FORMULA =>
      FormulaCell(Formula.unsafe(cell.getCellFormula))
    case cell if cell.getCellType == POICell.CELL_TYPE_STRING =>
      StringCell(cell.getStringCellValue)
  }

  private def readWorkbook(wb: POIWorkbook) =
    Workbook(
      ((0 until wb.getNumberOfSheets).toList >>= { i: Int =>
        Option(wb.getSheetAt(i)).toList.map { poiSheet =>
          poiSheet.getSheetName -> Sheet(
            ((0 to poiSheet.getLastRowNum).toList >>= { k =>
              Option(poiSheet.getRow(k)).toList.map { poiRow =>
                Pos.unsafe(k) -> Row(
                  ((0 until poiRow.getLastCellNum).toList >>= { j =>
                    (Option(poiRow.getCell(j)) >>= poiCellToCell.lift).toList.map {
                      Pos.unsafe(j) -> _
                    }
                  }).toMap
                )
              }
            }).toMap
          )
        }
      }).toMap
    )
}
