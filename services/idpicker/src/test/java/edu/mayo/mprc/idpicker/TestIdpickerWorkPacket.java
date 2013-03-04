package edu.mayo.mprc.idpicker;

import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class TestIdpickerWorkPacket {
	private File tempFolder;

	@BeforeClass
	public void startup() {
		tempFolder = FileUtilities.createTempFolder();
	}

	@AfterClass
	public void teardown() {
		FileUtilities.cleanupTempFile(tempFolder);
	}

	@Test
	public void shouldProduceProperDescription() {

		final File input = new File(tempFolder, "input.pepXML");
		final File output = new File(tempFolder, "output.idp");
		final IdpQonvertSettings settings = new IdpQonvertSettings();

		final File fasta = new File(tempFolder, "test.fasta");
		final IdpickerWorkPacket packet = new IdpickerWorkPacket(
				output, settings, input, fasta, "Idpicker-1", false
		);

		final String desc = packet.getStringDescriptionOfTask();
		Assert.assertTrue(desc.contains("ChargeStateHandling=\"Partition\"\n"), "Packet does not serialize properly");
		Assert.assertTrue(desc.contains(input.getAbsolutePath()), "Packet does not serialize properly");
		Assert.assertTrue(desc.contains(output.getAbsolutePath()), "Packet does not serialize properly");
		Assert.assertTrue(desc.contains(fasta.getAbsolutePath()), "Packet does not serialize properly");
	}
}