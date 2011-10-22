package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    updater.downloader.download.PatchDownloaderTest.class,
    updater.downloader.download.RemoteContentTest.class,
    updater.downloader.util.DownloadProgessUtilTest.class,
    updater.downloader.util.UtilTest.class
})
public class TestSuite {
}
