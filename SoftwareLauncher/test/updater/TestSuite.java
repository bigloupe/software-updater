package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    updater.launcher.SoftwareStarterTest.class,
    updater.launcher.patch.PatchLogTest.class
})
public class TestSuite {
}
