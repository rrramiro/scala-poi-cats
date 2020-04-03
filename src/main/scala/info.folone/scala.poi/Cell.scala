package info.folone.scala.poi

import java.util.Date

import cats.{Eq, Show}

sealed trait Cell {
  def style: Option[CellStyle]
}
case class StringCell(data: String, style: Option[CellStyle]   = None) extends Cell
case class NumericCell(data: Double, style: Option[CellStyle]  = None) extends Cell
case class DateCell(data: Date, style: Option[CellStyle]       = None) extends Cell
case class BooleanCell(data: Boolean, style: Option[CellStyle] = None) extends Cell
case class FormulaCell(data: Formula, style: Option[CellStyle] = None) extends Cell { //TODO data.value.dropWhile(_ == '=')
  import equalities.formulaCellEqualInstance //TODO check
  override def equals(obj: Any) =
    obj != null && obj.isInstanceOf[FormulaCell] && Eq[FormulaCell].eqv(obj.asInstanceOf[FormulaCell], this)
  override def hashCode: Int = data.hashCode
}
