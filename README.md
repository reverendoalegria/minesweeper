# minesweeper-API

## The Game
Develop the classic game of Minesweeper

## What to build (prioritized)
 * `Make it a playable Demo`
   * Design and implement a (non-documented) RESTful API for the game
   * Ability to start a new game and preserve/resume the old ones
   * When a cell with no adjacent mines is revealed, all adjacent squares will be revealed (and repeat)
   * Time tracking
   * Detect when game is over
   * Ability to select the game parameters: number of rows, columns, and mines
 * `Make it public-available`
   * Ability to 'flag' a cell with a question mark or red flag
   * Persistence
   * Implement an API client library for the API designed above. Ideally, in a different language, of your preference, to the one used for the API
   * Ability to support multiple users/accounts

## Offered Deliverables (given time and expectations):
 * Accesible endpoint for the API - only playable via simulated REST calls
 * Code in a public Github repo
 * README file with the decisions taken and important notes

## Game API
 * API is exposed on http://minesweeper.romangarcia.net
 * There are 2 resources available.
   * Minefield: This resource represents the game board.
   * Sweep: This resource represents a sweep made on the minefield
 * The minefield model provides information about:
   * width: The minefield width in cells
   * height: The minefield height in cells
   * sweepedCells: All previously sweepedCells, giving cell index and sweep result 
   * startTimeMs: Epoch millis at the time the minefield was created
   * gameResult: NULL if the game is ongoing. WIN or LOSE if the game is over.
 * For simplicity, each Sweep replies with the updated minefield, assuming the client will be interested. 
 For sure not the restful approach.
 
## How to Play
 * Create a Minefield
   * `POST http://minesweeper.romangarcia.net/minefield?width=10&height=10&mines=50`

 * Read the Minefield
   * `GET http://minesweeper.romangarcia.net/minefield`

 * Create sweeps until you either Win / Lose
   * `POST http://minesweeper.romangarcia.net/minefield/sweep`
   
## TODO / Debt
 * Rename all references of `Board` to `Minefield`
 * Stop using Int->Int tuples all around. Migrate to some sort of case class that represents a cell coordinate (CellPos maybe)
 * Implement multi-player. Since this is a first time Demo
 * Start time and time counting should start when the first sweep is made
 * Discuss Sweep response, should we include the whole Minefield? Should the client go get a new one?
 * Prioritize all pending backlog!