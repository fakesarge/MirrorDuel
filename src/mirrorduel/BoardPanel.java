package mirrorduel;

import mirrorduel.GameEnums.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;

//BoardPanel is the visual game board drawn inside the game screen
//it handles mouse clicks and draws everything: the grid tiles laser beams pieces highlights
//all drawing is done by overriding paintComponent which java swing calls whenever the board needs to refresh
public class BoardPanel extends JPanel {
    //size of each grid cell in pixels - the full board is 8 tiles wide and 8 tiles tall
    private static final int TILE = 72;
    private static final int BOARD_PX = TILE * Board.SIZE;

    //colors for the checkerboard tiles
    private static final Color TILE_LIGHT    = new Color(220, 228, 240);
    private static final Color TILE_DARK     = new Color(168, 185, 210);
    //laser beam colors with some transparency so pieces show through slightly
    private static final Color LASER_RED     = new Color(255, 60, 60, 200);
    private static final Color LASER_BLUE    = new Color(60, 160, 255, 200);
    //yellow highlight for the currently selected piece
    private static final Color SELECT_COLOR  = new Color(255, 220, 0, 180);
    //green hint squares showing where the selected piece can legally move
    private static final Color VALID_MOVE    = new Color(100, 255, 130, 120);
    //board outline border color
    private static final Color BOARD_BORDER  = new Color(40, 55, 80);
    //yellow fill for last-move highlight squares
    private static final Color LAST_MOVE_FILL   = new Color(255, 215, 0, 120);
    //darker yellow border around last-move highlight squares
    private static final Color LAST_MOVE_BORDER = new Color(200, 165, 0, 220);

    private final GameManager gm;

