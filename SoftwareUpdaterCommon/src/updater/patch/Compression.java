package updater.patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum Compression {

    GZIP(0), LZMA2(1);
    protected final int value;

    Compression(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Compression getCompression(int value) {
        Compression[] compressions = Compression.values();
        for (Compression compression : compressions) {
            if (compression.getValue() == value) {
                return compression;
            }
        }
        return null;
    }
}
