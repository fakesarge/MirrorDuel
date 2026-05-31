package mirrorduel;

import mirrorduel.GameEnums.*;
import java.awt.Point;
import java.util.*;

//the AI opponent that plays as RED
//it uses a strategy called minimax with alpha-beta pruning
//minimax works by imagining all the possible moves several turns ahead
//for each possible game state it scores how good the position is
//then it picks the move that leads to the best outcome assuming the opponent also plays well
//alpha-beta pruning is an optimization that skips branches we already know are worse than
//something we have already found so the AI can think further ahead in the same time
//hard mode: searches 5 turns deep which is strong enough to beat expert human players
//easy mode: same engine but 35 percent of the time picks a completely random move
public class AIPlayer {

    public enum Difficulty { EASY, HARD }

    //represents one possible action the AI or move generator can make
    public static class Move {
        public enum Type { MOVE_PIECE, ROTATE }
        public final Type type;
        public final int pieceRow, pieceCol;   //where the piece is before the action
        public final int targetRow, targetCol; //destination cell for movement actions

        //constructor for a movement action
        public Move(int pr, int pc, int tr, int tc) {
            this.type = Type.MOVE_PIECE;
            pieceRow = pr; pieceCol = pc;
            targetRow = tr; targetCol = tc;
        }

        //constructor for a rotation action - no target cell needed
        public Move(int pr, int pc) {
            this.type = Type.ROTATE;
            pieceRow = pr; pieceCol = pc;
            targetRow = -1; targetCol = -1;
        }
    }

    //how many turns ahead the AI considers in hard mode
    private static final int    HARD_DEPTH         = 5;
    //probability of playing a random move in easy mode (35 percent)
    private static final double EASY_RANDOM_CHANCE = 0.35;
    private static final Random RNG                = new Random();

    //which player this AI controls - always RED
    private final PlayerID   aiPlayer;
    private final Difficulty difficulty;

    public AIPlayer(PlayerID player, Difficulty difficulty) {
        this.aiPlayer   = player;
        this.difficulty = difficulty;
    }

    //the main entry point - given the current board state returns the best move to make
    public Move getBestMove(Board board) {
        //in easy mode randomly skip the full search 35 percent of the time
        if (difficulty == Difficulty.EASY && RNG.nextDouble() < EASY_RANDOM_CHANCE) {
            return getRandomMove(board);
        }
        return minimaxRoot(board, HARD_DEPTH);
    }

    //picks a random legal move but still grabs an instant win if one is available
    //even the easy AI will not throw away a free victory
    private Move getRandomMove(Board board) {
        List<Move> moves = generateMoves(board, aiPlayer);
        if (moves.isEmpty()) return null;
        for (Move m : moves) {
            Board sim = applyMove(board, m, aiPlayer);
            if (sim.checkWinner() == aiPlayer) return m;
        }
        return moves.get(RNG.nextInt(moves.size()));
    }

