package ir.meros.qrscanner.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import ir.meros.qrscanner.BuildConfig;
import ir.meros.qrscanner.R;

/**
 * Opens this app's page in the store the build was published to.
 *
 * <p>Which store that is comes from {@code BuildConfig.STORE}, set by the
 * product flavor, so the same code lands the user in Bazaar or Myket depending
 * on which APK they installed. If the store app is missing the browser page is
 * used instead.</p>
 */
public final class StoreLinks {

    private StoreLinks() {
    }

    /** Opens the store page for rating/reviewing the app. */
    public static void openAppPage(Context context) {
        String pkg = context.getPackageName();
        if (!tryOpen(context, appScheme(pkg)) && !tryOpen(context, webUrl(pkg))) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * The store address to attach to shared content.
     *
     * <p>The web address rather than a {@code bazaar://} scheme: it lands on
     * someone else's phone, which may not have that store app installed, and
     * both stores hand the web page off to their app when it is.</p>
     */
    public static String appUrl(Context context) {
        return webUrl(context.getPackageName());
    }

    private static String appScheme(String pkg) {
        // Myket has a dedicated review screen; Bazaar rates from the app page.
        return "myket".equals(BuildConfig.STORE)
                ? "myket://comment?id=" + pkg
                : "bazaar://details?id=" + pkg;
    }

    private static String webUrl(String pkg) {
        return "myket".equals(BuildConfig.STORE)
                ? "https://myket.ir/app/" + pkg
                : "https://cafebazaar.ir/app/" + pkg;
    }

    private static boolean tryOpen(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
