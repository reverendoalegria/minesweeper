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

  it should "fail when an already sweeped cell is trying to be swept" in {
    val b = BoardBuilder(10, 10, 0)

    val (newBoard, res) = b.sweep(0, 0)
    res.isInstanceOf[Invalid.type] should be (false)
    newBoard.gameResult should be (None)

    val (newBoard1, res1) = newBoard.sweep(0, 0)
    res1.isInstanceOf[Invalid.type] should be (true)
    newBoard1.gameResult should be (None)
  }

  it should "fail when an invalid cell coordinate is trying to be swept" in {
    val b = BoardBuilder(10, 10, 0)
    val (newBoard, res) = b.sweep(11, 11)
    res.isInstanceOf[Invalid.type] should be (true)
    newBoard.gameResult should be (None)
  }

  it should "find near mines when sweeping a close call" in {
    // mines are on (x=0, y=0) and (x=1, y=0) in a 3x3 grid
    // sweeping (x=0, y=1) should find 2 near mines
    val cells = Seq(Mine, Mine, Empty, Empty, Empty, Empty, Empty, Empty, Empty)
    val board0 = Board(3, 3, cells, Map(), Clock.systemUTC().millis())
    val (board1, sweepResult1) = board0.sweep(x = 0, y = 1)
    sweepResult1 should be (CloseCall(2))
    val (board2, sweepResult2) = board1.sweep(x = 1, y = 1)
    sweepResult2 should be (CloseCall(2))
    val (board3, sweepResult3) = board2.sweep(x = 2, y = 0)
    sweepResult3 should be (CloseCall(1))
    val (_, sweepResult4) = board3.sweep(x = 2, y = 1)
    sweepResult4 should be (CloseCall(1))
  }

  /* Make sure near cells strategy won't miss any possible cells */
  it should "evaluate all near cells given a coordinate" is pending

  it should "clear all cells that don't have near mines when finding one" in {
    val cells = Seq(Mine, Mine, Mine, Empty, Empty, Empty, Empty, Empty, Empty)
    val board0 = Board(3, 3, cells, Map(), Clock.systemUTC().millis())
    // cells (0,0) :: (1,0) :: (2,0) have mines (the entire 1st row)
    // sweeping anything on 2nd row should find near mines: |2|3|2|
    // sweeping anything on 3rd row should give NO-mines near, so should CLEAR all 3rd row on first sweep
    val (board1, result1) = board0.sweep(2,2)
    board1.gameResult should be (None)
    result1 should be (Clear)
    board1.sweepedCells.size should be (3)
    board1.sweepedCells(6) should be (Clear)
    board1.sweepedCells(7) should be (Clear)
    board1.sweepedCells(8) should be (Clear)
  }
}
