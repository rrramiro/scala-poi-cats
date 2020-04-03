package info.folone.scala.poi

sealed abstract class WorkbookVersion
final case object HSSF  extends WorkbookVersion
final case object XSSF  extends WorkbookVersion
final case object SXSSF extends WorkbookVersion
