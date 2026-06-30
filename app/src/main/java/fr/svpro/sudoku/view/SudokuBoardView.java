package fr.svpro.sudoku.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import fr.svpro.sudoku.model.SnakePath;
import fr.svpro.sudoku.model.SudokuBoard;

/**
 * Vue Canvas pour la grille Sudoku.
 *
 * Mode Serpentart :
 *   - Cases diagonales : fond bleu lavande (#DDE8F8)
 *   - Cases serpent    : fond vert sauge (#D4EDD4) + contour vert
 *   - Case tête serpent: fond vert plus soutenu (#A8D5A2)
 *   - Flèches de direction entre les cases du serpent
 *   - Lignes diagonales en tirets indigo sur toute la grille
 */
public class SudokuBoardView extends View {

    // ── Couleurs ──────────────────────────────────────────────────────────────
    private static final int C_BG            = Color.parseColor("#FAFAFA");
    private static final int C_NORMAL        = Color.parseColor("#FFFFFF");
    private static final int C_SELECTED      = Color.parseColor("#C8E6FA");
    private static final int C_RELATED       = Color.parseColor("#E8F4FC");
    private static final int C_SAME_NUM      = Color.parseColor("#B3D9F5");
    private static final int C_ERROR         = Color.parseColor("#FFCDD2");
    private static final int C_FIXED         = Color.parseColor("#F5F5F5");
    private static final int C_DIAGONAL      = Color.parseColor("#DDE8F8");
    private static final int C_SNAKE         = Color.parseColor("#D4EDD4");
    private static final int C_SNAKE_HEAD    = Color.parseColor("#A8D5A2");
    private static final int C_SNAKE_OVERLAP = Color.parseColor("#C5DDF5"); // diag+serpent
    private static final int C_TEXT_FIXED    = Color.parseColor("#1A237E");
    private static final int C_TEXT_USER     = Color.parseColor("#1565C0");
    private static final int C_TEXT_ERROR    = Color.parseColor("#C62828");
    private static final int C_TEXT_HINT     = Color.parseColor("#2E7D32");
    private static final int C_LINE_THIN     = Color.parseColor("#BDBDBD");
    private static final int C_LINE_THICK    = Color.parseColor("#37474F");
    private static final int C_DIAG_LINE     = Color.parseColor("#7986CB");
    private static final int C_SNAKE_BORDER  = Color.parseColor("#4CAF50");
    private static final int C_SNAKE_ARROW   = Color.parseColor("#2E7D32");
    private static final int C_NOTES         = Color.parseColor("#78909C");

    // ── Paints ────────────────────────────────────────────────────────────────
    private final Paint pBg          = new Paint();
    private final Paint pCell        = new Paint();
    private final Paint pThin        = new Paint();
    private final Paint pThick       = new Paint();
    private final Paint pDiagLine    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSnakeBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSnakeArrow  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFixed       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pUser        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pError       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHint        = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pNotes       = new Paint(Paint.ANTI_ALIAS_FLAG);

    private SudokuBoard       board;
    private CellTouchListener touchListener;

    private float cellSize;
    private final RectF  cellRect   = new RectF();
    private final Rect   textBounds = new Rect();
    private final Path   arrowPath  = new Path();

    private final int[][] notes   = new int[SudokuBoard.SIZE][SudokuBoard.SIZE];
    private boolean notesMode     = false;

    public interface CellTouchListener {
        void onCellTouched(int row, int col);
    }

    public SudokuBoardView(Context ctx)                          { super(ctx);       init(); }
    public SudokuBoardView(Context ctx, AttributeSet a)          { super(ctx, a);    init(); }
    public SudokuBoardView(Context ctx, AttributeSet a, int s)   { super(ctx, a, s); init(); }

