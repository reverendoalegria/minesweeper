import models._
import play.api.libs.json._
import play.api.libs.ws.EmptyBody

import scala.concurrent.{Await, ExecutionContext}
import scala.io.StdIn

object ConsoleGame extends App {

  val url = "http://localhost:9000"
  import scala.concurrent.ExecutionContext.Implicits._
  val client = new MinefieldClient(url)

  var input: String = ""
  do {
    println(
      """
        |1. New Game
        |0. Exit
      """.stripMargin)
    StdIn.readInt() match {
      case 1 => startGame()
      case 0 => sys.exit()
    }
  } while (input.nonEmpty)

  sys.addShutdownHook { () =>
    client.shutdown()
  }

  def startGame(): Unit = {

    val result = whileGameIsPlaying { board =>
      drawBoard(board)
      val (x, y) = askCoordinates()
      client.sweep(x, y)
    }

    client.getBoard.foreach { board =>
      drawBoard(board)
      println(s"Game result: ${result.get}!")
    }

    sys.exit(1)
  }

  def whileGameIsPlaying(fn: BoardView => (BoardView, SweepResult)): Option[GameResult] = {
    val (code, txt) = client.start()
    println(s"Response for start request: $code > $txt")

    def shouldKeepPlaying(gameResult: Option[GameResult], lastSweepResult: Option[SweepResult]): Boolean = {
      gameResult.isEmpty && !lastSweepResult.exists {
        case Win => true
        case Boom => true
        case _ => false
      }
    }

    var maybeBoard: Option[BoardView] = None
    var lastSweepResult: Option[SweepResult] = None
    do {
      maybeBoard = client.getBoard
      maybeBoard.foreach { b =>
        val (newBoard, sweepResult) = fn(b)
        lastSweepResult = Some(sweepResult)
        maybeBoard = Some(newBoard)
      }
    } while (shouldKeepPlaying(maybeBoard.flatMap(_.gameResult), lastSweepResult))
    maybeBoard.flatMap(_.gameResult)
  }

  def askCoordinates(): (Int, Int) = {
    println("\n\nEnter coordinates: X,Y")
    val line = StdIn.readLine()
    val parts = line.split(",")
    parts(0).toInt -> parts(1).toInt
  }

  def drawBoard(board: BoardView): Unit = {
    val sweepMap = board.sweepedCells
    for {
      y <- 0 until board.height
      x <- 0 until board.width
    } {
      val index = board.cellIndex(x, y)
      val cellDraw = sweepMap.get(index) match {
        case Some(Boom) => "X"
        case Some(CloseCall(n)) => n.toString
        case Some(Clear) => "O"
        case _ => " "
      }
      print(cellDraw + '|')
      if (x == board.width - 1) println()
    }
  }
}

class MinefieldClient(url: String)(implicit ec: ExecutionContext) extends MinefieldJsonReads {
  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import play.api.libs.ws.ahc.AhcWSClient

  import scala.concurrent.duration._
  import play.api.http.Status._
  private val TIMEOUT_DURATION: FiniteDuration = 10.seconds
  private implicit val system: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val ws = AhcWSClient()

  def start(): (Int, String) = {
    Await.result(ws.url(s"$url/minefield").post(EmptyBody).map(r => r.status -> r.body), TIMEOUT_DURATION)
  }

  def sweep(x: Int, y: Int): (BoardView, SweepResult) = {
    Await.result(ws.url(s"$url/minefield/sweep").post(Map("x" -> x.toString, "y" -> y.toString)).map(_.json).map { js =>
      val sweepResult = (js \ "sweepResult").as[SweepResult]
      val board = (js \ "board").as[BoardView]
      board -> sweepResult
    }, TIMEOUT_DURATION)
  }

  def getBoard: Option[BoardView] = {
    Await.result(ws.url(s"$url/minefield").get().map { resp =>
      if (resp.status == NOT_FOUND) {
        None
      } else {
        Some(resp.json.as[BoardView])
      }
    }, TIMEOUT_DURATION)
  }

  def shutdown(): Unit = {
    system.terminate()
    ws.close()
  }
}

trait MinefieldJsonReads {
  implicit val cellReads: Reads[Cell] = Reads { js =>
    (js \ "cell").as[String] match {
      case "MINE" => JsSuccess(Mine)
      case "EMPTY" => JsSuccess(Empty)
      case _ => JsError("Not a valid cell response")
    }
  }
  implicit val resultReads: Reads[GameResult] = Reads { js =>
    (js \ "result").as[String] match {
      case "WON" => JsSuccess(GameWon)
      case "LOST" => JsSuccess(GameLost)
      case _ => JsError("Not a valid game result")
    }
  }

  implicit lazy val sweepResultReads: Reads[SweepResult] = Reads { js =>
    (js \ "result").as[String] match {
      case "WIN" => JsSuccess(Win)
      case "BOOM" => JsSuccess(Boom)
      case "NEAR" => JsSuccess(CloseCall((js \ "count").as[Int]))
      case "CLEAR" => JsSuccess(Clear)
      case "INVALID" => JsSuccess(Invalid)
    }
  }

  implicit val mapReads: Reads[Map[Int, SweepResult]] = Reads { js =>
    JsSuccess(js.as[Map[String, SweepResult]].map { case (k, v) =>
      k.toInt -> v
    })
  }

  implicit val boardReads: Reads[BoardView] = Json.reads[BoardView]
}

case class BoardView(width: Int, height: Int, sweepedCells: Map[Int, SweepResult], startTimeMs: Long, gameResult: Option[GameResult]) {
  def cellIndex(x: Int, y: Int): Int = (y * width) + x
}