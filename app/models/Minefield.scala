package models

import java.time.Clock
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.util.Random

sealed trait Cell
case object Empty extends Cell
case object Mine extends Cell

object Board {

  val DEFAULT_WIDTH = 10
  val DEFAULT_HEIGHT = 10
  val DEFAULT_MINES = 10

  /**
    * Boards should be created with a defined height/width
    */
  def apply(h: Int, w: Int, bombs: Int): Board = {
    val cells: List[Cell] = buildMinefield(h, w, bombs)
    new Board(w, h, cells, Seq(), Clock.systemUTC().millis())
  }

  private def buildMinefield(h: Int, w: Int, bombs: Int) = {
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

case class Board(width: Int, height: Int, cells: Seq[Cell], sweepedCells: Seq[Int], startTimeMs: Long, gameResult: Option[GameResult] = None) {
  def elapsedTimeSeconds: Long = Duration(Clock.systemUTC().millis() - startTimeMs, TimeUnit.MILLISECONDS).toSeconds

  def sweep(x: Int, y: Int): (Board, SweepResult) = {
    val cellIdx = (y * width) + x
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

  private def withSweep(cellIdx: Int, result: SweepResult): Board = {
    copy(sweepedCells = sweepedCells :+ cellIdx).copy(gameResult = result match {
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
    emptyCellsList.sorted == (sweepedCells :+ excludingIdx).sorted
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

sealed trait GameResult
case object GameWon extends GameResult
case object GameLost extends GameResult