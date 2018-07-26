package models

import java.time.Clock
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.util.Random

sealed trait Cell
case object Empty extends Cell
case object Mine extends Cell

/*
  FIXME: Renamed from a companion object to avoid seralization conflict via Play-Json Macros having to infere multiple `apply` methods.
  https://stackoverflow.com/questions/15053053/how-to-fix-ambiguous-reference-to-overloaded-apply-method-when-deserializing-j
 */
object BoardBuilder {

  val DEFAULT_WIDTH = 10
  val DEFAULT_HEIGHT = 10
  val DEFAULT_MINES = 10

  /**
    * Boards should be created with a defined height/width
    */
  def apply(h: Int, w: Int, bombs: Int): Board = {
    val cells: Seq[Cell] = buildMinefield(h, w, bombs)
    Board(w, h, cells, Map(), Clock.systemUTC().millis())
  }

  private def buildMinefield(h: Int, w: Int, bombs: Int): Seq[Cell] = {
    val rnd = new Random()
    val cellsCount = h * w
    val cellsList = (0 until cellsCount).toList
    val bombsCells = rnd.shuffle(cellsList).take(bombs).toSet
    val cells = cellsList.map { idx =>
      if (bombsCells.contains(idx)) Mine
      else Empty
    }
    cells
  }
}

case class Board(width: Int, height: Int, cells: Seq[Cell], sweepedCells: Map[Int, SweepResult], startTimeMs: Long, gameResult: Option[GameResult] = None) {
  def elapsedTimeSeconds: Long = Duration(Clock.systemUTC().millis() - startTimeMs, TimeUnit.MILLISECONDS).toSeconds

  /**
    * Sweep a cell. A new board is provided with updated state,
    * as well as consequences for this action (#SweepResult).
    * If a mine was hit, Game is lost, which is indicated both in Board#gameResult
    * and SweepResult.
    */
  def sweep(x: Int, y: Int): (Board, SweepResult) = {
    val cellIdx = cellIndex(x, y)
    if (sweepedCells.contains(cellIdx) || cellIdx < 0 || cellIdx >= cells.size) this -> Invalid
    else {
      makeSweep(x, y, cellIdx)
    }
  }

  /**
    * Find a cell given its coordinates
    */
  def cell(x: Int, y: Int): Option[Cell] = {
    if (x < 0 || x >= width || y < 0 || y >= height) None
    else Option(cells(cellIndex(x, y)))
  }

  private def cellIndex(x: Int, y: Int): Int = (y * width) + x

  private def makeSweep(x: Int, y: Int, cellIdx: Int): (Board, SweepResult) = {
    cells(cellIdx) match {
      case Empty if onlyBombsAreCovered(cellIdx) => withSweep(cellIdx, Win)
      case Empty =>
        findNearBombs(x, y) match {
          case 0 =>
            val (newBoard, _) = withSweep(cellIdx, Clear)
            val clearedBoard = newBoard.recursiveCleanClearing(x, y)
            clearedBoard -> Clear
          case n => withSweep(cellIdx, CloseCall(n))
        }
      case Mine => withSweep(cellIdx, Boom)
    }
  }

  private def recursiveCleanClearing(x: Int, y: Int): Board = {
    nearCellsOptions(x, y).
      filter(c => cell(c.x, c.y).isDefined).
      filterNot(c => sweepedCells.contains(cellIndex(c.x, c.y))).
      foldLeft(this) { (board, pos) =>
        board.sweep(pos.x, pos.y)._1
    }
  }

  /**
   * Sugar for copying this board after a sweep was made
   */
  private def withSweep(cellIdx: Int, result: SweepResult): (Board, SweepResult) = {
    copy(sweepedCells = sweepedCells.updated(cellIdx, result)).copy(gameResult = result match {
      case Win => Some(GameWon)
      case Boom => Some(GameLost)
      case _ => None
    }) -> result
  }

  /**
    * If only bombs are covered it means user found all non-bombs cells
    */
  private def onlyBombsAreCovered(excludingIdx: Int): Boolean = {
    val emptyCellsList = cells.zipWithIndex.collect { case (Empty, idx) => idx }
    emptyCellsList.sorted == (sweepedCells.keys.toSeq :+ excludingIdx).sorted
  }

  implicit def intTupleToCellPos( t: (Int, Int) ): CellPos = CellPos(t._1, t._2)
  case class CellPos(x: Int, y: Int)

  private def nearCells(x: Int, y: Int): Seq[Cell] = nearCellsOptions(x, y).flatMap(c => cell(c.x, c.y))

  private def nearCellsOptions(x: Int, y: Int): Seq[CellPos] = {
    CellPos(x - 1, y - 1) ::
    CellPos(x, y - 1) ::
    CellPos(x + 1, y - 1) ::
    CellPos(x - 1, y) ::
    CellPos(x + 1, y) ::
    CellPos(x - 1, y + 1) ::
    CellPos(x, y + 1) ::
    CellPos(x + 1, y + 1) :: Nil
  }



  /**
    * When a clear cell is found we need to find near bombs and count 'em
    */
  private def findNearBombs(x: Int, y: Int): Int = {
    /*
        -------
        |x-1,y-1|x,y-1|x+1,y-1|
        |x-1,y  | X,Y |x+1,y  |
        |x-1,y+1|x,y+1|x+1,y+1|
        -------
     */
    countBomb(x - 1, y - 1) +
    countBomb(x, y - 1) +
    countBomb(x + 1, y - 1) +
    countBomb(x - 1, y) +
    countBomb(x + 1, y) +
    countBomb(x - 1, y + 1) +
    countBomb(x, y + 1) +
    countBomb(x + 1, y + 1)
  }

  private def countBomb(x: Int, y: Int): Int = {
    cell(x, y).count {
      case Mine => true
      case _ => false
    }
  }

  private def isMine(x: Int, y: Int): Boolean = {
    cell(x, y).exists {
      case Mine => true
      case _ => false
    }
  }
}

sealed trait SweepResult
case object Win extends SweepResult
case object Boom extends SweepResult
case class CloseCall(bombs: Int) extends SweepResult
case object Clear extends SweepResult
case object Invalid extends SweepResult

sealed trait GameResult
case object GameWon extends GameResult
case object GameLost extends GameResult