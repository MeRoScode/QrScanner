package ir.meros.qrscanner.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import ir.tapsell.plus.AdRequestCallback;
import ir.tapsell.plus.AdShowListener;
import ir.tapsell.plus.TapsellPlus;
import ir.tapsell.plus.TapsellPlusBannerType;
import ir.tapsell.plus.TapsellPlusInitListener;
import ir.tapsell.plus.model.AdNetworkError;
import ir.tapsell.plus.model.AdNetworks;
import ir.tapsell.plus.model.TapsellPlusAdModel;
import ir.tapsell.plus.model.TapsellPlusErrorModel;

/**
 * Thin wrapper around the Tapsell Plus SDK. Keeps the app key, the zone ids and
 * the request/show dance in one place.
 *
 * <p>{@link #init(Context)} runs once from {@code QrApp}. Because a banner
 * request made before init completes is rejected by the SDK, requests that
 * arrive early are parked and replayed from the init callback. Every method is
 * main-thread only, which is where both the Activity lifecycle and the SDK
 * callbacks live, so the static state needs no synchronisation.</p>
 */
public final class TapsellAdManager {

    private static final String TAG = "TapsellAdManager";

    private static final String APP_KEY =
            "kthbsidtcatdtimgqrorarnsfhmimofodeegfjdpallbjafnlleksfkcqaodhksrldlqff";

    /**
     * Banner zone for the single banner at the bottom of the main screen.
     *
     * <p>TODO: carried over from the previous (ticket validator) build — replace
     * with a zone created for this app in the Tapsell dashboard before
     * publishing, otherwise fill and reporting belong to the old app.</p>
     */
    public static final String ZONE_MAIN_BANNER = "650ef1f0b8c2e8295be32d59";

    /**
     * Interstitial zone, shown between actions by {@link AdGate}.
     *
     * <p>TODO: empty until an interstitial zone is created in the Tapsell
     * dashboard. While empty the interstitial is simply skipped — the old app's
     * ids are banner zones and would only produce failed requests.</p>
     */
    public static final String ZONE_INTERSTITIAL = "";

    private static boolean initStarted;
    private static boolean initialized;

    /** Requests received before init finished, keyed by container or by {@link #INTERSTITIAL_KEY}. */
    private static final Map<Object, Runnable> PENDING = new LinkedHashMap<>();
    /** Response ids of shown banners, needed to destroy them. */
    private static final Map<ViewGroup, String> RESPONSE_IDS = new WeakHashMap<>();

    private static final Object INTERSTITIAL_KEY = new Object();
    /** Response id of a preloaded interstitial, or null when none is ready. */
    private static String interstitialResponseId;
    private static boolean interstitialRequested;

    private TapsellAdManager() {
    }

    /** Initialises the SDK. Call once from {@code Application.onCreate}. */
    public static void init(Context context) {
        if (initStarted) {
            return;
        }
        initStarted = true;
        TapsellPlus.initialize(context, APP_KEY, new TapsellPlusInitListener() {
            @Override
            public void onInitializeSuccess(AdNetworks adNetworks) {
                Log.d(TAG, "Tapsell init success: " + adNetworks.name());
                onInitialized();
            }

            @Override
            public void onInitializeFailed(AdNetworks adNetworks, AdNetworkError error) {
                Log.e(TAG, "Tapsell init failed: " + adNetworks.name()
                        + " -> " + error.getErrorMessage());
            }
        });
    }

