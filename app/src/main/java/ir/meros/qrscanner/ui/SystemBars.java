package ir.meros.qrscanner.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps content clear of the status and navigation bars.
 *
 * <p>From API 35 the system draws windows edge to edge regardless of the theme,
 * so the root view has to pad itself by the system bar insets or the header ends
 * up under the notification bar and the bottom banner under the gesture pill.</p>
 */
public final class SystemBars {

    private SystemBars() {
    }

    /** Adds the system bar insets to {@code root}'s existing padding. */
    public static void pad(View root) {
        Rect original = new Rect(root.getPaddingLeft(), root.getPaddingTop(),
                root.getPaddingRight(), root.getPaddingBottom());
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(original.left + bars.left,
                    original.top + bars.top,
                    original.right + bars.right,
                    original.bottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
