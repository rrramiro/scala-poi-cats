package info.folone.scala.poi

trait FontColor

object FontColor {
  case object Normal extends FontColor
  case object Red    extends FontColor
}

final case class CellStyle(font: Font, dataFormat: DataFormat)

final case class Font(name: String = Font.defaultFontName, bold: Boolean = false, color: FontColor)

object Font {
  def defaultFontName: String = "Arial"
}

final case class DataFormat(format: String)
