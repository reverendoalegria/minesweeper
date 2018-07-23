package models

import java.time.Clock
import java.util.concurrent.TimeUnit

import controllers._

import scala.concurrent.duration.Duration
import scala.util.Random

sealed trait Cell
case object Empty extends Cell
case object Mine extends Cell

object Board {

  val DEFAULT_WIDTH = 10
  val DEFAULT_HEIGHT = 10

  /**
    * Boards should be created with a defined height/width
    */
  def apply(h: Int, w: Int, bombs: Int = 10): Board = {
    val rnd = new Random()
    val cellsCount = h * w
    val cellsList = (0 until cellsCount).toList
    val bombsCells = rnd.shuffle(cellsList).take(bombs).toSet
    val cells = cellsList.map { idx =>
      if (bombsCells.contains(idx)) Mine
      else Empty
    }
    new Board(w, h, cells, Seq(), Clock.systemUTC().millis())
  }
}

case class Board(width: Int, height: Int, cells: Seq[Cell], sweepedCellsIndexes: Seq[Int], startTimeMs: Long) {
  def elapsedTimeSeconds: Long = Duration(Clock.systemUTC().millis() - startTimeMs, TimeUnit.MILLISECONDS).toSeconds

  def sweep(x: Int, y: Int): (Board, SweepResult) = {
    val cellIdx = (y * width) + x
    val result = cells(cellIdx) match {
      case Empty if onlyBombsAreCovered => Win
      case Empty =>
        findNearBombs(x, y) match {
          case 0 => Clear
          case n => CloseCall(n)
        }
      case Mine => Boom
    }

    withSweep(cellIdx) -> result
  }

  private def withSweep(cellIdx: Int): Board = {
    copy(sweepedCellsIndexes = sweepedCellsIndexes :+ cellIdx)
  }

  /**
    * If only bombs are covered it means user found all non-bombs cells
    */
  private def onlyBombsAreCovered: Boolean = {
    val minesIdxList = cells.zipWithIndex.collect { case (Mine, idx) => idx }
    minesIdxList.sorted == sweepedCellsIndexes.sorted
  }

  /**
    * When a clear cell is found we need to find near bombs and count 'em
    */
  private def findNearBombs(x: Int, y: Int): Int = {
    0
  }
}

