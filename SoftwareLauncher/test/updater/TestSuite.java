package updater;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Chan Wai Shing <cws1989@gmail.com>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
  updater.launcher.BatchPatcherTest.class,
  updater.launcher.SoftwareStarterTest.class
})
public class TestSuite {
}
