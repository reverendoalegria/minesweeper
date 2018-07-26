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

  def sweep(x: Int, y: Int): (Board, SweepResult) = {
    val cellIdx = cellIndex(x, y)
    if (sweepedCells.contains(cellIdx)) this -> Invalid
    else {
      makeSweep(x, y, cellIdx)
    }
  }

  def cellIndex(x: Int, y: Int): Int = (y * width) + x

  private def makeSweep(x: Int, y: Int, cellIdx: Int): (Board, SweepResult) = {
    val result = cells(cellIdx) match {
      case Empty if onlyBombsAreCovered(cellIdx) => Win
      case Empty =>
        findNearBombs(x, y) match {
          case 0 => Clear
          case n => CloseCall(n)
        }
      case Mine => Boom
    }

    withSweep(cellIdx, result) -> result
  }

  /*
    Sugar for copying this board after a sweep was made
   */
  private def withSweep(cellIdx: Int, result: SweepResult): Board = {
    copy(sweepedCells = sweepedCells.updated(cellIdx, result)).copy(gameResult = result match {
      case Win => Some(GameWon)
      case Boom => Some(GameLost)
      case _ => None
    })
  }

  /**
    * If only bombs are covered it means user found all non-bombs cells
    */
  private def onlyBombsAreCovered(excludingIdx: Int): Boolean = {
    val emptyCellsList = cells.zipWithIndex.collect { case (Empty, idx) => idx }
    emptyCellsList.sorted == (sweepedCells.keys.toSeq :+ excludingIdx).sorted
  }

  /**
    * When a clear cell is found we need to find near bombs and count 'em
    */
  private def findNearBombs(x: Int, y: Int): Int = {
    0
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