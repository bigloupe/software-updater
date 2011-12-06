package updater.patch;

/**
 * Enum for specifying the compression method used by the patch.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public enum Compression {

  GZIP(0), LZMA2(1);
  /**
   * The integer value representation of the compression.
   */
  protected final int value;

  Compression(int value) {
    this.value = value;
  }

  /**
   * Get the unique integer representation for this type of compression..
   * @return the integer value
   */
  public int getValue() {
    return value;
  }

  /**
   * Get the {@link updater.patch.Compression} by the compressions' integer 
   * value.
   * @param value the integer value
   * @return the {@link updater.patch.Compression} or null if not 
   * correspondent found
   */
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
