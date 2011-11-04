package updater.patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface PatcherListener {

    void extractProgress(int percentage);

    void extractFinished();

    void patchProgress(int percentage, String message);

    void patchFinished();

    void patchEnableCancel(boolean enable);
}
