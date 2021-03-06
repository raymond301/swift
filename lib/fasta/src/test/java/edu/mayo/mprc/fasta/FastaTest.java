package edu.mayo.mprc.fasta;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.integration.Installer;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A class to test the FASTAInputStream and FASTAOuputStream classes
 *
 * @author Eric J. Winter Date: Apr 19, 2007
 */
public final class FastaTest {
	private File fastaFileFolder;

	@BeforeClass
	public void installFiles() {
		fastaFileFolder = Installer.testFastaFiles(null, Installer.Action.INSTALL);
	}

	@AfterClass
	public void cleanupFiles() {
		Installer.testFastaFiles(fastaFileFolder, Installer.Action.UNINSTALL);
	}

	@Test
	public void testInputAndOutput() throws IOException {
		final File inFile = new File(fastaFileFolder, "test_in.fasta");
		final File outFile = File.createTempFile("test_out", ".fasta");

		Assert.assertEquals(FASTAInputStream.isFASTAFileValid(inFile, true), null, "No error should be detected");

		final DBInputStream in = new FASTAInputStream(inFile);
		final DBOutputStream out = new FASTAOutputStream(outFile);

		in.beforeFirst();

		Assert.assertTrue(in.gotoNextSequence());

		final String header = in.getHeader();
		Assert.assertEquals(header, ">Q4U9M9|104K_THEAN 104 kDa microneme-rhoptry antigen precursor (p104) - Theileria annulata");
		final String sequence = in.getSequence();

		// Drop the > in header. It will be re-added.
		out.appendSequence(header.substring(1), sequence);
		out.appendRemaining(in);
		Assert.assertEquals(out.getFile(), outFile);

		out.close();
		in.close();

		assertTrue(outFile.exists());
		assertEquals(out.getSequenceCount(), 7);
		Assert.assertNull(TestingUtilities.compareFilesByLine(inFile, outFile));

		FileUtilities.cleanupTempFile(outFile);
	}

	@Test
	public void testInputAndOutputZipped() throws IOException {
		final File inFile = new File(fastaFileFolder, "test_in.fasta.gz");
		Assert.assertEquals(FASTAInputStream.isFASTAFileValid(inFile, true), null, "No errors should be found");
		assertTrue(inFile.exists());

		final File outFile = File.createTempFile("test_out", ".fasta");

		final DBInputStream in = new FASTAInputStream(inFile);
		final DBOutputStream out = new FASTAOutputStream(outFile);

		in.beforeFirst();
		out.appendRemaining(in);

		out.close();
		in.close();

		assertTrue(inFile.exists());
		assertTrue(outFile.exists());
		assertEquals(out.getSequenceCount(), 7);

		FileUtilities.cleanupTempFile(outFile);
	}

	@Test
	public void testEmptyAccnum() throws IOException {
		final File malformedFile = createTempFileWithString(">OKAY desc\nGGGGG\n> empty accnum\nSEQUENCE\n");
		Assert.assertEquals(FASTAInputStream.isFASTAFileValid(malformedFile, true), "Empty accession number (line #3)", "Should fail");
		FileUtilities.cleanupTempFile(malformedFile);
	}

	private File createTempFileWithString(final String contents) {
		final File malformedFile;
		try {
			malformedFile = File.createTempFile("malformed", ".fasta");
		} catch (final IOException e) {
			throw new MprcException("Could not create temp file", e);
		}
		FileUtilities.writeStringToFile(malformedFile, contents, true);
		return malformedFile;
	}

	@Test
	public void shouldDetectDuplicateAccnums() throws IOException {
		final File inFile = new File(fastaFileFolder, "test_in_dups.fasta");
		final String errorMessage = FASTAInputStream.isFASTAFileValid(inFile, true);
		Assert.assertTrue(errorMessage.contains("Q4U9M9|104K_THEAN"), "Message [" + errorMessage + "] must mention duplicate accnum");
		Assert.assertTrue(errorMessage.toLowerCase(Locale.US).contains("duplicate"), "Must mention duplicity: " + errorMessage);
	}

	@Test
	public void shouldDetectLongAccnum() {
		final File malformedFile = createTempFileWithString(">ABCDE012345678901234567890123456789 desc\nGGGGG\n");
		Assert.assertEquals(FASTAInputStream.isFASTAFileValid(malformedFile, true), "Accession number too long: [ABCDE012345678901234567890123456789]. Length: 35, max: 34 (line #1)", "Should fail");
		FileUtilities.cleanupTempFile(malformedFile);
	}

	@Test
	public void shouldGetAccnum() {
		Assert.assertEquals(FASTAInputStream.getAccNum(">hello world"), "hello");
		Assert.assertEquals(FASTAInputStream.getAccNum(">hello_world"), "hello_world");
	}

	@Test
	public void shouldCheckHeaders() {
		assertErrorContains(">HELLO_123456789012345678901234567890 Too long accnum", "too long");
		assertErrorContains(">12345678901234567890123456789012345 Too long accnum", "too long");
		assertErrorContains(">strange<>*/chars Not-supported characters", "invalid");
		assertErrorContains(">abcABC0129|_+*. Ok", null);
		assertErrorContains(">M99641|IGHV1-18*01|98-AA|98+0=98| Ok", null);
		assertErrorContains(">LONG_DESC " + Strings.repeat("0123456789", 20), "too long");
		assertErrorContains(">LONG_DESC " + Strings.repeat("X", 1 + 200 - ">LONG_DESC ".length()), null);
		assertErrorContains(">LONG_DESC " + Strings.repeat("X", 1 + 200 - ">LONG_DESC ".length() + 1), "too long");
	}

	@Test
	public void shouldCleanHeaders() {
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(null, 5), "");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcde", 5), ">abcde");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcdef", 5), ">a...f");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcdefg", 5), ">a...g");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcdefgh", 5), ">a...h");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcdefgh", 6), ">ab...h");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcdefgh", 7), ">ab...gh");
		Assert.assertEquals(FASTAOutputStream.cleanupHeader(">abcdefghi", 8), ">abc...hi");
	}

	private void assertErrorContains(final String sequence, final String error) {
		final String actualError = FASTAInputStream.checkHeader(sequence, FASTAInputStream.getAccNum(sequence), true);
		if (actualError == null) {
			Assert.assertNull(error, "No error was produced while expected '" + error + "'");
			return;
		}
		Assert.assertTrue(error != null && actualError.toLowerCase().contains(error.toLowerCase()), "error must mention '" + error + "', was '" + actualError + "'");
	}
}
