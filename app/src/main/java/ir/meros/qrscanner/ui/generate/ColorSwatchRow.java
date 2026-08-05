package ir.meros.qrscanner.ui.generate;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import ir.meros.qrscanner.R;

/**
 * Fills a horizontal container with round colour swatches and keeps exactly one
 * of them selected. Built in code rather than XML because the palettes are just
 * lists of colours — a dozen near-identical {@code <View>} blocks per row would
 * be harder to change than the loop below.
 */
final class ColorSwatchRow {

    interface Listener {
        void onColorPicked(int color);
    }

    private static final int SWATCH_DP = 40;
    private static final int GAP_DP = 8;
    private static final int RING_DP = 1;
    private static final int SELECTED_RING_DP = 3;

    private final LinearLayout container;
    private final int[] colors;
    private final Listener listener;
    private int selected;

    private ColorSwatchRow(LinearLayout container, int[] colors, int selected, Listener listener) {
        this.container = container;
        this.colors = colors;
        this.selected = selected;
        this.listener = listener;
    }

    /** Builds the swatches into {@code container}, marking {@code selected} as current. */
    static void into(LinearLayout container, int[] colors, int selected, Listener listener) {
        new ColorSwatchRow(container, colors, selected, listener).build();
    }

    private void build() {
        Context context = container.getContext();
        container.removeAllViews();
        for (int color : colors) {
            View swatch = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(context, SWATCH_DP), dp(context, SWATCH_DP));
            params.setMarginEnd(dp(context, GAP_DP));
            swatch.setLayoutParams(params);
            swatch.setBackground(drawableFor(context, color, color == selected));
            swatch.setContentDescription(context.getString(
                    Color.alpha(color) == 0 ? R.string.color_transparent : R.string.option_color));
            swatch.setOnClickListener(v -> pick(color));
            container.addView(swatch);
        }
    }

    private void pick(int color) {
        if (color == selected) {
            return;
        }
        selected = color;
        Context context = container.getContext();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            child.setBackground(drawableFor(context, colors[i], colors[i] == selected));
        }
        listener.onColorPicked(color);
    }

    /**
     * A filled circle, ringed to stay visible against a card of its own colour.
     * The selected swatch gets a thicker, darker ring — a checkmark would be
     * invisible on the light half of the palette.
     */
    private static GradientDrawable drawableFor(Context context, int color, boolean isSelected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        shape.setStroke(
                dp(context, isSelected ? SELECTED_RING_DP : RING_DP),
                ContextCompat.getColor(context,
                        isSelected ? R.color.swatch_selected_ring : R.color.swatch_ring));
        return shape;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}
