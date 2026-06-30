package fr.svpro.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import fr.svpro.sudoku.model.SudokuBoard;
import fr.svpro.sudoku.util.GameSave;
import fr.svpro.sudoku.util.GameTimer;
import fr.svpro.sudoku.view.NumberPadView;
import fr.svpro.sudoku.view.SudokuBoardView;

public class GameActivity extends AppCompatActivity
        implements SudokuBoardView.CellTouchListener, NumberPadView.OnNumberClickListener {

    private SudokuBoard board;
    private SudokuBoardView boardView;
    private NumberPadView numPad;
    private GameTimer timer;

    private TextView tvTimer, tvMistakes, tvDifficulty;
    private Button btnNotes;
    private ImageButton btnHint, btnCheck, btnUndo;

    private static final int MAX_MISTAKES = 3;

    private final int[] undoRow = new int[200];
    private final int[] undoCol = new int[200];
    private final int[] undoVal = new int[200];
    private int undoTop = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        boardView    = findViewById(R.id.sudokuBoard);
        numPad       = findViewById(R.id.numberPad);
        tvTimer      = findViewById(R.id.tvTimer);
        tvMistakes   = findViewById(R.id.tvMistakes);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        btnNotes     = findViewById(R.id.btnNotes);
        btnHint      = findViewById(R.id.btnHint);
        btnCheck     = findViewById(R.id.btnCheck);
        btnUndo      = findViewById(R.id.btnUndo);

        timer = new GameTimer();
        timer.setListener(elapsed -> tvTimer.setText(GameTimer.format(elapsed)));

        // ── Restauration ou nouvelle partie ──────────────────────────────
        boolean restoring = getIntent().getBooleanExtra(MainActivity.EXTRA_RESUME, false);
        if (restoring) {
            restoreGame();
        } else {
            String diffName = getIntent().getStringExtra(MainActivity.EXTRA_DIFFICULTY);
            String varName  = getIntent().getStringExtra(MainActivity.EXTRA_VARIANT);
            SudokuBoard.Difficulty diff = (diffName != null)
                    ? SudokuBoard.Difficulty.valueOf(diffName)
                    : SudokuBoard.Difficulty.EASY;
            SudokuBoard.Variant variant = (varName != null)
                    ? SudokuBoard.Variant.valueOf(varName)
                    : SudokuBoard.Variant.CLASSIC;
            board = new SudokuBoard(diff, variant);
        }

        boardView.setBoard(board);
        boardView.setTouchListener(this);
        numPad.setOnNumberClickListener(this);

        tvDifficulty.setText(getDifficultyLabel(board.getDifficulty()));
        updateMistakesUI();

        btnNotes.setOnClickListener(v -> {
            boolean on = !boardView.isNotesMode();
            boardView.setNotesMode(on);
            btnNotes.setAlpha(on ? 1f : 0.5f);
        });
        btnNotes.setAlpha(0.5f);

        btnHint.setOnClickListener(v -> giveHint());
        btnCheck.setOnClickListener(v -> {
            board.highlightErrors();
            boardView.refresh();
            Toast.makeText(this, R.string.errors_highlighted, Toast.LENGTH_SHORT).show();
        });
        btnUndo.setOnClickListener(v -> undo());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        timer.start();
    }

    // ── Sauvegarde / Restauration ─────────────────────────────────────────

    private void saveGame() {
        if (board == null || board.isSolved()) return;
        GameSave.save(this, board, boardView.getNotes(), timer.getElapsedSeconds());
    }

    private void restoreGame() {
        GameSave.Snapshot s = GameSave.load(this);
        if (s == null) {
            // snapshot corrompu → nouvelle partie facile
            board = new SudokuBoard(SudokuBoard.Difficulty.EASY);
            return;
        }
        board = new SudokuBoard(s.difficulty, s.variant,
                s.solution, s.puzzle, s.userGrid,
                s.fixed, s.errors,
                s.mistakes, s.hints,
                s.snake);
        timer = new GameTimer();
        timer.setListener(elapsed -> tvTimer.setText(GameTimer.format(elapsed)));
        // Le chrono reprend là où il s'était arrêté
        // On le restaure via un décalage interne
        restoreElapsed(s.elapsedSeconds);
        // Notes
        // (boardView n'est initialisé qu'après, on stocke temporairement)
        pendingNotes = s.notes;
    }

    /** Hack minimal : on injecte le temps écoulé dans le timer avant son démarrage. */
    private void restoreElapsed(long seconds) {
        // GameTimer expose un accumulateur — on le pilote via reset + pré-remplissage
        // Plutôt : on sous-classe ici via une méthode package
        timer.addAccumulatedMs(seconds * 1000L);
    }

    private int[][] pendingNotes = null;

    @Override
    protected void onStart() {
        super.onStart();
        // Injecte les notes après que boardView soit attaché à la fenêtre
        if (pendingNotes != null) {
            boardView.setNotes(pendingNotes);
            pendingNotes = null;
        }
    }

    // ── Touch / Saisie ────────────────────────────────────────────────────

    @Override
    public void onCellTouched(int row, int col) {
        board.setSelected(row, col);
        boardView.refresh();
    }

    @Override
    public void onNumber(int number) {
        int row = board.getSelectedRow();
        int col = board.getSelectedCol();
        if (row < 0 || col < 0 || board.isFixed(row, col)) return;

        if (boardView.isNotesMode()) {
            if (board.getValue(row, col) == SudokuBoard.EMPTY)
                boardView.toggleNote(row, col, number);
            saveGame();
            return;
        }

        int prev = board.getValue(row, col);
        if (board.setValue(row, col, number)) {
            boardView.clearNotes(row, col);
            pushUndo(row, col, prev);
            boardView.refresh();
            updateMistakesUI();
            if (board.isSolved()) {
                GameSave.delete(this);   // partie terminée → effacer la sauvegarde
                showWinDialog();
            } else if (board.getMistakeCount() >= MAX_MISTAKES) {
                GameSave.delete(this);
                showLoseDialog();
            } else {
                saveGame();
            }
        }
    }

    @Override
    public void onErase() {
        int row = board.getSelectedRow();
        int col = board.getSelectedCol();
        if (row < 0 || col < 0) return;
        if (boardView.isNotesMode()) { boardView.clearNotes(row, col); saveGame(); return; }
        if (!board.isFixed(row, col)) {
            int prev = board.getValue(row, col);
            board.setValue(row, col, SudokuBoard.EMPTY);
            pushUndo(row, col, prev);
            boardView.refresh();
            saveGame();
        }
    }

    // ── Indice ────────────────────────────────────────────────────────────

    private void giveHint() {
        int row = board.getSelectedRow();
        int col = board.getSelectedCol();
        if (row < 0 || col < 0) {
            Toast.makeText(this, R.string.select_cell_first, Toast.LENGTH_SHORT).show();
            return;
        }
        if (board.isFixed(row, col)) {
            Toast.makeText(this, R.string.cell_already_filled, Toast.LENGTH_SHORT).show();
            return;
        }
        board.useHint();
        boardView.clearNotes(row, col);
        boardView.refresh();
        if (board.isSolved()) { GameSave.delete(this); showWinDialog(); }
        else saveGame();
    }

    // ── Annuler ───────────────────────────────────────────────────────────

    private void pushUndo(int row, int col, int val) {
        if (undoTop < undoRow.length) {
            undoRow[undoTop] = row;
            undoCol[undoTop] = col;
            undoVal[undoTop] = val;
            undoTop++;
        }
    }

    private void undo() {
        if (undoTop == 0) return;
        undoTop--;
        board.setValue(undoRow[undoTop], undoCol[undoTop], undoVal[undoTop]);
        board.setSelected(undoRow[undoTop], undoCol[undoTop]);
        boardView.refresh();
        updateMistakesUI();
        saveGame();
    }

    // ── Dialogues ─────────────────────────────────────────────────────────

    private void showWinDialog() {
        timer.pause();
        long elapsed = timer.getElapsedSeconds();
        new AlertDialog.Builder(this)
                .setTitle(R.string.win_title)
                .setMessage(getString(R.string.win_message,
                        GameTimer.format(elapsed), board.getMistakeCount(), board.getHintCount()))
                .setPositiveButton(R.string.new_game, (d, w) -> goToMenu())
                .setNegativeButton(R.string.back_menu, (d, w) -> goToMenu())
                .setCancelable(false).show();
    }

    private void showLoseDialog() {
        timer.pause();
        new AlertDialog.Builder(this)
                .setTitle(R.string.lose_title)
                .setMessage(R.string.lose_message)
                .setPositiveButton(R.string.new_game, (d, w) -> goToMenu())
                .setNegativeButton(R.string.back_menu, (d, w) -> goToMenu())
                .setCancelable(false).show();
    }

    private void goToMenu() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    // ── Cycle de vie ──────────────────────────────────────────────────────

    @Override
    protected void onPause() {
        super.onPause();
        timer.pause();
        saveGame();   // ← sauvegarde automatique à chaque mise en arrière-plan
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!board.isSolved()) timer.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void updateMistakesUI() {
        tvMistakes.setText(getString(R.string.mistakes, board.getMistakeCount(), MAX_MISTAKES));
    }

    private String getDifficultyLabel(SudokuBoard.Difficulty d) {
        String diff;
        switch (d) {
            case EASY:   diff = getString(R.string.difficulty_easy);   break;
            case MEDIUM: diff = getString(R.string.difficulty_medium); break;
            case HARD:   diff = getString(R.string.difficulty_hard);   break;
            default:     diff = ""; break;
        }
        if (board.getVariant() == SudokuBoard.Variant.SERPENTART)
            return getString(R.string.variant_serpentart) + " · " + diff;
        return diff;
    }
}
