package fr.svpro.sudoku.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Clavier numérique 1-9 + effacement.
 */
public class NumberPadView extends LinearLayout {

    public interface OnNumberClickListener {
        void onNumber(int number);   // 1-9
        void onErase();
    }

    private OnNumberClickListener listener;

    public NumberPadView(Context context) { super(context); init(); }
    public NumberPadView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);

        // Boutons 1-9
        for (int i = 1; i <= 9; i++) {
            addView(makeButton(String.valueOf(i), i, false));
        }
        // Bouton effacement
        addView(makeButton("✕", 0, true));
    }

    private Button makeButton(String label, int num, boolean erase) {
        Button btn = new Button(getContext());
        btn.setText(label);
        btn.setTextSize(18f);
        btn.setTextColor(erase ? Color.parseColor("#C62828") : Color.parseColor("#1A237E"));
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> {
            if (listener == null) return;
            if (erase) listener.onErase();
            else listener.onNumber(num);
        });
        return btn;
    }

    public void setOnNumberClickListener(OnNumberClickListener l) { this.listener = l; }
}