    /** Fires once, on the first ad network that reports back. */
    private static void onInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        for (Runnable request : PENDING.values()) {
            request.run();
        }
        PENDING.clear();
    }

    /**
     * Requests a 320x50 standard banner for {@code zoneId} and renders it into
     * {@code container} once ready. The container stays {@code GONE} until an ad
     * actually shows, so a failed or unfilled request leaves no empty strip.
     */
    public static void loadBanner(Activity activity, ViewGroup container, String zoneId) {
        container.setVisibility(View.GONE);
        whenReady(container, () -> requestBanner(activity, container, zoneId));
    }

    /** Runs {@code request} now if the SDK is ready, otherwise parks it under {@code key}. */
    private static void whenReady(Object key, Runnable request) {
        if (initialized) {
            request.run();
        } else {
            PENDING.put(key, request);
        }
    }

    private static void requestBanner(Activity activity, ViewGroup container, String zoneId) {
        if (activity.isDestroyed() || activity.isFinishing()) {
            return;
        }
        TapsellPlus.requestStandardBannerAd(activity, zoneId,
                TapsellPlusBannerType.BANNER_320x50,
                new AdRequestCallback() {
                    @Override
                    public void response(TapsellPlusAdModel adModel) {
                        super.response(adModel);
                        showBanner(activity, container, adModel.getResponseId());
                    }

                    @Override
                    public void error(@NonNull String message) {
                        Log.e(TAG, "Banner request failed (" + zoneId + "): " + message);
                        container.setVisibility(View.GONE);
                    }
                });
    }

    private static void showBanner(Activity activity, ViewGroup container, String responseId) {
        if (activity.isDestroyed() || activity.isFinishing()) {
            return;
        }
        RESPONSE_IDS.put(container, responseId);
        TapsellPlus.showStandardBannerAd(activity, responseId, container,
                new AdShowListener() {
                    @Override
                    public void onOpened(TapsellPlusAdModel adModel) {
                        super.onOpened(adModel);
                        container.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(TapsellPlusErrorModel error) {
                        super.onError(error);
                        Log.e(TAG, "Banner show failed: " + error.toString());
                        container.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Fetches an interstitial in the background so one is on hand at the next
     * break point. No-op while a request is in flight or an ad is already held.
     */
    public static void preloadInterstitial(Activity activity) {
        if (ZONE_INTERSTITIAL.isEmpty() || interstitialRequested || interstitialResponseId != null) {
            return;
        }
        interstitialRequested = true;
        whenReady(INTERSTITIAL_KEY, () -> TapsellPlus.requestInterstitialAd(activity,
                ZONE_INTERSTITIAL,
                new AdRequestCallback() {
                    @Override
                    public void response(TapsellPlusAdModel adModel) {
                        super.response(adModel);
                        interstitialRequested = false;
                        interstitialResponseId = adModel.getResponseId();
                    }

                    @Override
                    public void error(@NonNull String message) {
                        Log.e(TAG, "Interstitial request failed: " + message);
                        interstitialRequested = false;
                    }
                }));
    }

    /** True when an interstitial is loaded and can be shown without a wait. */
    public static boolean isInterstitialReady() {
        return interstitialResponseId != null;
    }

    /**
     * Shows the preloaded interstitial, if there is one, and starts loading the
     * next. Returns whether an ad was handed to the SDK.
     */
    public static boolean showInterstitial(Activity activity) {
        String responseId = interstitialResponseId;
        if (responseId == null || activity.isDestroyed() || activity.isFinishing()) {
            return false;
        }
        interstitialResponseId = null;
        TapsellPlus.showInterstitialAd(activity, responseId, new AdShowListener() {
            @Override
            public void onClosed(TapsellPlusAdModel adModel) {
                super.onClosed(adModel);
                preloadInterstitial(activity);
            }

            @Override
            public void onError(TapsellPlusErrorModel error) {
                super.onError(error);
                Log.e(TAG, "Interstitial show failed: " + error.toString());
            }
        });
        return true;
    }

    /**
     * Releases the banner in {@code container}. Call from {@code onDestroy} so a
     * finished Activity is not kept alive by the SDK or by a parked request.
     */
    public static void destroyBanner(Activity activity, ViewGroup container) {
        PENDING.remove(container);
        String responseId = RESPONSE_IDS.remove(container);
        if (responseId != null) {
            TapsellPlus.destroyStandardBanner(activity, responseId, container);
        }
    }
}
