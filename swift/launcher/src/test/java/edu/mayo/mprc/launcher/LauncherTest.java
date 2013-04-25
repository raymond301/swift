package edu.mayo.mprc.launcher;

import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class LauncherTest {
	private File configFile;

	@BeforeTest
	public void setup() throws IOException {
		configFile = TestingUtilities.getTempFileFromResource(LauncherTest.class, "/edu/mayo/mprc/launcher/test.conf", true, null);
	}

	@AfterTest
	public void teardown() {
		FileUtilities.cleanupTempFile(configFile);
	}

	@Test
	public void testGetDaemonConfig() throws Exception {
		final DaemonConfig main = Launcher.getDaemonConfig(configFile, "main");
		Assert.assertEquals(main.getResources().size(), 1);
	}

	@Test
	public void testGetTempFolder() throws Exception {
		Assert.assertEquals(Launcher.getTempFolder(configFile, "main"), new File("/tmp/main"));
	}

	@Test
	public void testGetPortNumber() throws Exception {
		Assert.assertEquals(Launcher.getPortNumber(null, configFile, "main"), 8080);
	}
}