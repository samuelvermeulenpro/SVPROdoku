package fr.svpro.sudoku.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.svpro.sudoku.model.SudokuBoard;

/**
 * Sauvegarde et restauration d'une partie en cours dans SharedPreferences.
 *
 * Format JSON stocké sous la clé "saved_game" :
 * {
 *   "difficulty"   : "EASY" | "MEDIUM" | "HARD",
 *   "solution"     : "81 chiffres, ligne par ligne",
 *   "puzzle"       : "81 chiffres (grille initiale)",
 *   "userGrid"     : "81 chiffres (saisies utilisateur)",
 *   "fixed"        : "81 booléens 0/1",
 *   "errors"       : "81 booléens 0/1",
 *   "notes"        : "81 entiers bitmask",
 *   "mistakes"     : int,
 *   "hints"        : int,
 *   "elapsedSecs"  : long,
 *   "timestamp"    : long (epoch ms)
 * }
 */
public class GameSave {

    private static final String PREFS_NAME = "sudoku_save";
    private static final String KEY_GAME   = "saved_game";

    // ── Snapshot transporté entre Save et GameActivity ────────────────────

    public static class Snapshot {
        public SudokuBoard.Difficulty difficulty;
        public SudokuBoard.Variant    variant;
        public int[][] solution;
        public int[][] puzzle;
        public int[][] userGrid;
        public boolean[][] fixed;
        public boolean[][] errors;
        public int[][] notes;       // bitmask 1-9 par case
        public int  mistakes;
        public int  hints;
        public long elapsedSeconds;
        public long timestamp;
        public fr.svpro.sudoku.model.SnakePath snake; // null si CLASSIC
    }

    // ── Écriture ──────────────────────────────────────────────────────────

    public static void save(Context ctx,
                            SudokuBoard board,
                            int[][] notes,
                            long elapsedSeconds) {
        try {
            JSONObject json = new JSONObject();
            json.put("difficulty",  board.getDifficulty().name());
            json.put("variant",     board.getVariant().name());
            json.put("solution",    gridToString(board.getSolutionGrid()));
            json.put("puzzle",      gridToString(board.getPuzzleGrid()));
            json.put("userGrid",    gridToString(board.getUserGrid()));
            json.put("fixed",       boolGridToString(board.getFixedGrid()));
            json.put("errors",      boolGridToString(board.getErrorsGrid()));
            json.put("notes",       notesToString(notes));
            json.put("mistakes",    board.getMistakeCount());
            json.put("hints",       board.getHintCount());
            json.put("elapsedSecs", elapsedSeconds);
            json.put("timestamp",   System.currentTimeMillis());
            if (board.getSnake() != null)
                json.put("snake", board.getSnake().serialize());

            prefs(ctx).edit().putString(KEY_GAME, json.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ── Lecture ───────────────────────────────────────────────────────────

    public static Snapshot load(Context ctx) {
        String raw = prefs(ctx).getString(KEY_GAME, null);
        if (raw == null) return null;
        try {
            JSONObject json = new JSONObject(raw);
            Snapshot s = new Snapshot();
            s.difficulty    = SudokuBoard.Difficulty.valueOf(json.getString("difficulty"));
            // rétro-compatibilité : les anciens saves n'ont pas "variant"
            String variantStr = json.optString("variant", "CLASSIC");
            s.variant       = SudokuBoard.Variant.valueOf(variantStr);
            s.solution      = stringToGrid(json.getString("solution"));
            s.puzzle        = stringToGrid(json.getString("puzzle"));
            s.userGrid      = stringToGrid(json.getString("userGrid"));
            s.fixed         = stringToBoolGrid(json.getString("fixed"));
            s.errors        = stringToBoolGrid(json.getString("errors"));
            s.notes         = stringToNotes(json.getString("notes"));
            s.mistakes      = json.getInt("mistakes");
            s.hints         = json.getInt("hints");
            s.elapsedSeconds= json.getLong("elapsedSecs");
            s.timestamp     = json.getLong("timestamp");
            String snakeStr = json.optString("snake", null);
            s.snake = (snakeStr != null && !snakeStr.isEmpty())
                    ? fr.svpro.sudoku.model.SnakePath.deserialize(snakeStr)
                    : null;
            return s;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasSave(Context ctx) {
        return prefs(ctx).contains(KEY_GAME);
    }

    public static void delete(Context ctx) {
        prefs(ctx).edit().remove(KEY_GAME).apply();
    }

    // ── Helpers de sérialisation ──────────────────────────────────────────

    private static String gridToString(int[][] g) {
        StringBuilder sb = new StringBuilder(81);
        for (int r = 0; r < SudokuBoard.SIZE; r++)
            for (int c = 0; c < SudokuBoard.SIZE; c++)
                sb.append((char) ('0' + g[r][c]));
        return sb.toString();
    }

    private static int[][] stringToGrid(String s) {
        int[][] g = new int[SudokuBoard.SIZE][SudokuBoard.SIZE];
        for (int i = 0; i < s.length(); i++)
            g[i / SudokuBoard.SIZE][i % SudokuBoard.SIZE] = s.charAt(i) - '0';
        return g;
    }

    private static String boolGridToString(boolean[][] g) {
        StringBuilder sb = new StringBuilder(81);
        for (int r = 0; r < SudokuBoard.SIZE; r++)
            for (int c = 0; c < SudokuBoard.SIZE; c++)
                sb.append(g[r][c] ? '1' : '0');
        return sb.toString();
    }

    private static boolean[][] stringToBoolGrid(String s) {
        boolean[][] g = new boolean[SudokuBoard.SIZE][SudokuBoard.SIZE];
        for (int i = 0; i < s.length(); i++)
            g[i / SudokuBoard.SIZE][i % SudokuBoard.SIZE] = s.charAt(i) == '1';
        return g;
    }

    /** Notes : 81 entiers séparés par ',' (bitmask 0-1023) */
    private static String notesToString(int[][] notes) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SudokuBoard.SIZE; r++)
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                if (r > 0 || c > 0) sb.append(',');
                sb.append(notes[r][c]);
            }
        return sb.toString();
    }

    private static int[][] stringToNotes(String s) {
        int[][] notes = new int[SudokuBoard.SIZE][SudokuBoard.SIZE];
        String[] parts = s.split(",");
        for (int i = 0; i < parts.length && i < 81; i++)
            notes[i / SudokuBoard.SIZE][i % SudokuBoard.SIZE] = Integer.parseInt(parts[i].trim());
        return notes;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