    //the top level of the minimax search
    //checks for an immediate winning move before doing the full depth search
    //then evaluates all moves and returns the one with the highest score
    private Move minimaxRoot(Board board, int depth) {
        List<Move> moves = generateMoves(board, aiPlayer);
        if (moves.isEmpty()) return null;

        //always take a free win immediately without spending search time on it
        for (Move m : moves) {
            Board sim = applyMove(board, m, aiPlayer);
            if (sim.checkWinner() == aiPlayer) return m;
        }

        Move best      = null;
        int  bestScore = Integer.MIN_VALUE;

        for (Move m : moves) {
            Board sim   = applyMove(board, m, aiPlayer);
            //after the AI makes a move it is the opponent's turn so maximizing is false
            int   score = minimax(sim, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            if (score > bestScore) {
                bestScore = score;
                best      = m;
            }
        }
        return best;
    }

    //the recursive minimax function
    //maximizing=true means it is the AI's turn and we want the highest score
    //maximizing=false means it is the opponent's turn and we want the lowest score
    //alpha is the best score the AI has found so far beta is the best the opponent has found
    //if beta is less than or equal to alpha we can stop looking at this branch entirely
    //because the opponent would never allow us to reach this line - this is alpha-beta pruning
    private int minimax(Board board, int depth, int alpha, int beta, boolean maximizing) {
        PlayerID winner = board.checkWinner();
        //adding depth to win scores makes the AI prefer winning sooner rather than later
        if (winner == aiPlayer)            return  100_000 + depth;
        if (winner == aiPlayer.opponent()) return -100_000 - depth;
        //when we reach the depth limit use the heuristic to estimate who is winning
        if (depth == 0)                    return evaluate(board);

        PlayerID current = maximizing ? aiPlayer : aiPlayer.opponent();
        List<Move> moves = generateMoves(board, current);

        if (maximizing) {
            int max = Integer.MIN_VALUE;
            for (Move m : moves) {
                int score = minimax(applyMove(board, m, current), depth - 1, alpha, beta, false);
                if (score > max)   max   = score;
                if (score > alpha) alpha = score;
                //the opponent would never let us reach this state so stop searching this branch
                if (beta <= alpha) break;
            }
            return max;
        } else {
            int min = Integer.MAX_VALUE;
            for (Move m : moves) {
                int score = minimax(applyMove(board, m, current), depth - 1, alpha, beta, true);
                if (score < min)  min  = score;
                if (score < beta) beta = score;
                //we already found a better option for the AI elsewhere so stop this branch
                if (beta <= alpha) break;
            }
            return min;
        }
    }

    //estimates how good the current board position is for the AI
    //positive score means the AI is winning negative means the opponent is winning
    //the function considers beam length beam endpoint distance to crystals and mirror usage
    private int evaluate(Board board) {
        PlayerID opp = aiPlayer.opponent();

        List<Point> aiBeam  = board.getBeam(aiPlayer);
        List<Point> oppBeam = board.getBeam(opp);

        Piece oppCrystal = findCrystal(board, opp);
        Piece ownCrystal = findCrystal(board, aiPlayer);
        if (oppCrystal == null || ownCrystal == null) return 0;

        int score = 0;

        //longer beams generally mean more pressure so reward raw beam length
        score += aiBeam.size()  * 8;
        score -= oppBeam.size() * 8;

        //reward the AI beam endpoint being close to the enemy crystal
        //Manhattan distance is just the row difference plus the column difference
        if (!aiBeam.isEmpty()) {
            Point last = aiBeam.get(aiBeam.size() - 1);
            int dist = Math.abs(last.y - oppCrystal.row) + Math.abs(last.x - oppCrystal.col);
            score += (14 - dist) * 22;
            //extra reward for any beam cell that is very close to the enemy crystal
            for (Point pt : aiBeam) {
                if (Math.abs(pt.y - oppCrystal.row) + Math.abs(pt.x - oppCrystal.col) <= 2)
                    score += 12;
            }
        }

        //punish the opponent's beam endpoint being close to the AI's own crystal
        if (!oppBeam.isEmpty()) {
            Point last = oppBeam.get(oppBeam.size() - 1);
            int dist = Math.abs(last.y - ownCrystal.row) + Math.abs(last.x - ownCrystal.col);
            score -= (14 - dist) * 22;
            for (Point pt : oppBeam) {
                if (Math.abs(pt.y - ownCrystal.row) + Math.abs(pt.x - ownCrystal.col) <= 2)
                    score -= 12;
            }
        }

        //reward mirrors that are actively deflecting the AI beam
        //penalize own mirrors deflecting the enemy beam
        for (Point pt : aiBeam) {
            Piece pc = board.pieceAt(pt.y, pt.x);
            if (pc != null && pc.isMirror())
                score += pc.owner == aiPlayer ? 5 : -3;
        }
        for (Point pt : oppBeam) {
            Piece pc = board.pieceAt(pt.y, pt.x);
            if (pc != null && pc.isMirror())
                score -= pc.owner == opp ? 5 : -3;
        }

        return score;
    }

    //generates every legal move a player can make from the current board state
    //for each piece: try moving in all four orthogonal directions
    //for each mirror: also add a rotation move
    //crystals cannot be moved as per the rules
    private List<Move> generateMoves(Board board, PlayerID player) {
        List<Move> moves = new ArrayList<>();
        int[] drs = {-1, 1,  0, 0};
        int[] dcs = { 0, 0, -1, 1};

        for (Piece piece : board.piecesOf(player)) {
            if (piece.type == PieceType.CRYSTAL) continue;

            for (int i = 0; i < 4; i++) {
                int nr = piece.row + drs[i];
                int nc = piece.col + dcs[i];
                if (board.canMovePieceTo(piece, nr, nc))
                    moves.add(new Move(piece.row, piece.col, nr, nc));
            }

            if (piece.isMirror())
                moves.add(new Move(piece.row, piece.col));
        }
        return moves;
    }

    //creates a copy of the board with one move applied to it
    //used during the minimax search so we simulate without altering the real board
    private Board applyMove(Board board, Move move, PlayerID player) {
        Board copy  = board.copy();
        Piece piece = copy.pieceAt(move.pieceRow, move.pieceCol);
        if (piece == null) return copy;

        if (move.type == Move.Type.ROTATE) {
            copy.rotateMirror(piece);
        } else {
            copy.movePiece(piece, move.targetRow, move.targetCol);
        }
        return copy;
    }

    //finds and returns a player's crystal piece from the board
    private Piece findCrystal(Board board, PlayerID player) {
        for (Piece p : board.piecesOf(player))
            if (p.type == PieceType.CRYSTAL) return p;
        return null;
    }
}
