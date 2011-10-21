package updater.launcher.patch;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface PatcherListener {

    void patchProgress(int percentage, String message);

    void patchFinished(boolean succeed);

    void patchEnableCancel(boolean enable);
}
