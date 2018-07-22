package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._

import models._

@Singleton
class MinesController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with BoardJsonFormat with SweepJsonFormat {

  private var currentBoard: Option[Board] = None

  def createBoard(h: Int, w: Int) = Action {
    val newBoard = Board(h, w)
    currentBoard = Some(newBoard)
    Ok(Json.toJson(newBoard))
  }

  def getBoard = Action {
    currentBoard.map(b => Ok(Json.toJson(b))).getOrElse(NotFound("No minefield was created"))
  }

  def createSweep(x: Int, y: Int) = Action {
    if (currentBoard.isEmpty) {
      BadRequest("No minefield was created")
    } else {
      val board = currentBoard.get
      Ok(Json.toJson(board.sweep(x, y)))
    }
  }

}

sealed trait SweepResult
case object Win extends SweepResult
case object Boom extends SweepResult
case class CloseCall(bombs: Int) extends SweepResult
case object Clear extends SweepResult

trait BoardJsonFormat {
  implicit val cellWrites: Writes[Cell] = Writes {
    case Empty => Json.obj("cell" -> "EMPTY")
    case Mine => Json.obj("cell" -> "MINE")
  }

  implicit val boardWrites: OWrites[Board] = Json.writes[Board]
}

trait SweepJsonFormat {
  implicit val sweepWrites: Writes[SweepResult] = Writes {
    case Win => Json.obj("result" -> "WIN")
    case Boom => Json.obj("result" -> "BOOM")
    case CloseCall(n) => Json.obj("result" -> "NEAR", "count" -> n)
    case Clear => Json.obj("result" -> "CLEAR")
  }
}