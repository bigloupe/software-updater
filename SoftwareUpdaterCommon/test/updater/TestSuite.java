package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    updater.concurrent.LockUtilTest.class,
    updater.crypto.AESKeyTest.class,
    updater.crypto.KeyGeneratorTest.class,
    updater.crypto.RSAKeyTest.class,
    updater.patch.PatchLogTest.class,
    updater.patch.PatchTest.class,
    updater.script.ScriptTest.class,
    updater.util.CommonUtilTest.class,
    updater.util.DownloadProgressUtilTest.class,
    updater.util.HTTPDownloaderTest.class
})
public class TestSuite {
}
