package ir.meros.qrscanner.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ir.meros.qrscanner.model.QrItem;

/**
 * Local, offline store for the QR history. Backed by {@link SharedPreferences}
 * with the list serialized as JSON (Gson) — no database dependency needed for a
 * small, single-user list. Newest items are kept first and the list is capped.
 */
public class HistoryRepository {

    private static final String PREFS = "qr_history";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 200;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<QrItem>>() {
    }.getType();

    public HistoryRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Returns the history, newest first. Never null. */
    public List<QrItem> load() {
        String json = prefs.getString(KEY_ITEMS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        List<QrItem> items = gson.fromJson(json, listType);
        return items != null ? items : new ArrayList<>();
    }

    /**
     * Prepends {@code item}, removing any earlier duplicate so re-scanning moves
     * an entry to the top instead of piling up. Returns the updated list.
     */
    public List<QrItem> add(QrItem item) {
        List<QrItem> items = load();
        for (Iterator<QrItem> it = items.iterator(); it.hasNext(); ) {
            if (it.next().sameAs(item)) {
                it.remove();
            }
        }
        items.add(0, item);
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        persist(items);
        return items;
    }

    public List<QrItem> delete(QrItem item) {
        List<QrItem> items = load();
        for (Iterator<QrItem> it = items.iterator(); it.hasNext(); ) {
            QrItem existing = it.next();
            if (existing.getTimestamp() == item.getTimestamp() && existing.sameAs(item)) {
                it.remove();
            }
        }
        persist(items);
        return items;
    }

    public void clear() {
        prefs.edit().remove(KEY_ITEMS).apply();
    }

    private void persist(List<QrItem> items) {
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items, listType)).apply();
    }
}
