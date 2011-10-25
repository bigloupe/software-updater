package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    updater.patch.PatchLogTest.class,
    updater.script.ScriptTest.class,
    updater.util.CommonUtilTest.class
})
public class TestSuite {
}
