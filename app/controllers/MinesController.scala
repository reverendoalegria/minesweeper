package controllers

import javax.inject._
import models._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import play.api.mvc._

@Singleton
class MinesController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with BoardJsonFormat {

  private var currentBoard: Option[Board] = None

  case class MineSweepForm(x: Int, y: Int)

  val sweepForm = Form(
    mapping(
    "x" -> number,
    "y" -> number
    )(MineSweepForm.apply)(MineSweepForm.unapply)
  )

  private def getFormIntValue(n: String, default: Int)(implicit request: Request[AnyContent]): Int = {
    val formMap = request.body.asFormUrlEncoded.getOrElse(Map())
    formMap.get(n).flatMap(_.headOption).map(_.toInt).getOrElse(default)
  }

  def createBoard = Action { implicit request =>
    val w = getFormIntValue("width", BoardBuilder.DEFAULT_WIDTH)
    val h = getFormIntValue("height", BoardBuilder.DEFAULT_HEIGHT)
    val m = getFormIntValue("mines", BoardBuilder.DEFAULT_MINES)

    val newBoard = BoardBuilder(h, w, m)
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
        Ok(Json.obj("sweepResult" -> result, "board" -> newBoard))
      }
    }.getOrElse {
      BadRequest("Missing cell row/column parameters (x, y)")
    }
  }

}

trait BoardJsonFormat {

  implicit val resultWrites: Writes[GameResult] = Writes {
    case GameLost => Json.obj("result" -> "LOST")
    case GameWon => Json.obj("result" -> "WON")
  }

  implicit val sweepWrites: Writes[SweepResult] = Writes {
    case Win => Json.obj("result" -> "WIN")
    case Boom => Json.obj("result" -> "BOOM")
    case CloseCall(n) => Json.obj("result" -> "NEAR", "count" -> n)
    case Clear => Json.obj("result" -> "CLEAR")
    case Invalid => Json.obj("result" -> "INVALID")
  }

  implicit val cellWrites: Writes[Cell] = Writes {
    case Empty => Json.obj("cell" -> "EMPTY")
    case Mine => Json.obj("cell" -> "MINE")
  }

  implicit val mapWrites: Writes[Map[Int, SweepResult]] = Writes[Map[Int, SweepResult]] { objectMap =>
      Json.obj(
        objectMap.map {
          case (s, o) => s.toString -> (Json.toJson(o): JsValueWrapper)
      }.toSeq:_*)
  }

  implicit val boardWrites: OWrites[Board] = OWrites[Board] { b =>
    Json.obj(
      "width" -> b.width,
      "height" -> b.height,
      "sweepedCells" -> Json.toJson(b.sweepedCells),
      "startTimeMs" -> b.startTimeMs,
      "gameResult" -> b.gameResult
    )
  }
}
