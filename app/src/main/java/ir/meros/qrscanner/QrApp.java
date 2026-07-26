package ir.meros.qrscanner;

import android.app.Application;

import ir.meros.qrscanner.ads.TapsellAdManager;

/**
 * Application entry point. Declared in the manifest as {@code android:name}.
 *
 * <p>The ad SDK is initialised here rather than in an Activity so that the
 * handshake starts as early as possible and happens exactly once per process;
 * screens only ask for banners and are served as soon as init reports back.</p>
 */
public class QrApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TapsellAdManager.init(this);
    }
}
