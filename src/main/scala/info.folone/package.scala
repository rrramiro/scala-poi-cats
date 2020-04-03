package info.folone.scala

import poi._
import cats._
import cats.syntax.either._
import cats.instances.string._
import cats.data.EitherT
import cats.effect.IO
import eu.timepit.refined.refineV
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.string.Trimmed
import eu.timepit.refined.api.Refined

package object poi extends Instances with Lenses

final case class RefinedException(msg: String) extends Throwable

trait Instances {

  type Pos = Refined[Int, NonNegative]

  implicit val ordering = new Ordering[Pos] {
    def compare(x: Pos, y: Pos): Int = x.value - y.value
  }

  object Pos {
    def apply(value: Int): IO[Pos] =
      IO.fromEither(refineV[NonNegative](value).leftMap(RefinedException.apply))
    def unsafe(value: Int): Pos = Refined.unsafeApply(value)
  }

  type Formula = Refined[String, Trimmed]

  object Formula {
    def apply(value: String): IO[Formula] = //TODO data.value.dropWhile(_ == '=')
      IO.fromEither(refineV[Trimmed](value).leftMap(RefinedException.apply))
    def unsafe(value: String): Formula = Refined.unsafeApply(value)
  }

  // Typeclass instances

  object equalities {
    implicit val formulaCellEqualInstance: Eq[FormulaCell] = new Eq[FormulaCell] {
      override def eqv(f1: FormulaCell, f2: FormulaCell) = f1.data == f2.data
    }

    implicit val cellEqualInstance: Eq[Cell] = new Eq[Cell] {
      override def eqv(s1: Cell, s2: Cell) = (s1, s2) match {
        case (s1: StringCell, s2: StringCell)   => s1 == s2
        case (n1: NumericCell, n2: NumericCell) => n1 == n2
        case (d1: DateCell, d2: DateCell)       => d1 == d2
        case (b1: BooleanCell, b2: BooleanCell) => b1 == b2
        case (f1: FormulaCell, f2: FormulaCell) => Eq[FormulaCell].eqv(f1, f2)
        case (_, _)                             => false
      }
    }
  }

  implicit val cellInstance: Semigroup[Cell] with Eq[Cell] with Show[Cell] =
    new Semigroup[Cell] with Eq[Cell] with Show[Cell] {
      override def combine(f1: Cell, f2: Cell): Cell = f2
      override def eqv(a1: Cell, a2: Cell): Boolean  = a1 == a2 //TODO verify
      private def showStyled(style: Option[CellStyle])(cellStr: String) = style.fold(cellStr)(
        _ => s"StyledCell($cellStr, <style>)"
      )
      override def show(as: Cell): String =
        as match {
          case StringCell(data, style)  => showStyled(style)(s"""StringCell("$data")""")
          case NumericCell(data, style) => showStyled(style)(s"""NumericCell($data)""")
          case DateCell(data, style)    => showStyled(style)(s"""DateCell($data)""")
          case BooleanCell(data, style) => showStyled(style)(s"""BooleanCell($data)""")
          case FormulaCell(data, style) => showStyled(style)(s"""FormulaCell("=$data")""")
        }
    }
  implicit val rowInstance: Semigroup[Row] with Eq[Row] with Show[Row] =
    new Semigroup[Row] with Eq[Row] with Show[Row] {
      override def combine(f1: Row, f2: Row): Row =
        Row(combineMap(f1.cellMap, f2.cellMap))
      override def eqv(a1: Row, a2: Row): Boolean =
        a1.cellMap.toStream.corresponds(a2.cellMap.toStream) { //TODO check corresponds
          case ((indexL, cellL), (indexR, cellR)) => indexL == indexR && Eq[Cell].eqv(cellL, cellR)
        }
      override def show(as: Row): String = "Row(" + as.cellMap.toIndexedSeq.sortBy(_._1) + ")"
    }
  implicit val sheetInstance: Semigroup[Sheet] with Eq[Sheet] with Show[Sheet] =
    new Semigroup[Sheet] with Eq[Sheet] with Show[Sheet] {
      override def combine(f1: Sheet, f2: Sheet): Sheet =
        Sheet(combineMap(f1.rowMap, f2.rowMap))
      override def eqv(a1: Sheet, a2: Sheet): Boolean =
        (a1.rowMap.toIndexedSeq.sortBy(_._1) zip
          a2.rowMap.toIndexedSeq.sortBy(_._1))
          .foldLeft(true) {
            case (acc, ((indexL, rowL), (indexR, rowR))) =>
              acc && indexL == indexR && Eq[Row].eqv(rowL, rowR)
          }
      override def show(as: Sheet): String = "Sheet(" + as.rowMap.toIndexedSeq.sortBy(_._1) + ")"
    }
  implicit val wbInstance: Monoid[Workbook] with Eq[Workbook] with Show[Workbook] =
    new Monoid[Workbook] with Eq[Workbook] with Show[Workbook] {
      override def empty: Workbook = Workbook(Map.empty)
      override def combine(f1: Workbook, f2: Workbook): Workbook =
        Workbook(combineMap(f1.sheetMap, f2.sheetMap))
      override def eqv(a1: Workbook, a2: Workbook): Boolean =
        (a1.sheetMap.toIndexedSeq.sortBy(_._1) zip
          a2.sheetMap.toIndexedSeq.sortBy(_._1))
          .foldLeft(true) {
            case (acc, ((nameL, sheetL), (nameR, sheetR))) =>
              acc && Eq[String].eqv(nameL, nameR) && Eq[Sheet].eqv(sheetL, sheetR)
          }

      override def show(as: Workbook): String = "Workbook(" + as.sheetMap.toIndexedSeq.sortBy(_._1) + ")"
    }