    public BoardPanel(GameManager gm) {
        this.gm = gm;
        setPreferredSize(new Dimension(BOARD_PX, BOARD_PX));
        setBackground(new Color(18, 22, 38));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                //convert pixel position of the click into grid row and column
                int col = e.getX() / TILE;
                int row = e.getY() / TILE;
                if (col >= 0 && col < Board.SIZE && row >= 0 && row < Board.SIZE) {
                    boolean changed = gm.handleCellClick(row, col);
                    if (changed) {
                        repaint();
                        //notify the parent GameWindow so it can update the sidebar text
                        firePropertyChange("gameStateChanged", false, true);
                    }
                }
            }
        });
    }

    //java swing calls this method every time the board needs to be redrawn
    //we draw layers in order so each layer appears on top of the previous one
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawGrid(g);
        drawLastMoveHighlight(g);   //yellow squares show which cells were just involved in the last move
        drawValidMoveHints(g);
        drawLasers(g);
        drawPieces(g);
        drawSelectionHighlight(g);
        drawBoardBorder(g);
    }

    //fills the board with alternating light and dark tiles like a chess board
    private void drawGrid(Graphics2D g) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                g.setColor((r + c) % 2 == 0 ? TILE_LIGHT : TILE_DARK);
                g.fillRect(c * TILE, r * TILE, TILE, TILE);
            }
        }
    }

    //highlights the squares involved in the last move in yellow
    //for a movement: highlights both the square the piece came from and where it landed
    //for a rotation: highlights just the one square where the mirror sits
    private void drawLastMoveHighlight(Graphics2D g) {
        //a value of -1 means no move has been made yet so nothing to highlight
        if (gm.getLastMoveToRow() == -1) return;

        //always highlight the destination square (or the rotated mirror's square)
        highlightSquare(g, gm.getLastMoveToRow(), gm.getLastMoveToCol());

        //for movement actions also highlight the origin square the piece moved away from
        if (!gm.wasLastMoveRotate()) {
            highlightSquare(g, gm.getLastMoveFromRow(), gm.getLastMoveFromCol());
        }
    }

    //draws a single yellow rounded rectangle over a grid cell to indicate it was part of the last move
    private void highlightSquare(Graphics2D g, int row, int col) {
        g.setColor(LAST_MOVE_FILL);
        g.fillRoundRect(col * TILE + 3, row * TILE + 3, TILE - 6, TILE - 6, 10, 10);
        g.setColor(LAST_MOVE_BORDER);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(col * TILE + 3, row * TILE + 3, TILE - 6, TILE - 6, 10, 10);
    }

    //shows small green rounded rectangles on cells where the selected piece can legally move
    private void drawValidMoveHints(Graphics2D g) {
        if (!gm.isAwaitingMove() || gm.getSelectedPiece() == null) return;
        Piece sel = gm.getSelectedPiece();
        int[] drs = {-1, 1, 0, 0};
        int[] dcs = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nr = sel.row + drs[i];
            int nc = sel.col + dcs[i];
            if (nr >= 0 && nr < Board.SIZE && nc >= 0 && nc < Board.SIZE
                    && gm.getBoard().pieceAt(nr, nc) == null) {
                g.setColor(VALID_MOVE);
                g.fillRoundRect(nc * TILE + 6, nr * TILE + 6, TILE - 12, TILE - 12, 12, 12);
                g.setColor(new Color(80, 220, 100, 200));
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(nc * TILE + 6, nr * TILE + 6, TILE - 12, TILE - 12, 12, 12);
            }
        }
    }

    //draws both players' laser beams on the board
    private void drawLasers(Graphics2D g) {
        if (gm.getBoard() == null) return;
        drawBeam(g, gm.getBoard().getBeam(PlayerID.RED),  LASER_RED,  PlayerID.RED);
        drawBeam(g, gm.getBoard().getBeam(PlayerID.BLUE), LASER_BLUE, PlayerID.BLUE);
    }

    //draws one player's laser beam as three overlapping lines to create a glowing effect
    //the widest line is a faint glow, the middle line is the main colored beam,
    //the thinnest line is a bright white core to make it look like real light
    private void drawBeam(Graphics2D g, List<java.awt.Point> beam, Color color, PlayerID player) {
        if (beam == null || beam.isEmpty()) return;

        //find the emitter so we know the starting pixel position of the beam
        Piece emitter = null;
        for (Piece p : gm.getBoard().piecesOf(player))
            if (p.type == PieceType.EMITTER) { emitter = p; break; }
        if (emitter == null) return;

        int startX = emitter.col * TILE + TILE / 2;
        int startY = emitter.row * TILE + TILE / 2;

        //wide transparent outer glow
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawBeamPath(g, beam, startX, startY);

        //main colored beam line
        g.setColor(color);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawBeamPath(g, beam, startX, startY);

        //bright white core line on top
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawBeamPath(g, beam, startX, startY);
    }

    //builds the actual line path from the emitter center through each beam cell center
    //and draws it using the current stroke and color already set on the graphics object
    private void drawBeamPath(Graphics2D g, List<java.awt.Point> beam, int startX, int startY) {
        GeneralPath path = new GeneralPath();
        path.moveTo(startX, startY);
        for (java.awt.Point pt : beam) {
            int cx = pt.x * TILE + TILE / 2;
            int cy = pt.y * TILE + TILE / 2;
            path.lineTo(cx, cy);
        }
        g.draw(path);
    }

    //iterates over the entire grid and draws each piece it finds
    private void drawPieces(Graphics2D g) {
        if (gm.getBoard() == null) return;
        int padding = 6;
        int size = TILE - padding * 2;

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = gm.getBoard().pieceAt(r, c);
                if (p == null) continue;
                int px = c * TILE + padding;
                int py = r * TILE + padding;
                drawPiece(g, p, px, py, size);
            }
        }
    }

    //draws a single piece at the given pixel position
    //tries to load a PNG image for the piece and falls back to a drawn shape if no image exists
    private void drawPiece(Graphics2D g, Piece p, int x, int y, int size) {
        boolean isRed  = p.owner == PlayerID.RED;
        String imgName = null;

        switch (p.type) {
            case EMITTER -> imgName = isRed ? "emitter_red.png" : "emitter_blue.png";
            case CRYSTAL -> imgName = isRed ? "crystal_red.png" : "crystal_blue.png";
            case MIRROR_SLASH, MIRROR_BACKSLASH -> imgName = isRed ? "mirror_red.png" : "mirror_blue.png";
        }

        BufferedImage img = imgName != null ? AssetLoader.scaled(imgName, size, size) : null;

        if (img != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (p.type == PieceType.MIRROR_BACKSLASH) {
                //flip the slash mirror image horizontally to get the backslash orientation
                g2.translate(x + size, y);
                g2.scale(-1, 1);
                g2.drawImage(img, 0, 0, size, size, null);
            } else {
                g2.drawImage(img, x, y, size, size, null);
            }
            g2.dispose();
        } else {
            drawFallbackPiece(g, p, x, y, size);
        }

        //draw the / or \ symbol on mirrors so the player can always tell the orientation
        if (p.isMirror()) {
            g.setColor(new Color(255, 255, 255, 200));
            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            String sym = p.type == PieceType.MIRROR_SLASH ? "/" : "\\";
            int tx = x + (size - fm.stringWidth(sym)) / 2;
            int ty = y + (size + fm.getAscent()) / 2 - 4;
            g.drawString(sym, tx, ty);
        }
    }

    //draws a piece using simple geometric shapes when no image file is available
    //emitters are circles with a dark pupil, crystals are circles with a crown polygon,
    //mirrors are rotated ellipses to visually suggest the diagonal angle
    private void drawFallbackPiece(Graphics2D g, Piece p, int x, int y, int size) {
        boolean isRed = p.owner == PlayerID.RED;
        Color base   = isRed ? new Color(220, 60, 60) : new Color(60, 140, 220);
        Color accent = isRed ? new Color(255, 120, 120) : new Color(120, 190, 255);

        switch (p.type) {
            case EMITTER -> {
                g.setColor(base);
                g.fillOval(x, y, size, size);
                g.setColor(accent);
                g.setStroke(new BasicStroke(3f));
                g.drawOval(x + 4, y + 4, size - 8, size - 8);
                g.setColor(new Color(0, 0, 0, 180));
                g.fillOval(x + size/3, y + size/3, size/3, size/3);
            }
            case CRYSTAL -> {
                g.setColor(base);
                g.fillOval(x, y, size, size);
                g.setColor(accent);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(x + 4, y + 4, size - 8, size - 8);
                int cx = x + size / 2, cy = y + size / 2;
                int[] crown_x = {cx-12, cx-12, cx-6, cx, cx+6, cx+12, cx+12};
                int[] crown_y = {cy+6,  cy-4,  cy+2, cy-8, cy+2, cy-4,  cy+6};
                g.setColor(accent);
                g.fillPolygon(crown_x, crown_y, 7);
            }
            case MIRROR_SLASH, MIRROR_BACKSLASH -> {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(base);
                g2.translate(x + size / 2, y + size / 2);
                double angle = p.type == PieceType.MIRROR_SLASH ? -35.0 : 35.0;
                g2.rotate(Math.toRadians(angle));
                g2.fillOval(-size / 2, -size / 6, size, size / 3);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(-size / 2, -size / 6, size, size / 3);
                g2.dispose();
            }
        }
    }

    //draws a bright yellow rounded rectangle outline around the currently selected piece
    //also fills it with a faint yellow glow to make the selection obvious
    private void drawSelectionHighlight(Graphics2D g) {
        Piece sel = gm.getSelectedPiece();
        if (sel == null) return;
        g.setColor(SELECT_COLOR);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(sel.col * TILE + 3, sel.row * TILE + 3, TILE - 6, TILE - 6, 10, 10);
        g.setColor(new Color(255, 220, 0, 50));
        g.fillRoundRect(sel.col * TILE + 3, sel.row * TILE + 3, TILE - 6, TILE - 6, 10, 10);
    }

    //draws a thin border around the entire board so it has a clean edge
    private void drawBoardBorder(Graphics2D g) {
        g.setColor(BOARD_BORDER);
        g.setStroke(new BasicStroke(4f));
        g.drawRect(0, 0, BOARD_PX - 1, BOARD_PX - 1);
    }

    public int getTileSize() { return TILE; }
}
