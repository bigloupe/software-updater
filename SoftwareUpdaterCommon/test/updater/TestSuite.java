package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    updater.crypto.AESKeyTest.class,
    updater.crypto.KeyGeneratorTest.class,
    updater.crypto.RSAKeyTest.class,
    updater.patch.PatchLogTest.class,
    updater.patch.PatchTest.class,
    updater.script.ScriptTest.class,
    updater.util.CommonUtilTest.class,
    updater.util.DownloadProgressUtilTest.class
})
public class TestSuite {
}
