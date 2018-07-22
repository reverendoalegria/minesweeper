package controllers

import java.time.Clock
import java.util.concurrent.TimeUnit

import javax.inject._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.duration.Duration

@Singleton
class MinesController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with BoardJsonFormat {

  private var currentBoard: Option[Board] = None

  def createBoard(h: Int, w: Int) = Action {
    currentBoard = Some(Board(h, w))
    Ok(currentBoard)
  }

  def getBoard = Action {
    Ok(currentBoard)
  }

  def createSweep(x: Int, y: Int) = Action {
    if (currentBoard.isEmpty) {
      BadRequest("No minefield was created")
    } else {
      val board = currentBoard.get
      board.
    }

  }

}

sealed trait Cell
case object Empty extends Cell
case object Mine extends Cell

object Board {
  def apply(h: Int, w: Int) = {
    Board(w, h, Seq(), Clock.systemUTC().millis())
  }
}

case class Board(width: Int, height: Int, cells: Seq[Cell], startTimeMs: Long) {
  def elapsedTimeSeconds: Long = Duration(Clock.systemUTC().millis() - startTimeMs, TimeUnit.MILLISECONDS).toSeconds

  def sweep(x: Int, y: Int): SweepResult = {
    val pos = (y * width) + x
    cells(pos) match {
      case Empty if allClear => {
        findNearBombs(x, y) match {
          case 0 => Clear
          case n => CloseCall(n)
        }
      }
      case Empty => Clear
      case Mine =>
    }

  }

  private def findNearBombs(x: Int, y: Int): Int = {
    0
  }
}

sealed trait SweepResult
case object Win
case object Boom
case class CloseCall(bombs: Int)
case object Clear

trait BoardJsonFormat {
  implicit val boardWrites = Json.writes[Board]
}