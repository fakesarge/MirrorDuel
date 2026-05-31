package mirrorduel;

import mirrorduel.GameEnums.*;

//a single game piece on the board
//pieces are mutable - their row col and type fields change during the game
//when a piece moves its row and col are updated directly on this object
public class Piece {
    public PieceType type;   //what kind of piece this is
    public PlayerID owner;   //which player this piece belongs to
    public int row, col;     //current position on the 8x8 grid

    public Piece(PieceType type, PlayerID owner, int row, int col) {
        this.type  = type;
        this.owner = owner;
        this.row   = row;
        this.col   = col;
    }

    //returns true if this piece is one of the two mirror types
    //used in many places to decide if rotation is allowed or if the beam should bounce
    public boolean isMirror() {
        return type == PieceType.MIRROR_SLASH || type == PieceType.MIRROR_BACKSLASH;
    }

    //toggles this mirror between slash and backslash
    //called when the player uses their rotate action on their turn
    public void rotateMirror() {
        if (type == PieceType.MIRROR_SLASH)          type = PieceType.MIRROR_BACKSLASH;
        else if (type == PieceType.MIRROR_BACKSLASH) type = PieceType.MIRROR_SLASH;
    }

    //given the direction a beam is traveling when it enters this piece
    //returns the new direction after this piece affects the beam
    //non-mirror pieces like emitters and crystals do not change the beam direction
    public Direction reflectBeam(Direction incoming) {
        if (type == PieceType.MIRROR_SLASH)      return incoming.reflectSlash();
        if (type == PieceType.MIRROR_BACKSLASH)  return incoming.reflectBackslash();
        return incoming;
    }

    @Override
    public String toString() {
        return owner + " " + type + " @(" + row + "," + col + ")";
    }
}
