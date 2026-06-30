package fr.svpro.sudoku.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Modèle principal du plateau de Sudoku.
 *
 * Variantes :
 *  - CLASSIC    : règles standard 9×9
 *  - SERPENTART : X-Sudoku (deux diagonales = 1-9) + serpent aléatoire de 9 cases (= 1-9)
 */
public class SudokuBoard {

    public static final int SIZE     = 9;
    public static final int BOX_SIZE = 3;
    public static final int EMPTY    = 0;

    public enum Variant    { CLASSIC, SERPENTART }
    public enum Difficulty {
        EASY(35), MEDIUM(45), HARD(55);
        final int cellsToRemove;
        Difficulty(int n) { cellsToRemove = n; }
    }

    // ── État interne ──────────────────────────────────────────────────────────
    private final int[][]     solution;
    private final int[][]     puzzle;
    private final int[][]     userGrid;
    private final boolean[][] fixed;
    private final boolean[][] errors;

    private final Variant    variant;
    private final Difficulty difficulty;
    private       SnakePath  snake;      // null en mode CLASSIC

    private int  selectedRow  = -1;
    private int  selectedCol  = -1;
    private int  mistakeCount = 0;
    private int  hintCount    = 0;
    private boolean solved    = false;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    /** Nouvelle partie. */
    public SudokuBoard(Difficulty difficulty, Variant variant) {
        this.difficulty = difficulty;
        this.variant    = variant;
        solution = new int[SIZE][SIZE];
        puzzle   = new int[SIZE][SIZE];
        userGrid = new int[SIZE][SIZE];
        fixed    = new boolean[SIZE][SIZE];
        errors   = new boolean[SIZE][SIZE];
        if (variant == Variant.SERPENTART)
            snake = SnakePath.generate(new Random());
        generate();
    }

    public SudokuBoard(Difficulty difficulty) { this(difficulty, Variant.CLASSIC); }

    /** Constructeur de restauration (GameSave). */
    public SudokuBoard(Difficulty difficulty, Variant variant,
                       int[][] solution, int[][] puzzle, int[][] userGrid,
                       boolean[][] fixed, boolean[][] errors,
                       int mistakes, int hints,
                       SnakePath snake) {
        this.difficulty   = difficulty;
        this.variant      = variant;
        this.solution     = solution;
        this.puzzle       = puzzle;
        this.userGrid     = userGrid;
        this.fixed        = fixed;
        this.errors       = errors;
        this.mistakeCount = mistakes;
        this.hintCount    = hints;
        this.snake        = snake;
        checkSolved();
    }

    /** Rétro-compat restauration sans serpent (anciens saves). */
    public SudokuBoard(Difficulty difficulty, Variant variant,
                       int[][] solution, int[][] puzzle, int[][] userGrid,
                       boolean[][] fixed, boolean[][] errors,
                       int mistakes, int hints) {
        this(difficulty, variant, solution, puzzle, userGrid,
             fixed, errors, mistakes, hints, null);
    }

    public SudokuBoard(Difficulty difficulty,
                       int[][] solution, int[][] puzzle, int[][] userGrid,
                       boolean[][] fixed, boolean[][] errors,
                       int mistakes, int hints) {
        this(difficulty, Variant.CLASSIC, solution, puzzle, userGrid,
             fixed, errors, mistakes, hints, null);
    }

    // ── Génération ────────────────────────────────────────────────────────────

    private void generate() {
        fillBoard(solution, 0, 0);
        copyGrid(solution, puzzle);
        removeCells(puzzle, difficulty.cellsToRemove);
        copyGrid(puzzle, userGrid);
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                fixed[r][c] = puzzle[r][c] != EMPTY;
    }

    private boolean fillBoard(int[][] grid, int row, int col) {
        if (row == SIZE) return true;
        int nextRow = (col == SIZE - 1) ? row + 1 : row;
        int nextCol = (col == SIZE - 1) ? 0        : col + 1;

        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= SIZE; i++) nums.add(i);
        Collections.shuffle(nums, new Random());

