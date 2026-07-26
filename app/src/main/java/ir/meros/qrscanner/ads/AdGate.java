package ir.meros.qrscanner.ads;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Decides how often an interstitial may appear.
 *
 * <p>Counts completed user actions (a scan, a generated code) and lets an ad
 * through only once every {@link #ACTIONS_PER_AD}. The count survives restarts,
 * so closing and reopening the app is not a way to see an ad on every scan —
 * and, more importantly, a user who scans one code and leaves is never
 * interrupted.</p>
 */
public final class AdGate {

    private static final String PREFS = "ad_gate";
    private static final String KEY_ACTIONS = "actions_since_ad";

    /** Completed actions required before the next interstitial. */
    private static final int ACTIONS_PER_AD = 3;

    private AdGate() {
    }

    /** Records one completed scan or generate. */
    public static void recordAction(Context context) {
        SharedPreferences prefs = prefs(context);
        prefs.edit().putInt(KEY_ACTIONS, prefs.getInt(KEY_ACTIONS, 0) + 1).apply();
    }

    /**
     * Returns whether enough actions have piled up to show an ad, resetting the
     * count when it does. Only call this when an ad is actually ready to show,
     * otherwise the credit is spent on nothing.
     */
    public static boolean consumeIfDue(Context context) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getInt(KEY_ACTIONS, 0) < ACTIONS_PER_AD) {
            return false;
        }
        prefs.edit().putInt(KEY_ACTIONS, 0).apply();
        return true;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
