package models

import java.time.Clock

import org.scalatest.{FlatSpec, Matchers}

class BoardSpecs extends FlatSpec with Matchers {

  "Board" should "build a valid board" in {
    val b = BoardBuilder(10, 10, 100)
    b.sweepedCells should be ('empty)
    b.cells.size should be (100)
    b.cells.count { case Mine => true; case _ => false } === 100
    b.cells.count { case Empty => true; case _ => false } === 0
  }

  it should "loose when sweeping a mine" in {
    val b = BoardBuilder(10, 10, 100)
    val (newBoard, res) = b.sweep(0, 0)
    res.isInstanceOf[Boom.type] should be (true)
    newBoard.gameResult should be (Some(GameLost))
  }

  it should "detect win when sweeping last empty cell" in {
    // mine is on (x=0, y=0) in a 3x3 grid
    val cells = Seq(Mine, Empty, Empty, Empty, Empty, Empty, Empty, Empty, Empty)
    val b = Board(3, 3, cells, Map(), Clock.systemUTC().millis())
    val emptyCells = for {
      x <- 0 until 3
      y <- 0 until 3
      if x + y > 0
    } yield x -> y

    val (lastBoard, lastResult) = emptyCells.foldLeft(b -> (Clear: SweepResult)) { case ((board, res), (x, y)) =>
      board.sweep(x, y)
    }

    lastResult should matchPattern {
      case Win =>
    }

    lastBoard.gameResult should be (Some(GameWon))
  }

  it should "fail action when an already sweeped cell is trying to be swept" in {
    val b = BoardBuilder(10, 10, 0)
    val (newBoard, res) = b.sweep(0, 0)
    res.isInstanceOf[Invalid.type] should be (false)
    val (newBoard1, res1) = newBoard.sweep(0, 0)
    res1.isInstanceOf[Invalid.type] should be (true)
    newBoard.gameResult should be (None)
  }
}
