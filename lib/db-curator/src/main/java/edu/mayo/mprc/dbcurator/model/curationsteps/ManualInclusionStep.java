package edu.mayo.mprc.dbcurator.model.curationsteps;

import com.google.common.base.Objects;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.dbcurator.model.CurationExecutor;
import edu.mayo.mprc.dbcurator.model.CurationStep;
import edu.mayo.mprc.dbcurator.model.StepValidation;
import edu.mayo.mprc.fasta.DBInputStream;
import edu.mayo.mprc.fasta.DBOutputStream;

import java.io.IOException;

/**
 * A CurationStep that takes the database and manually adds a FASTA format sequence to it The user can either enter a
 * header and a sequence or optionally just pass in a header/sequence combination String.  If the entered String is
 * invalid or malformed then it will just sit in the sequence field and any calls to preValidate() will indicate how the
 * sequence is malformed.
 *
 * @author Eric J. Winter Date: Apr 10, 2007 Time: 11:46:11 AM
 */
public class ManualInclusionStep implements CurationStep {
	private static final long serialVersionUID = 20071220L;

	/**
	 * the header of the sequence (may be null if no valid header was detected
	 */
	private String header;

	/**
	 * the sequence that was entered.  This may contain a malformed header/footer combination
	 */
	private String sequence;

	/**
	 * CTor that takes the header and the sequence in seperate fields.  The header does not need to contain a '>' in the
	 * first character position.
	 */
	public ManualInclusionStep() {
		super();
	}

	/**
	 * For testing only. Fast creation of a step.
	 */
	public ManualInclusionStep(final String header, final String sequence, final Integer lastRunCompletionCount) {
		this.header = header;
		this.sequence = sequence;
		this.lastRunCompletionCount = lastRunCompletionCount;
	}

	/**
	 * perfom the step on a given local database.  If the step could not be performed then a CurationStepException is
	 * thrown.  This indicates that the PostValidation will be unsuccessful and will contain a message indicating why it
	 * was unsuccesfull.
	 * <p/>
	 * There are obviously a wide variety of things that could go wrong with a call to perform step.
	 *
	 * @param exe the executor we are performing the step for
	 * @return the post validation.
	 */
	@Override
	public StepValidation performStep(final CurationExecutor exe) {
		StepValidation runValidation = preValidate(exe.getCurationDao());

		//run a prevalidation before continuing.  If prevalidation fails return that StepValidation else create a new one
		//for the post validation
		if (!runValidation.isOK()) {
			return runValidation;
		}

		runValidation = new StepValidation();

		final DBInputStream in = exe.getCurrentInStream();
		final DBOutputStream out = exe.getCurrentOutStream();

		try {
			if (in != null) {
				in.beforeFirst();
				out.appendRemaining(in);
			}
			out.appendSequence(getHeader(), getSequence());
		} catch (final IOException e) {
			runValidation.addMessageAndException("Error writing manual inclusion: " + getHeader(), e);
			return runValidation;
		}

		runValidation.setCompletionCount(out.getSequenceCount());
		setLastRunCompletionCount(out.getSequenceCount());

		return runValidation;
	}

	/**
	 * Call this method if you want to see if the step is ready to be run and if any issues have been predicted.  NOTE:
	 * succesfull prevalidation can not guarentee<sp> successful processing.
	 *
	 * @param curationDao
	 * @return the @see StepValidation to interrogate for issues
	 */
	@Override
	public StepValidation preValidate(final CurationDao curationDao) {
		final StepValidation preValidation = new StepValidation();

		if (sequence == null) {
			preValidation.addMessage("No sequence has been entered.");
			return preValidation;
		}

		sequence = sequence.trim();

		if (sequence.equals("")) {
			preValidation.addMessage("No sequence has been entered.");
		}

		//if the header is still null after sequence validation then we need to get one
		if (header == null) {
			preValidation.addMessage("No header has been entered.");
		} else {
			header = header.trim();
			if (header.equals("")) {
				preValidation.addMessage("No header has been entered.");
			}
		}

		return preValidation;
	}

	/**
	 * Creates a copy of this step.  Only persistent properties are included in the copy.
	 *
	 * @return a cropy of this step
	 */
	@Override
	public CurationStep createCopy() {
		final ManualInclusionStep copy = new ManualInclusionStep();
		copy.setHeader(header);
		copy.setSequence(sequence);
		return copy;
	}

	/**
	 * the FASTA header (with leading '>') that this step will insert
	 */
	public String getHeader() {
		return header;
	}

	public void setHeader(final String header) {
		this.header = header;
	}

	/**
	 * the sequence that this step will insert
	 */
	public String getSequence() {
		return sequence;
	}

	public void setSequence(final String sequence) {
		this.sequence = sequence;
	}

	/**
	 * the number of sequences that were present in the curation after this step was last run
	 */
	private Integer lastRunCompletionCount = null;

	@Override
	public Integer getLastRunCompletionCount() {
		return lastRunCompletionCount;
	}

	@Override
	public void setLastRunCompletionCount(final Integer count) {
		lastRunCompletionCount = count;
	}

	@Override
	public String simpleDescription() {
		return "user sequence";
	}

    @Override
    public String getStepTypeName() {
        return "manual_inclusion";
    }

	@Override
	public int hashCode() {
		return Objects.hashCode(header, sequence, lastRunCompletionCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final ManualInclusionStep other = (ManualInclusionStep) obj;
		return Objects.equal(this.header, other.header) && Objects.equal(this.sequence, other.sequence) && Objects.equal(this.lastRunCompletionCount, other.lastRunCompletionCount);
	}
}
