package models

import org.scalatest.{FlatSpec, Matchers}

class BoardSpecs extends FlatSpec with Matchers {

  "Board" should "build a valid board" in {
    val b = Board(10, 10, 100)
    b.sweepedCellsIndexes should be ('empty)
    b.cells.size should be (100)
    b.cells.count { case Mine => true; case _ => false } === 100
    b.cells.count { case Empty => true; case _ => false } === 0
  }
}
