package controllers

import javax.inject._
import models._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc._

@Singleton
class MinesController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with BoardJsonFormat with SweepJsonFormat {

  private var currentBoard: Option[Board] = None

  case class MineSweepForm(x: Int, y: Int)

  val sweepForm = Form(
    mapping(
    "x" -> number,
    "y" -> number
    )(MineSweepForm.apply)(MineSweepForm.unapply)
  )

  def createBoard = Action { request =>
    val w = request.getQueryString("width").map(_.toInt).getOrElse(Board.DEFAULT_WIDTH)
    val h = request.getQueryString("height").map(_.toInt).getOrElse(Board.DEFAULT_HEIGHT)

    val newBoard = Board(h, w)
    currentBoard = Some(newBoard)
    Ok(Json.toJson(newBoard))
  }

  def getBoard = Action {
    currentBoard.map(b => Ok(Json.toJson(b))).getOrElse(NotFound("No minefield was created"))
  }

  def createSweep = Action { implicit request =>
    sweepForm.bindFromRequest().value.map { form =>
      if (currentBoard.isEmpty) {
        BadRequest("No minefield was created")
      } else {
        val board = currentBoard.get
        val (newBoard, result) = board.sweep(form.x, form.y)
        currentBoard = Some(newBoard)
        Ok(Json.toJson(result))
      }
    }.getOrElse {
      BadRequest("Missing cell row/column parameters (x, y)")
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