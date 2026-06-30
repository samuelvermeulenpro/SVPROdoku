package fr.svpro.sudoku.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Génère et représente le chemin « serpent » du mode Serpentart.
 *
 * Le serpent est un chemin de 9 cases connexes (4-connexité, sans diagonale)
 * qui couvre exactement 9 cases distinctes de la grille 9×9.
 * Ces 9 cases constituent une nouvelle contrainte : elles doivent contenir
 * les chiffres 1-9 sans répétition (comme une ligne, colonne ou boîte).
 *
 * Le chemin est sérialisé sous forme de chaîne "r0c0,r1c1,…" pour GameSave.
 */
public class SnakePath {

    // ── Directions (haut, bas, gauche, droite) ────────────────────────────────
    private static final int[] DR = {-1, 1,  0, 0};
    private static final int[] DC = { 0, 0, -1, 1};

    /** Coordonnées ordonnées du chemin [0..8]. */
    private final int[] rows;
    private final int[] cols;

    /** Lookup rapide : snakeMask[r][c] = position dans le chemin (1-9) ou 0. */
    private final int[][] positionInSnake;

    // ── Construction ──────────────────────────────────────────────────────────

    public SnakePath(int[] rows, int[] cols) {
        if (rows.length != 9 || cols.length != 9)
            throw new IllegalArgumentException("Le serpent doit comporter exactement 9 cases");
        this.rows = rows.clone();
        this.cols = cols.clone();
        this.positionInSnake = new int[SudokuBoard.SIZE][SudokuBoard.SIZE];
        for (int i = 0; i < 9; i++)
            positionInSnake[rows[i]][cols[i]] = i + 1; // 1-indexé
    }

    // ── Génération aléatoire ──────────────────────────────────────────────────

    /**
     * Génère un chemin aléatoire de 9 cases en DFS avec backtracking.
     * On essaie depuis plusieurs points de départ aléatoires jusqu'à succès.
     */
    public static SnakePath generate(Random rng) {
        int[] rows = new int[9];
        int[] cols = new int[9];
        boolean[][] visited = new boolean[SudokuBoard.SIZE][SudokuBoard.SIZE];

        // Points de départ aléatoires
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i < SudokuBoard.SIZE * SudokuBoard.SIZE; i++) starts.add(i);
        Collections.shuffle(starts, rng);

        for (int start : starts) {
            int sr = start / SudokuBoard.SIZE;
            int sc = start % SudokuBoard.SIZE;
            // Reset
            for (int r = 0; r < SudokuBoard.SIZE; r++)
                java.util.Arrays.fill(visited[r], false);
            rows[0] = sr;
            cols[0] = sc;
            visited[sr][sc] = true;
            if (dfs(rows, cols, visited, rng, 1)) {
                return new SnakePath(rows, cols);
            }
        }
        // Ne devrait jamais arriver, mais sécurité : serpent en L par défaut
        return defaultSnake();
    }

    private static boolean dfs(int[] rows, int[] cols, boolean[][] visited,
                                Random rng, int depth) {
        if (depth == 9) return true;

        // Mélanger les directions
        int[] dirs = {0, 1, 2, 3};
        shuffleArray(dirs, rng);

        int r = rows[depth - 1];
        int c = cols[depth - 1];
        for (int d : dirs) {
            int nr = r + DR[d];
            int nc = c + DC[d];
            if (nr >= 0 && nr < SudokuBoard.SIZE && nc >= 0 && nc < SudokuBoard.SIZE
                    && !visited[nr][nc]) {
                visited[nr][nc] = true;
                rows[depth] = nr;
                cols[depth] = nc;
                if (dfs(rows, cols, visited, rng, depth + 1)) return true;
                visited[nr][nc] = false;
            }
        }
        return false;
    }

    private static void shuffleArray(int[] arr, Random rng) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
    }

    /** Serpent de secours : colonne 4, lignes 0-8. */
    private static SnakePath defaultSnake() {
        int[] r = {0,1,2,3,4,5,6,7,8};
        int[] c = {4,4,4,4,4,4,4,4,4};
        return new SnakePath(r, c);
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /** Vrai si (row,col) fait partie du serpent. */
    public boolean contains(int row, int col) {
        return positionInSnake[row][col] > 0;
    }

    /** Position dans le chemin (1-9), ou 0 si hors serpent. */
    public int positionOf(int row, int col) {
        return positionInSnake[row][col];
    }

    public int getRow(int index) { return rows[index]; }
    public int getCol(int index) { return cols[index]; }
    public int size() { return 9; }

    /**
     * Direction de la case index vers la suivante.
     * Retourne un int : 0=haut, 1=bas, 2=gauche, 3=droite, -1=fin.
     */
    public int directionAt(int index) {
        if (index >= 8) return -1;
        int dr = rows[index + 1] - rows[index];
        int dc = cols[index + 1] - cols[index];
        if (dr == -1) return 0;
        if (dr ==  1) return 1;
        if (dc == -1) return 2;
        return 3;
    }

    // ── Sérialisation ─────────────────────────────────────────────────────────

    /** "r0,c0,r1,c1,…,r8,c8" */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i > 0) sb.append(',');
            sb.append(rows[i]).append(',').append(cols[i]);
        }
        return sb.toString();
    }

    public static SnakePath deserialize(String s) {
        String[] parts = s.split(",");
        int[] r = new int[9], c = new int[9];
        for (int i = 0; i < 9; i++) {
            r[i] = Integer.parseInt(parts[i * 2].trim());
            c[i] = Integer.parseInt(parts[i * 2 + 1].trim());
        }
        return new SnakePath(r, c);
    }
}
