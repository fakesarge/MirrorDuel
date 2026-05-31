package mirrorduel;

import mirrorduel.GameEnums.*;
import java.util.ArrayList;
import java.util.List;

//GameManager is the brain of the game
//it holds all the game state and processes every action - clicks rotations and AI moves
//the UI panels talk to GameManager to make things happen and read state to display it
public class GameManager {
    private Board board;
    private PlayerID currentPlayer;
    private PlayerID winner;
    private GameScreen screen;

    //the piece the current player has selected but not yet moved
    private Piece selectedPiece;
    //true after the player has picked a piece and needs to pick a destination cell
    private boolean awaitingMoveTarget;

    //if this is not null we are in AI mode and this object makes moves for the RED player
    private AIPlayer aiOpponent;

    //move history shown in the sidebar - capped at 20 entries
    private final List<String> moveLog = new ArrayList<>();
    private int turnNumber = 1;

    //tracks the squares involved in the most recent move so the board can highlight them
    //values of -1 mean no move has happened yet so nothing should be highlighted
    private int lastMoveFromRow = -1;
    private int lastMoveFromCol = -1;
    private int lastMoveToRow   = -1;
    private int lastMoveToCol   = -1;
    //true if the last action was a rotation so only one square needs to be highlighted
    private boolean lastMoveWasRotate = false;

    public GameManager() {
        screen = GameScreen.MAIN_MENU;
    }

    //resets everything and starts a fresh game
    //BLUE always goes first whether playing against a human or the AI
    public void startNewGame() {
        board = new Board();
        currentPlayer = PlayerID.BLUE;
        winner = null;
        screen = GameScreen.PLAYING;
        selectedPiece = null;
        awaitingMoveTarget = false;
        moveLog.clear();
        turnNumber = 1;
        lastMoveFromRow = -1;
        lastMoveFromCol = -1;
        lastMoveToRow   = -1;
        lastMoveToCol   = -1;
        lastMoveWasRotate = false;
    }

    //called when the human player clicks a cell on the board
    //first click selects a piece, second click moves it or switches to another piece
    //returns true if the board state changed and the UI needs to be redrawn
    public boolean handleCellClick(int row, int col) {
        if (screen != GameScreen.PLAYING || winner != null) return false;
        //block human input while the AI is computing its move
        if (isAITurn()) return false;

        Piece clickedPiece = board.pieceAt(row, col);

        if (awaitingMoveTarget) {
            //the player already selected a piece and is now choosing where to move it
            if (clickedPiece == null) {
                //capture the piece's current position before the move changes it
                int fromRow = selectedPiece.row;
                int fromCol = selectedPiece.col;
                boolean moved = board.movePiece(selectedPiece, row, col);
                if (moved) {
                    //record the origin and destination for the yellow highlight
                    lastMoveFromRow   = fromRow;
                    lastMoveFromCol   = fromCol;
                    lastMoveToRow     = row;
                    lastMoveToCol     = col;
                    lastMoveWasRotate = false;
                    log("Move " + selectedPiece.type + " to (" + row + "," + col + ")");
                    endTurn();
                    return true;
                }
            }
            //clicking the same piece again cancels the selection
            if (clickedPiece == selectedPiece) {
                selectedPiece = null;
                awaitingMoveTarget = false;
                return true;
            }
            //clicking a different own piece switches the selection to that piece
            if (clickedPiece != null && clickedPiece.owner == currentPlayer) {
                selectedPiece = clickedPiece;
                return true;
            }
            return false;
        }

        //first click - select one of the current player's pieces
        if (clickedPiece != null && clickedPiece.owner == currentPlayer) {
            selectedPiece = clickedPiece;
            awaitingMoveTarget = true;
            return true;
        }
        return false;
    }