        for (int num : nums) {
            if (isValidPlacement(grid, row, col, num)) {
                grid[row][col] = num;
                if (fillBoard(grid, nextRow, nextCol)) return true;
                grid[row][col] = EMPTY;
            }
        }
        return false;
    }

    private void removeCells(int[][] grid, int count) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < SIZE * SIZE; i++) positions.add(i);
        Collections.shuffle(positions, new Random());

        int removed = 0;
        for (int pos : positions) {
            if (removed >= count) break;
            int r = pos / SIZE, c = pos % SIZE;
            int backup = grid[r][c];
            grid[r][c] = EMPTY;
            if (countSolutions(copyOf(grid), 0) == 1) removed++;
            else grid[r][c] = backup;
        }
    }

    private int countSolutions(int[][] grid, int count) {
        if (count > 1) return count;
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (grid[r][c] == EMPTY) {
                    for (int num = 1; num <= SIZE; num++) {
                        if (isValidPlacement(grid, r, c, num)) {
                            grid[r][c] = num;
                            count = countSolutions(grid, count);
                            grid[r][c] = EMPTY;
                        }
                    }
                    return count;
                }
        return count + 1;
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public boolean isValidPlacement(int[][] grid, int row, int col, int num) {
        // Ligne + colonne
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == num) return false;
            if (grid[i][col] == num) return false;
        }
        // Boîte 3×3
        int br = (row / BOX_SIZE) * BOX_SIZE;
        int bc = (col / BOX_SIZE) * BOX_SIZE;
        for (int r = br; r < br + BOX_SIZE; r++)
            for (int c = bc; c < bc + BOX_SIZE; c++)
                if (grid[r][c] == num) return false;

        if (variant == Variant.SERPENTART) {
            // Diagonale principale
            if (row == col)
                for (int i = 0; i < SIZE; i++)
                    if (i != col && grid[i][i] == num) return false;
            // Anti-diagonale
            if (row + col == SIZE - 1)
                for (int i = 0; i < SIZE; i++)
                    if (i != row && grid[i][SIZE - 1 - i] == num) return false;
            // Contrainte serpent
            if (snake != null && snake.contains(row, col))
                for (int k = 0; k < snake.size(); k++) {
                    int sr = snake.getRow(k), sc = snake.getCol(k);
                    if ((sr != row || sc != col) && grid[sr][sc] == num) return false;
                }
        }
        return true;
    }

    // ── Saisie joueur ─────────────────────────────────────────────────────────

    public boolean setValue(int row, int col, int value) {
        if (fixed[row][col]) return false;
        userGrid[row][col] = value;
        errors[row][col] = (value != EMPTY) && (value != solution[row][col]);
        if (errors[row][col]) mistakeCount++;
        checkSolved();
        return true;
    }

    public void useHint() {
        if (selectedRow < 0 || selectedCol < 0) return;
        if (fixed[selectedRow][selectedCol]) return;
        userGrid[selectedRow][selectedCol] = solution[selectedRow][selectedCol];
        errors[selectedRow][selectedCol]   = false;
        fixed[selectedRow][selectedCol]    = true;
        hintCount++;
        checkSolved();
    }

    private void checkSolved() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (userGrid[r][c] != solution[r][c]) { solved = false; return; }
        solved = true;
    }

    public void highlightErrors() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (!fixed[r][c] && userGrid[r][c] != EMPTY)
                    errors[r][c] = userGrid[r][c] != solution[r][c];
    }

    // ── Helpers surbrillance ──────────────────────────────────────────────────

    public boolean isSerpentCell(int row, int col) {
        if (variant != Variant.SERPENTART) return false;
        return (snake != null && snake.contains(row, col))
            || row == col
            || row + col == SIZE - 1;
    }

    public boolean isOnDiagonal(int row, int col) {
        if (variant != Variant.SERPENTART) return false;
        return row == col || row + col == SIZE - 1;
    }

    public boolean isOnSnake(int row, int col) {
        return variant == Variant.SERPENTART && snake != null && snake.contains(row, col);
    }

    public boolean isInSameDiagonal(int row, int col) {
        if (variant != Variant.SERPENTART || selectedRow < 0) return false;
        if (selectedRow == selectedCol && row == col) return true;
        if (selectedRow + selectedCol == SIZE - 1 && row + col == SIZE - 1) return true;
        return false;
    }

    public boolean isInSameSnake(int row, int col) {
        if (variant != Variant.SERPENTART || snake == null || selectedRow < 0) return false;
        return snake.contains(row, col) && snake.contains(selectedRow, selectedCol);
    }

    public boolean isInSameRowColBox(int row, int col) {
        if (selectedRow < 0) return false;
        if (row == selectedRow || col == selectedCol) return true;
        int br = (selectedRow / BOX_SIZE) * BOX_SIZE;
        int bc = (selectedCol / BOX_SIZE) * BOX_SIZE;
        return row >= br && row < br + BOX_SIZE && col >= bc && col < bc + BOX_SIZE;
    }

    public boolean isSameValue(int row, int col) {
        if (selectedRow < 0 || userGrid[selectedRow][selectedCol] == EMPTY) return false;
        return userGrid[row][col] == userGrid[selectedRow][selectedCol];
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int        getValue(int r, int c)    { return userGrid[r][c]; }
    public int        getSolution(int r, int c) { return solution[r][c]; }
    public boolean    isFixed(int r, int c)     { return fixed[r][c]; }
    public boolean    isError(int r, int c)     { return errors[r][c]; }
    public boolean    isSolved()                { return solved; }
    public int        getMistakeCount()         { return mistakeCount; }
    public int        getHintCount()            { return hintCount; }
    public Difficulty getDifficulty()           { return difficulty; }
    public Variant    getVariant()              { return variant; }
    public SnakePath  getSnake()                { return snake; }
    public int        getSelectedRow()          { return selectedRow; }
    public int        getSelectedCol()          { return selectedCol; }

    public void setSelected(int row, int col) { selectedRow = row; selectedCol = col; }

    // ── Sérialisation (GameSave) ──────────────────────────────────────────────

    public int[][]     getSolutionGrid() { return solution; }
    public int[][]     getPuzzleGrid()   { return puzzle;   }
    public int[][]     getUserGrid()     { return userGrid; }
    public boolean[][] getFixedGrid()    { return fixed;    }
    public boolean[][] getErrorsGrid()   { return errors;   }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void copyGrid(int[][] src, int[][] dst) {
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(src[r], 0, dst[r], 0, SIZE);
    }

    private int[][] copyOf(int[][] src) {
        int[][] c = new int[SIZE][SIZE]; copyGrid(src, c); return c;
    }
}
