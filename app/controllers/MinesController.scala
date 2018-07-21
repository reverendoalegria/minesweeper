package controllers

import javafx.scene.control
import javax.inject._

@Singleton
class MinesController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def start = Action {
    Ok(Json.obj("board" -> Json.arr()))
  }

  def play(x: Int, y: Int) = Action {

  }

}

sealed trait Cell
case object Empty extends Cell
case object Mine extends Cell
case class Board(cells: Seq[Cell], time: Int)
