package mirrorduel;

import mirrorduel.GameEnums.*;
import java.awt.Point;
import java.util.*;

//the board holds the 8x8 grid of pieces and the live laser beam paths
//it is the source of truth for the game state at any moment
//the AI makes copies of this board to simulate future moves without affecting the real game
public class Board {
    public static final int SIZE = 8;

    //the grid stores pieces by position - grid[row][col] is null if that cell is empty
    private final Piece[][] grid = new Piece[SIZE][SIZE];

    //the laser beam path for each player stored as a list of cells the beam passes through
    //each Point uses x for column and y for row
    private final List<Point> redBeam  = new ArrayList<>();
    private final List<Point> blueBeam = new ArrayList<>();

    //whether each player's laser has reached and hit the enemy crystal
    private boolean redHitsTarget  = false;
    private boolean blueHitsTarget = false;

    public Board() {
        setupInitial();
    }

    //places all pieces in their starting positions and runs the first laser calculation
    private void setupInitial() {
        //RED player: emitter top-left corner crystal top-right corner mirrors in the top half
        place(new Piece(PieceType.EMITTER, PlayerID.RED,  0, 0));
        place(new Piece(PieceType.CRYSTAL, PlayerID.RED,  0, 7));

        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.RED, 1, 2));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.RED, 2, 5));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.RED, 3, 1));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.RED, 1, 6));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.RED, 3, 4));

        //BLUE player: emitter bottom-right crystal bottom-left mirrors in the bottom half
        place(new Piece(PieceType.EMITTER, PlayerID.BLUE, 7, 7));
        place(new Piece(PieceType.CRYSTAL, PlayerID.BLUE, 7, 0));

        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.BLUE, 6, 5));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.BLUE, 5, 2));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.BLUE, 4, 6));
        place(new Piece(PieceType.MIRROR_SLASH,      PlayerID.BLUE, 6, 1));
        place(new Piece(PieceType.MIRROR_BACKSLASH,  PlayerID.BLUE, 4, 3));

        recalculateLasers();
    }

    //returns the piece at a given cell or null if the cell is empty or out of bounds
    public Piece pieceAt(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return null;
        return grid[row][col];
    }

    //returns the laser beam path for a player as a list of (col x row y) points
    public List<Point> getBeam(PlayerID p) {
        return p == PlayerID.RED ? redBeam : blueBeam;
    }

    //returns true if that player's laser beam is currently hitting the enemy crystal
    public boolean laserHitsTarget(PlayerID p) {
        return p == PlayerID.RED ? redHitsTarget : blueHitsTarget;
    }

    //collects all pieces belonging to a given player into a list
    public List<Piece> piecesOf(PlayerID p) {
        List<Piece> list = new ArrayList<>();
        for (Piece[] row : grid)
            for (Piece pc : row)
                if (pc != null && pc.owner == p) list.add(pc);
        return list;
    }

    //puts a piece into the grid at the piece's current row and col
    private void place(Piece p) {
        grid[p.row][p.col] = p;
    }

    //removes a piece from the grid at its current position (sets that cell to null)
    private void remove(Piece p) {
        grid[p.row][p.col] = null;
    }

    //checks whether a piece can legally move to a target cell
    //rules: the target must be on the board, exactly one step away (no diagonal), and empty
    public boolean canMovePieceTo(Piece p, int newRow, int newCol) {
        if (newRow < 0 || newRow >= SIZE || newCol < 0 || newCol >= SIZE) return false;
        int dr = Math.abs(newRow - p.row);
        int dc = Math.abs(newCol - p.col);
        if (!((dr == 1 && dc == 0) || (dr == 0 && dc == 1))) return false;
        return grid[newRow][newCol] == null;
    }

    //moves a piece to a new cell if the move is legal
    //updates the piece object's row and col then recalculates both laser beams
    //returns true if the move was applied false if it was illegal
    public boolean movePiece(Piece p, int newRow, int newCol) {
        if (!canMovePieceTo(p, newRow, newCol)) return false;
        remove(p);
        p.row = newRow;
        p.col = newCol;
        place(p);
        recalculateLasers();
        return true;
    }

    //flips a mirror piece between slash and backslash then recalculates lasers
    //returns false if the piece is not a mirror
    public boolean rotateMirror(Piece p) {
        if (!p.isMirror()) return false;
        p.rotateMirror();
        recalculateLasers();
        return true;
    }

    //rebuilds both laser beam paths from scratch
    //called after every move or rotation so the displayed beams are always up to date
    public void recalculateLasers() {
        traceBeam(PlayerID.RED,  redBeam,  false);
        traceBeam(PlayerID.BLUE, blueBeam, false);
        redHitsTarget  = checkHitsTarget(PlayerID.RED,  redBeam);
        blueHitsTarget = checkHitsTarget(PlayerID.BLUE, blueBeam);
    }

    //traces the laser beam for one player step by step across the board
    //the beam starts at the emitter and travels one cell at a time in its current direction
    //when it hits a mirror it changes direction and keeps going
    //when it hits any other piece or the board edge it stops
    //a visited set prevents the beam from looping forever in a mirror cycle
    private void traceBeam(PlayerID shooter, List<Point> beam, boolean dummy) {
        beam.clear();

        //find this player's emitter piece
        Piece emitter = null;
        for (Piece[] row : grid)
            for (Piece pc : row)
                if (pc != null && pc.owner == shooter && pc.type == PieceType.EMITTER) {
                    emitter = pc; break;
                }
        if (emitter == null) return;

        //RED emitter fires to the RIGHT, BLUE emitter fires to the LEFT
        Direction dir = (shooter == PlayerID.RED) ? Direction.RIGHT : Direction.LEFT;

        int row = emitter.row;
        int col = emitter.col;

        //visited tracks every (row col direction) combination we have already passed through
        //if we arrive at the same cell going the same direction again we are in an infinite loop
        Set<String> visited = new HashSet<>();

        while (true) {
            //advance one step in the current direction
            row += dir.dy();
            col += dir.dx();

            //beam has left the board - stop
            if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) break;

            //loop detection - same cell and direction seen before means we would repeat forever
            String key = row + "," + col + "," + dir;
            if (visited.contains(key)) break;
            visited.add(key);

            //record this cell as part of the beam path
            //Point stores x as column and y as row
            beam.add(new Point(col, row));

            Piece pc = grid[row][col];
            if (pc == null) continue;

            if (pc.isMirror()) {
                //bounce off the mirror and continue in the new direction
                dir = pc.reflectBeam(dir);
            } else {
                //hit an emitter crystal or other solid piece - beam stops here
                break;
            }
        }
    }

    //checks if the last cell of a beam contains the enemy crystal
    //if it does that means the shooter has won
    private boolean checkHitsTarget(PlayerID shooter, List<Point> beam) {
        PlayerID enemy = shooter.opponent();
        if (beam.isEmpty()) return false;
        Point last = beam.get(beam.size() - 1);
        Piece pc = grid[last.y][last.x];
        return pc != null && pc.type == PieceType.CRYSTAL && pc.owner == enemy;
    }

    //returns the winning player if either laser is hitting an enemy crystal right now
    //returns null if nobody has won yet
    public PlayerID checkWinner() {
        if (redHitsTarget)  return PlayerID.RED;
        if (blueHitsTarget) return PlayerID.BLUE;
        return null;
    }

    //creates a completely independent copy of this board
    //used by the AI to simulate hypothetical moves without changing the real game board
    //all pieces are newly created objects so modifying the copy does not affect the original
    public Board copy() {
        Board b = new Board();
        //wipe the default setup that the Board constructor just placed
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                b.grid[r][c] = null;
        //copy every piece from the original into the new board
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] != null) {
                    Piece p = grid[r][c];
                    b.grid[r][c] = new Piece(p.type, p.owner, p.row, p.col);
                }
        b.recalculateLasers();
        return b;
    }
}
