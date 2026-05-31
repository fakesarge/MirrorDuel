package mirrorduel;

//this file holds all the shared enums used across the game
//enums are just a fixed list of named values so instead of using raw numbers like 0 1 2 3
//you use named constants like UP DOWN LEFT RIGHT which makes the code much easier to read

public class GameEnums {

    //the four directions a laser beam can travel
    public enum Direction {
        UP, DOWN, LEFT, RIGHT;

        //when a beam moving in this direction hits a slash mirror (/)
        //these rules follow real optics - a slash tilted at 45 degrees
        //for example a beam going RIGHT hits a / and bounces UP
        public Direction reflectSlash() {
            return switch (this) {
                case UP    -> RIGHT;
                case DOWN  -> LEFT;
                case LEFT  -> DOWN;
                case RIGHT -> UP;
            };
        }

        //when a beam hits a backslash mirror (\)
        //opposite diagonal so the reflections are mirrored compared to slash
        //for example a beam going RIGHT hits a \ and bounces DOWN
        public Direction reflectBackslash() {
            return switch (this) {
                case UP    -> LEFT;
                case DOWN  -> RIGHT;
                case LEFT  -> UP;
                case RIGHT -> DOWN;
            };
        }

        //how many columns to move each step - negative means left positive means right
        public int dx() {
            return switch (this) {
                case LEFT  -> -1;
                case RIGHT ->  1;
                default    ->  0;
            };
        }

        //how many rows to move each step - negative means up positive means down
        //on screen row 0 is the top so going UP means row number decreases
        public int dy() {
            return switch (this) {
                case UP    -> -1;
                case DOWN  ->  1;
                default    ->  0;
            };
        }
    }

    //the four types of game pieces each player has on the board
    public enum PieceType {
        EMITTER,          //fires the laser beam
        CRYSTAL,          //the target piece - if hit the owner loses
        MIRROR_SLASH,     //the / shaped mirror
        MIRROR_BACKSLASH  //the \ shaped mirror
    }

    //the two players
    public enum PlayerID {
        RED, BLUE;

        //returns the other player - useful so you don't have to write if RED then BLUE else RED everywhere
        public PlayerID opponent() {
            return this == RED ? BLUE : RED;
        }
    }

    //which screen the game is currently showing
    public enum GameScreen {
        MAIN_MENU,  //the start screen with the three play buttons
        RULES,      //the how to play screen
        PLAYING,    //the actual game is active
        VICTORY     //someone won and the victory screen is showing
    }
}
