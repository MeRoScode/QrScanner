package ir.meros.qrscanner.model;

/**
 * One entry in the QR history: either a code the user scanned or one they
 * generated. Persisted as JSON by {@code HistoryRepository}, so it is a plain
 * data holder with a no-arg constructor for Gson.
 */
public class QrItem {

    private String content;
    private QrType type;
    private boolean generated;
    private long timestamp;

    public QrItem() {
    }

    public QrItem(String content, QrType type, boolean generated, long timestamp) {
        this.content = content;
        this.type = type;
        this.generated = generated;
        this.timestamp = timestamp;
    }

    /** Builds an item, auto-detecting the type from {@code content}. */
    public static QrItem of(String content, boolean generated) {
        return new QrItem(content, QrType.detect(content), generated, System.currentTimeMillis());
    }

    public String getContent() {
        return content;
    }

    public QrType getType() {
        return type != null ? type : QrType.TEXT;
    }

    public boolean isGenerated() {
        return generated;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /** Two items are "the same" for de-duplication if content + origin match. */
    public boolean sameAs(QrItem other) {
        return other != null
                && generated == other.generated
                && content != null
                && content.equals(other.content);
    }
}
