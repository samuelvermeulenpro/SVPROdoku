package fr.svpro.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import fr.svpro.sudoku.model.SudokuBoard;
import fr.svpro.sudoku.util.GameSave;
import fr.svpro.sudoku.util.GameTimer;

/**
 * Écran d'accueil.
 * Propose : reprendre la partie en cours, Classic (3 niveaux), Serpentart (3 niveaux).
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_VARIANT    = "variant";
    public static final String EXTRA_RESUME     = "resume";

    private LinearLayout cardResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardResume = findViewById(R.id.cardResume);

        // ── Reprendre ──
        findViewById(R.id.btnResume).setOnClickListener(v -> resumeGame());

        // ── Classic ──
        findViewById(R.id.btnEasy)  .setOnClickListener(v ->
                startGame(SudokuBoard.Difficulty.EASY,   SudokuBoard.Variant.CLASSIC));
        findViewById(R.id.btnMedium).setOnClickListener(v ->
                startGame(SudokuBoard.Difficulty.MEDIUM, SudokuBoard.Variant.CLASSIC));
        findViewById(R.id.btnHard)  .setOnClickListener(v ->
                startGame(SudokuBoard.Difficulty.HARD,   SudokuBoard.Variant.CLASSIC));

        // ── Serpentart ──
        findViewById(R.id.btnSerpEasy)  .setOnClickListener(v ->
                startGame(SudokuBoard.Difficulty.EASY,   SudokuBoard.Variant.SERPENTART));
        findViewById(R.id.btnSerpMedium).setOnClickListener(v ->
                startGame(SudokuBoard.Difficulty.MEDIUM, SudokuBoard.Variant.SERPENTART));
        findViewById(R.id.btnSerpHard)  .setOnClickListener(v ->
                startGame(SudokuBoard.Difficulty.HARD,   SudokuBoard.Variant.SERPENTART));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshResumeCard();
    }

    private void refreshResumeCard() {
        if (GameSave.hasSave(this)) {
            cardResume.setVisibility(View.VISIBLE);
            GameSave.Snapshot s = GameSave.load(this);
            if (s != null) {
                TextView tvInfo = findViewById(R.id.tvResumeInfo);
                String variantLabel = s.variant == SudokuBoard.Variant.SERPENTART
                        ? getString(R.string.variant_serpentart)
                        : getString(R.string.variant_classic);
                tvInfo.setText(variantLabel + " · " + diffLabel(s.difficulty)
                        + "  •  " + GameTimer.format(s.elapsedSeconds));
            }
        } else {
            cardResume.setVisibility(View.GONE);
        }
    }

    private void resumeGame() {
        startActivity(new Intent(this, GameActivity.class)
                .putExtra(EXTRA_RESUME, true));
    }

    private void startGame(SudokuBoard.Difficulty diff, SudokuBoard.Variant variant) {
        if (GameSave.hasSave(this)) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.new_game_confirm_title)
                    .setMessage(R.string.new_game_confirm_message)
                    .setPositiveButton(R.string.yes, (d, w) -> {
                        GameSave.delete(this);
                        launchNewGame(diff, variant);
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            launchNewGame(diff, variant);
        }
    }

    private void launchNewGame(SudokuBoard.Difficulty diff, SudokuBoard.Variant variant) {
        startActivity(new Intent(this, GameActivity.class)
                .putExtra(EXTRA_DIFFICULTY, diff.name())
                .putExtra(EXTRA_VARIANT, variant.name()));
    }

    private String diffLabel(SudokuBoard.Difficulty d) {
        switch (d) {
            case EASY:   return getString(R.string.difficulty_easy);
            case MEDIUM: return getString(R.string.difficulty_medium);
            case HARD:   return getString(R.string.difficulty_hard);
            default:     return "";
        }
    }
}