    private void init() {
        pBg.setColor(C_BG); pBg.setStyle(Paint.Style.FILL);
        pCell.setStyle(Paint.Style.FILL);
        pThin.setColor(C_LINE_THIN);  pThin.setStrokeWidth(1f);
        pThick.setColor(C_LINE_THICK); pThick.setStrokeWidth(3f);

        pDiagLine.setColor(C_DIAG_LINE);
        pDiagLine.setStyle(Paint.Style.STROKE);
        pDiagLine.setStrokeWidth(2f);
        pDiagLine.setPathEffect(new android.graphics.DashPathEffect(new float[]{8, 6}, 0));

        pSnakeBorder.setColor(C_SNAKE_BORDER);
        pSnakeBorder.setStyle(Paint.Style.STROKE);
        pSnakeBorder.setStrokeWidth(3f);

        pSnakeArrow.setColor(C_SNAKE_ARROW);
        pSnakeArrow.setStyle(Paint.Style.FILL);

        Typeface bold   = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        Typeface normal = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);

        pFixed.setTypeface(bold);   pFixed.setColor(C_TEXT_FIXED); pFixed.setTextAlign(Paint.Align.CENTER);
        pUser .setTypeface(normal); pUser .setColor(C_TEXT_USER);  pUser .setTextAlign(Paint.Align.CENTER);
        pError.setTypeface(normal); pError.setColor(C_TEXT_ERROR); pError.setTextAlign(Paint.Align.CENTER);
        pHint .setTypeface(bold);   pHint .setColor(C_TEXT_HINT);  pHint .setTextAlign(Paint.Align.CENTER);
        pNotes.setColor(C_NOTES);   pNotes.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int size = Math.min(MeasureSpec.getSize(ws), MeasureSpec.getSize(hs));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        cellSize = (float) Math.min(w, h) / SudokuBoard.SIZE;
        float ts = cellSize * 0.55f;
        pFixed.setTextSize(ts); pUser.setTextSize(ts);
        pError.setTextSize(ts); pHint.setTextSize(ts);
        pNotes.setTextSize(cellSize * 0.25f);
    }

    // ── Dessin principal ──────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), pBg);
        if (board == null) return;

        drawCellBackgrounds(canvas);
        drawNumbers(canvas);
        drawGrid(canvas);

        if (board.getVariant() == SudokuBoard.Variant.SERPENTART) {
            drawDiagonalLines(canvas);
            drawSnakeOverlay(canvas);
        }
    }

    // ── Fonds de cellules ─────────────────────────────────────────────────────

    private void drawCellBackgrounds(Canvas canvas) {
        boolean isSerpentart = board.getVariant() == SudokuBoard.Variant.SERPENTART;

        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                cellRect.set(c * cellSize, r * cellSize,
                             (c + 1) * cellSize, (r + 1) * cellSize);

                boolean onSnake    = isSerpentart && board.isOnSnake(r, c);
                boolean onDiag     = isSerpentart && board.isOnDiagonal(r, c);
                boolean isSelected = r == board.getSelectedRow() && c == board.getSelectedCol();

                int color;
                if (isSelected) {
                    color = C_SELECTED;
                } else if (board.isError(r, c)) {
                    color = C_ERROR;
                } else if (board.isSameValue(r, c)) {
                    color = C_SAME_NUM;
                } else if (board.isInSameSnake(r, c) || board.isInSameDiagonal(r, c)
                        || board.isInSameRowColBox(r, c)) {
                    color = C_RELATED;
                } else if (onSnake && onDiag) {
                    color = C_SNAKE_OVERLAP;
                } else if (onSnake) {
                    // Tête du serpent (position 0) plus soutenue
                    SnakePath snake = board.getSnake();
                    color = (snake != null && snake.positionOf(r, c) == 1)
                            ? C_SNAKE_HEAD : C_SNAKE;
                } else if (onDiag) {
                    color = C_DIAGONAL;
                } else if (board.isFixed(r, c)) {
                    color = C_FIXED;
                } else {
                    color = C_NORMAL;
                }
                pCell.setColor(color);
                canvas.drawRect(cellRect, pCell);
            }
        }
    }

    // ── Chiffres & notes ──────────────────────────────────────────────────────

    private void drawNumbers(Canvas canvas) {
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                int val = board.getValue(r, c);
                float cx = c * cellSize + cellSize / 2f;
                float cy = r * cellSize + cellSize / 2f;
                if (val != SudokuBoard.EMPTY) {
                    String txt = String.valueOf(val);
                    Paint p = board.isError(r, c)  ? pError
                            : board.isFixed(r, c)  ? pFixed
                            : (board.getSolution(r, c) == val) ? pHint
                            : pUser;
                    p.getTextBounds(txt, 0, txt.length(), textBounds);
                    canvas.drawText(txt, cx, cy - textBounds.exactCenterY(), p);
                } else {
                    drawNotes(canvas, r, c);
                }
            }
        }
    }

    private void drawNotes(Canvas canvas, int row, int col) {
        int mask = notes[row][col];
        if (mask == 0) return;
        float ns = cellSize / 3f;
        for (int n = 1; n <= 9; n++) {
            if ((mask & (1 << n)) != 0) {
                float nx = col * cellSize + ((n-1) % 3) * ns + ns / 2f;
                float ny = row * cellSize + ((n-1) / 3) * ns + ns / 2f;
                String s = String.valueOf(n);
                pNotes.getTextBounds(s, 0, 1, textBounds);
                canvas.drawText(s, nx, ny - textBounds.exactCenterY(), pNotes);
            }
        }
    }

    // ── Grille ────────────────────────────────────────────────────────────────

    private void drawGrid(Canvas canvas) {
        float size = cellSize * SudokuBoard.SIZE;
        for (int i = 0; i <= SudokuBoard.SIZE; i++) {
            Paint p = (i % SudokuBoard.BOX_SIZE == 0) ? pThick : pThin;
            float pos = i * cellSize;
            canvas.drawLine(pos, 0, pos, size, p);
            canvas.drawLine(0, pos, size, pos, p);
        }
    }

    // ── Diagonales (mode Serpentart) ──────────────────────────────────────────

    private void drawDiagonalLines(Canvas canvas) {
        float s = cellSize * SudokuBoard.SIZE;
        float m = 2f;
        canvas.drawLine(m, m, s - m, s - m, pDiagLine);
        canvas.drawLine(s - m, m, m, s - m, pDiagLine);
    }

    // ── Serpent (mode Serpentart) ─────────────────────────────────────────────

    private void drawSnakeOverlay(Canvas canvas) {
        SnakePath snake = board.getSnake();
        if (snake == null) return;

        // 1. Contour des cases du serpent
        for (int i = 0; i < snake.size(); i++) {
            int r = snake.getRow(i), c = snake.getCol(i);
            float l = c * cellSize + 2f, t = r * cellSize + 2f;
            float ri = l + cellSize - 4f, bo = t + cellSize - 4f;
            canvas.drawRect(l, t, ri, bo, pSnakeBorder);
        }

        // 2. Flèches entre cases consécutives
        for (int i = 0; i < snake.size() - 1; i++) {
            int dir = snake.directionAt(i);
            int r = snake.getRow(i), c = snake.getCol(i);
            drawArrow(canvas, r, c, dir);
        }

        // 3. Point de départ (cercle sur la tête)
        int hr = snake.getRow(0), hc = snake.getCol(0);
        float hx = hc * cellSize + cellSize / 2f;
        float hy = hr * cellSize + cellSize / 2f;
        float radius = cellSize * 0.12f;
        canvas.drawCircle(hx, hy, radius, pSnakeArrow);
    }

    /**
     * Dessine une petite flèche au bord de la cellule (r,c) pointant dans la direction dir.
     * dir : 0=haut 1=bas 2=gauche 3=droite
     */
    private void drawArrow(Canvas canvas, int row, int col, int dir) {
        float cx = col * cellSize + cellSize / 2f;
        float cy = row * cellSize + cellSize / 2f;
        float half = cellSize / 2f;
        float arrowLen  = cellSize * 0.22f;
        float arrowHead = cellSize * 0.10f;

        // Point de départ de la flèche (au centre, décalé vers le bord)
        float ox, oy, tx, ty;
        switch (dir) {
            case 0: ox = cx; oy = cy - half * 0.35f; tx = ox; ty = oy - arrowLen; break; // haut
            case 1: ox = cx; oy = cy + half * 0.35f; tx = ox; ty = oy + arrowLen; break; // bas
            case 2: ox = cx - half * 0.35f; oy = cy; tx = ox - arrowLen; ty = oy; break; // gauche
            default: ox = cx + half * 0.35f; oy = cy; tx = ox + arrowLen; ty = oy; break;// droite
        }

        // Tige
        pSnakeArrow.setStyle(Paint.Style.STROKE);
        pSnakeArrow.setStrokeWidth(cellSize * 0.06f);
        canvas.drawLine(ox, oy, tx, ty, pSnakeArrow);

        // Pointe triangulaire
        pSnakeArrow.setStyle(Paint.Style.FILL);
        arrowPath.reset();
        switch (dir) {
            case 0: // haut
                arrowPath.moveTo(tx, ty - arrowHead);
                arrowPath.lineTo(tx - arrowHead * 0.7f, ty);
                arrowPath.lineTo(tx + arrowHead * 0.7f, ty);
                break;
            case 1: // bas
                arrowPath.moveTo(tx, ty + arrowHead);
                arrowPath.lineTo(tx - arrowHead * 0.7f, ty);
                arrowPath.lineTo(tx + arrowHead * 0.7f, ty);
                break;
            case 2: // gauche
                arrowPath.moveTo(tx - arrowHead, ty);
                arrowPath.lineTo(tx, ty - arrowHead * 0.7f);
                arrowPath.lineTo(tx, ty + arrowHead * 0.7f);
                break;
            default: // droite
                arrowPath.moveTo(tx + arrowHead, ty);
                arrowPath.lineTo(tx, ty - arrowHead * 0.7f);
                arrowPath.lineTo(tx, ty + arrowHead * 0.7f);
                break;
        }
        arrowPath.close();
        canvas.drawPath(arrowPath, pSnakeArrow);
    }

    // ── Touch ─────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            int c = (int)(e.getX() / cellSize), r = (int)(e.getY() / cellSize);
            if (r >= 0 && r < SudokuBoard.SIZE && c >= 0 && c < SudokuBoard.SIZE) {
                if (touchListener != null) touchListener.onCellTouched(r, c);
                return true;
            }
        }
        return super.onTouchEvent(e);
    }

    // ── API publique ──────────────────────────────────────────────────────────

    public void setBoard(SudokuBoard board) { this.board = board; invalidate(); }
    public void setTouchListener(CellTouchListener l) { touchListener = l; }
    public void setNotesMode(boolean on) { notesMode = on; }
    public boolean isNotesMode() { return notesMode; }

    public void toggleNote(int r, int c, int n) { notes[r][c] ^= (1 << n); invalidate(); }
    public void clearNotes(int r, int c)         { notes[r][c] = 0; invalidate(); }

    public void setNotes(int[][] saved) {
        for (int r = 0; r < SudokuBoard.SIZE; r++)
            System.arraycopy(saved[r], 0, notes[r], 0, SudokuBoard.SIZE);
        invalidate();
    }

    public int[][] getNotes() {
        int[][] copy = new int[SudokuBoard.SIZE][SudokuBoard.SIZE];
        for (int r = 0; r < SudokuBoard.SIZE; r++)
            System.arraycopy(notes[r], 0, copy[r], 0, SudokuBoard.SIZE);
        return copy;
    }

    public void refresh() { invalidate(); }
}
