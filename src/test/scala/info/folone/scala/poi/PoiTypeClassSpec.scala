package info.folone.scala.poi

import org.typelevel.discipline.specs2.Discipline
import org.specs2.Specification

import cats.kernel.laws.discipline.SemigroupTests
import cats.kernel.laws.discipline.MonoidTests

import Arbitraries._

class PoiTypeClassSpec extends Specification with Discipline {
  def is = s2"""
  Typeclasses should
    
    satisfy for Cell       $e1
    satisfy for Row        $e2
    satisfy for Sheet      $e3
    satisfy for Workbook   $e4
"""

  def e1 = checkAll("Cell.semigroupLaws", SemigroupTests[Cell].semigroup)
  def e2 = checkAll("Row.semigroupLaws", SemigroupTests[Row].semigroup)
  def e3 = checkAll("Sheet.semigroupLaws", SemigroupTests[Sheet].semigroup)
  def e4 = checkAll("Workbook.monoidLaws", MonoidTests[Workbook].monoid)

}