    //called when the Rotate button is pressed
    //rotates the currently selected mirror and ends the turn
    //returns true if a rotation happened and the UI needs to redraw
    public boolean handleRotate() {
        if (screen != GameScreen.PLAYING || winner != null) return false;
        if (isAITurn()) return false;
        if (selectedPiece == null || !selectedPiece.isMirror()) return false;
        if (selectedPiece.owner != currentPlayer) return false;

        //record the square before rotating so the highlight knows which cell to mark
        lastMoveFromRow   = selectedPiece.row;
        lastMoveFromCol   = selectedPiece.col;
        lastMoveToRow     = selectedPiece.row;
        lastMoveToCol     = selectedPiece.col;
        lastMoveWasRotate = true;

        board.rotateMirror(selectedPiece);
        log("Rotate mirror at (" + selectedPiece.row + "," + selectedPiece.col + ")");
        selectedPiece = null;
        awaitingMoveTarget = false;
        endTurn();
        return true;
    }

    //cleans up selection state then checks for a winner and swaps the active player
    private void endTurn() {
        selectedPiece = null;
        awaitingMoveTarget = false;

        PlayerID w = board.checkWinner();
        if (w != null) {
            winner = w;
            screen = GameScreen.VICTORY;
            return;
        }

        currentPlayer = currentPlayer.opponent();
        turnNumber++;
    }

    //adds a text entry to the move log keeping only the most recent 20 entries
    private void log(String msg) {
        moveLog.add("T" + turnNumber + " [" + currentPlayer + "] " + msg);
        if (moveLog.size() > 20) moveLog.remove(0);
    }

    //sets the AI opponent - pass null to switch to 2-player mode
    public void setAIOpponent(AIPlayer ai) { this.aiOpponent = ai; }

    //returns true when it is the AI's turn to move
    //the AI always plays as RED so this triggers when currentPlayer is RED and an AI is set
    public boolean isAITurn() {
        return aiOpponent != null
                && screen == GameScreen.PLAYING
                && winner == null
                && currentPlayer == PlayerID.RED;
    }

    //computes the AI's best move and applies it to the board
    //this must be called from a background thread so the UI does not freeze while thinking
    //returns true if a move was successfully made
    public boolean doAIMove() {
        if (!isAITurn()) return false;

        AIPlayer.Move move = aiOpponent.getBestMove(board);
        if (move == null) return false;

        Piece piece = board.pieceAt(move.pieceRow, move.pieceCol);
        if (piece == null) return false;

        if (move.type == AIPlayer.Move.Type.ROTATE) {
            //record the square for the highlight before rotating
            lastMoveFromRow   = piece.row;
            lastMoveFromCol   = piece.col;
            lastMoveToRow     = piece.row;
            lastMoveToCol     = piece.col;
            lastMoveWasRotate = true;
            board.rotateMirror(piece);
            log("Rotate mirror at (" + piece.row + "," + piece.col + ")");
        } else {
            //capture the origin before movePiece updates the piece's row and col
            lastMoveFromRow   = piece.row;
            lastMoveFromCol   = piece.col;
            lastMoveToRow     = move.targetRow;
            lastMoveToCol     = move.targetCol;
            lastMoveWasRotate = false;
            board.movePiece(piece, move.targetRow, move.targetCol);
            log("Move " + piece.type + " to (" + move.targetRow + "," + move.targetCol + ")");
        }
        endTurn();
        return true;
    }

    public Board getBoard()              { return board; }
    public PlayerID getCurrentPlayer()   { return currentPlayer; }
    public PlayerID getWinner()          { return winner; }
    public GameScreen getScreen()        { return screen; }
    public Piece getSelectedPiece()      { return selectedPiece; }
    public boolean isAwaitingMove()      { return awaitingMoveTarget; }
    public List<String> getMoveLog()     { return moveLog; }
    public int getTurnNumber()           { return turnNumber; }
    public AIPlayer getAIOpponent()      { return aiOpponent; }

    //getters for the last move highlight data used by BoardPanel
    public int getLastMoveFromRow()      { return lastMoveFromRow; }
    public int getLastMoveFromCol()      { return lastMoveFromCol; }
    public int getLastMoveToRow()        { return lastMoveToRow; }
    public int getLastMoveToCol()        { return lastMoveToCol; }
    public boolean wasLastMoveRotate()   { return lastMoveWasRotate; }

    public void setScreen(GameScreen s)  { this.screen = s; }
}
