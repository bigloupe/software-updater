package updater.util;

/**
 * Support pause and resume action.
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
public interface Pausable {

    /**
     * Pause or resume the action.
     * @param pause true to pause, false to resume
     */
    void pause(boolean pause);
}
