package ir.meros.qrscanner.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ir.meros.qrscanner.data.HistoryRepository;
import ir.meros.qrscanner.model.QrItem;

import java.util.List;

/**
 * Backs the main screen with the QR history. All persistence goes through
 * {@link HistoryRepository}; the Activity only observes {@link #getHistory()}
 * and forwards user intent (scan saved, item deleted, cleared).
 */
public class MainViewModel extends AndroidViewModel {

    private final HistoryRepository repository;
    private final MutableLiveData<List<QrItem>> history = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = new HistoryRepository(application);
        reload();
    }

    public LiveData<List<QrItem>> getHistory() {
        return history;
    }

    /** Re-reads from storage — call when returning to the screen. */
    public void reload() {
        history.setValue(repository.load());
    }

    /** Records a scanned code and refreshes the list. */
    public QrItem recordScan(String content) {
        QrItem item = QrItem.of(content, false);
        history.setValue(repository.add(item));
        return item;
    }

    public void delete(QrItem item) {
        history.setValue(repository.delete(item));
    }

    public void clear() {
        repository.clear();
        reload();
    }
}
