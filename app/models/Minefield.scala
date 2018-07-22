package models

import java.time.Clock
import java.util.concurrent.TimeUnit

import controllers._

import scala.concurrent.duration.Duration

sealed trait Cell
case object Empty extends Cell
case object Mine extends Cell

object Board {

  /**
    * Boards should be created with a defined height/width
    */
  def apply(h: Int, w: Int): Board = {
    Board(w, h, Seq(), Clock.systemUTC().millis())
  }
}

case class Board(width: Int, height: Int, cells: Seq[Cell], startTimeMs: Long) {
  def elapsedTimeSeconds: Long = Duration(Clock.systemUTC().millis() - startTimeMs, TimeUnit.MILLISECONDS).toSeconds

  def sweep(x: Int, y: Int): SweepResult = {
    val pos = (y * width) + x
    cells(pos) match {
      case Empty if onlyBombsAreCovered => Win
      case Empty =>
        findNearBombs(x, y) match {
          case 0 => Clear
          case n => CloseCall(n)
        }
      case Mine => Boom
    }

  }

  /**
    * If only bombs are covered it means user found all non-bombs cells
    */
  private def onlyBombsAreCovered: Boolean = false

  /**
    * When a clear cell is found we need to find near bombs and count 'em
    */
  private def findNearBombs(x: Int, y: Int): Int = {
    0
  }
}

