package edu.mayo.mprc.database;

import java.io.File;

/**
 * @author Roman Zenka
 */
public class TestFile extends PersistableBase {
	private File file1;
	private File file2;

	public TestFile() {
	}

	public TestFile(File file1, File file2) {
		this.file1 = file1;
		this.file2 = file2;
	}

	public File getFile1() {
		return file1;
	}

	public void setFile1(File file1) {
		this.file1 = file1;
	}

	public File getFile2() {
		return file2;
	}

	public void setFile2(File file2) {
		this.file2 = file2;
	}
}
