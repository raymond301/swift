package edu.mayo.mprc.dbcurator.model.curationsteps;

import com.google.common.base.Objects;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.dbcurator.model.CurationExecutor;
import edu.mayo.mprc.dbcurator.model.CurationStep;
import edu.mayo.mprc.dbcurator.model.StepValidation;
import edu.mayo.mprc.fasta.DBInputStream;
import edu.mayo.mprc.fasta.DBOutputStream;
import edu.mayo.mprc.fasta.FASTAInputStream;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.io.IOException;

/**
 * @author Eric Winter
 */
public class DatabaseUploadStep implements CurationStep {
	private static final long serialVersionUID = 20071220L;

	/**
	 * where on the server can the file be found
	 */
	private File pathToUploadedFile;

	/**
	 * the name of the file that was selected by the user to be shown when the curation is displayed again
	 */
	private String fileName;

	/**
	 * the number of sequences present when this step was last run
	 */
	private Integer lastRunCompletionCount;

	public DatabaseUploadStep() {

	}

	/**
	 * For testing only. Fast creation of a step.
	 */
	public DatabaseUploadStep(final File pathToUploadedFile, final String fileName, final Integer lastRunCompletionCount) {
		this.pathToUploadedFile = pathToUploadedFile;
		this.fileName = fileName;
		this.lastRunCompletionCount = lastRunCompletionCount;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * this takes the uploaded file and appends it to the curation
	 *
	 * @param exec the CurationExecutor that is running this step
	 * @return the StepValidation describing the progress through this step
	 */
	@Override
	public StepValidation performStep(final CurationExecutor exec) {
		final StepValidation recentRunValidation = preValidate(new StepValidation());
		//if the prevalidation already failed then return the failed validation
		if (!recentRunValidation.isOK()) {
			return recentRunValidation;
		}

		//get the data we will need
		final DBInputStream in = exec.getCurrentInStream(); //the file we should be reading from (may be null)
		final DBOutputStream out = exec.getCurrentOutStream(); // the file we should be writing to

		// take the data from the input file and copy it to the output file.  We are not doing any filtering
		//  during this step so very one will be copied if any exist.
		if (in != null) {
			try {
				in.beforeFirst();
				out.appendRemaining(in);
			} catch (final IOException e) {
				recentRunValidation.addMessageAndException("Error copying in stream to out stream", e);
			}
		}

		// we want to create a new DBInputStream from the archive file and copy the archive file into the output stream
		DBInputStream archiveIn = null;
		try {
			try {
				archiveIn = new FASTAInputStream(pathToUploadedFile);
			} catch (final Exception e) {
				//this is not expected to happen
				recentRunValidation.addMessageAndException("Could not find the file on the server please re-upload", e);
			}

			//next if we have an archive setup then we want to copy it to the output stream
			if (archiveIn != null) {
				//if we are the first step then there is no need to copy the file over we just need to pass a reference
				//to the file to the next step.  This will eliminate some time in copying a large file.
				try {
					archiveIn.beforeFirst();
					out.appendRemaining(archiveIn);
				} catch (final IOException e) {
					recentRunValidation.addMessageAndException("Error copying archive to output file", e);
					return recentRunValidation;
				}
			} else {
				recentRunValidation.addMessage("Error finding the input file");
			}

		} finally {
			FileUtilities.closeQuietly(archiveIn);
		}

		recentRunValidation.setCompletionCount(out.getSequenceCount());
		setLastRunCompletionCount(out.getSequenceCount());

		return recentRunValidation;
	}

	@Override
	public StepValidation preValidate(final CurationDao curationDao) {
		return preValidate(new StepValidation());
	}

	/**
	 * if the file has not been uploaded
	 *
	 * @param toValidateInto
	 * @return
	 */
	private StepValidation preValidate(final StepValidation toValidateInto) {
		if (getPathToUploadedFile() == null) {
			toValidateInto.addMessage("No file has been uploaded");
		}

		if (!(getPathToUploadedFile().exists())) {
			toValidateInto.addMessage("The file can no longer be found on the server please re-upload");
		}
		return toValidateInto;
	}

	@Override
	public CurationStep createCopy() {
		final DatabaseUploadStep copy = new DatabaseUploadStep();
		copy.setPathToUploadedFile(pathToUploadedFile);
		copy.setFileName(getFileName());
		return copy;
	}

	@Override
	public Integer getLastRunCompletionCount() {
		return lastRunCompletionCount;
	}

	@Override
	public void setLastRunCompletionCount(final Integer count) {
		lastRunCompletionCount = count;
	}

	/**
	 * get the path on the server where the file uploaded to
	 *
	 * @return the file on the server
	 */
	public File getPathToUploadedFile() {
		return pathToUploadedFile;
	}

	/**
	 * set the path on the server where the uploaded file was located
	 *
	 * @param pathToUploadedFile the path on the server where the uploaded file was located
	 */
	public void setPathToUploadedFile(final File pathToUploadedFile) {
		this.pathToUploadedFile = pathToUploadedFile;
	}

	/**
	 * get the file that was on the client machine that they uploaded
	 *
	 * @return the file that was on the client machine that they uploaded
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * set the file that was on the client machine that they uploaded
	 *
	 * @param fileName the file that was on the client machine that they uploaded
	 */
	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	@Override
	public String simpleDescription() {
		return "Upload " + getFileName();
	}

    @Override
    public String getStepTypeName() {
        return "database_upload";
    }

	@Override
	public int hashCode() {
		return Objects.hashCode(pathToUploadedFile, fileName, lastRunCompletionCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final DatabaseUploadStep other = (DatabaseUploadStep) obj;
		return Objects.equal(this.pathToUploadedFile, other.pathToUploadedFile) && Objects.equal(this.fileName, other.fileName) && Objects.equal(this.lastRunCompletionCount, other.lastRunCompletionCount);
	}
}