  // Utility functions
  private def mergeSets[A: Semigroup, B](list1: Set[A], list2: Set[A], on: A => B): Set[A] =
    combineMap(list1.map(l => (on(l), l)).toMap, list2.map(l => (on(l), l)).toMap).map { case (_, y) => y }.toSet

  private def combineMap[A, B: Semigroup](m1: Map[A, B], m2: Map[A, B]): Map[A, B] = {
    val k1           = Set(m1.keysIterator.toList: _*)
    val k2           = Set(m2.keysIterator.toList: _*)
    val intersection = k1 & k2
    val r1           = for (key <- intersection) yield (key -> Semigroup[B].combine(m1(key), m2(key)))
    val r2 = m1.filterKeys(!intersection.contains(_)) ++
      m2.filterKeys(!intersection.contains(_))
    r2.toMap ++ r1
  }
}

trait Lenses {
  import monocle._
  import monocle.macros.GenLens

  val doubleCellLens: NumericCell Lens Double = GenLens[NumericCell](_.data)
  val boolCellLens: BooleanCell Lens Boolean  = GenLens[BooleanCell](_.data)
  val stringCellLens: StringCell Lens String  = GenLens[StringCell](_.data)

  val rowLens: Lens[Row, Map[Pos, Cell]]         = GenLens[Row](_.cellMap)
  val sheetLens: Lens[Sheet, Map[Pos, Row]]      = GenLens[Sheet](_.rowMap)
  val wbLens: Lens[Workbook, Map[String, Sheet]] = GenLens[Workbook](_.sheetMap)

  def workbookCellOptional(ref: CellAddr): Optional[Workbook, Cell] = {
    import monocle.std.option._
    import monocle.function.At.at
    val atSheet: Lens[Workbook, Option[Sheet]] = wbLens.composeLens(at(ref.sheet))
    val atRow: Lens[Sheet, Option[Row]]        = sheetLens.composeLens(at(ref.row))
    val atCell: Lens[Row, Option[Cell]]        = rowLens.composeLens(at(ref.col))

    atSheet
      .composePrism(some[Sheet])
      .composeLens(atRow)
      .composePrism(some[Row])
      .composeLens(atCell)
      .composePrism(some[Cell])
  }

  implicit class MapLensWrapper[S, K, T](mapLens: Lens[S, Map[K, T]]) {
    import monocle.state.all._
    def contains(s: S)(cell: (K, T)): Boolean = {
      val map = mapLens.get(s)
      map.contains(cell._1) && map(cell._1) == cell._2
    }
    def +=(cell: (K, T))    = mapLens.mod_(_ + cell)
    def -=(cell: T)         = mapLens.mod_(_.filterNot { case (_, v) => v == cell })
    def &=(cells: (K, T)*)  = mapLens.mod_(_.filter(cells.contains))
    def &~=(cells: (K, T)*) = mapLens.mod_(_.filterNot(cells.contains))
    def |=(cells: (K, T)*)  = mapLens.mod_(_ ++ cells)
  }

}
