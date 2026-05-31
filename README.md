# Mirror Duel — Java Strategy Game

A turn-based 2-player laser reflection strategy game built with Java Swing.

## Requirements
- Java JDK 17 or later (download from https://adoptium.net)

## How to Build & Run

### Windows
1. Double-click `build.bat`  
   — OR —  
   Open a terminal in this folder and run:
   ```
   build.bat
   ```

### Mac / Linux
```bash
chmod +x build.sh
./build.sh
```

This compiles all sources, packages a `MirrorDuel.jar`, and launches the game.

## Running the pre-built JAR (after first build)
```bash
java -jar MirrorDuel.jar
```

## Project Structure
```
MirrorDuel/
├── src/mirrorduel/
│   ├── Main.java          — entry point
│   ├── GameEnums.java     — Direction, PieceType, PlayerID, GameScreen
│   ├── Piece.java         — game piece model
│   ├── Board.java         — 8×8 grid + laser tracing
│   ├── GameManager.java   — turn logic, action handling
│   ├── AssetLoader.java   — image loading/caching
│   ├── BoardPanel.java    — board renderer + mouse input
│   └── GameWindow.java    — main JFrame, all screens
├── assets/                — game piece images
├── MANIFEST.MF
├── build.bat              — Windows build script
├── build.sh               — Mac/Linux build script
└── README.md
```

## Game Rules (Quick Reference)
- **Objective:** Direct your laser to hit the enemy's Core Crystal
- **Each turn:** Do exactly ONE action — move a piece OR rotate a mirror
- **Slash mirror /:** Left→Up, Right→Down, Up→Right, Down→Left
- **Backslash mirror \\:** Left→Down, Right→Up, Up→Left, Down→Right
- **Win:** Laser hits enemy Crystal instantly ends the game
