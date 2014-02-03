package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.quameterdb.QuameterDbWorkPacket;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class QuameterDbTask extends AsyncTaskBase {
	private SearchDbTask searchDbTask;
	private QuameterTask quameterTask;
	private FileSearch fileSearch;

	public QuameterDbTask(final WorkflowEngine engine, final DaemonConnection daemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch, final SearchDbTask searchDbTask, final QuameterTask quameterTask, final FileSearch fileSearch) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.searchDbTask = searchDbTask;
		this.fileSearch = fileSearch;
		this.quameterTask = quameterTask;
		setName("quameter");
		setDescription("Load QuaMeter metadata from " + fileTokenFactory.fileToTaggedDatabaseToken(fileSearch.getInputFile()));
	}

	@Override
	public WorkPacket createWorkPacket() {
		Preconditions.checkNotNull(searchDbTask, "Unset search-db task");
		final Map<String, Integer> metadata = searchDbTask.getLoadedTandemFileMetadata();
		Preconditions.checkNotNull(metadata, "Search-db task did not produce raw file metadata");
		Preconditions.checkNotNull(fileSearch, "Input file not set");
		Preconditions.checkNotNull(quameterTask, "QuaMeter task not set");
		final String fileName = FileUtilities.getFileNameWithoutExtension(fileSearch.getInputFile());
		final Integer tandemMassSpectrometrySampleId = metadata.get(fileName);
		Preconditions.checkNotNull(tandemMassSpectrometrySampleId, "There must be sample id recorded for file name [" + fileName + "]");
		return new QuameterDbWorkPacket(getFullId(),
				isFromScratch(),
				tandemMassSpectrometrySampleId,
				fileSearch.getId(),
				quameterTask.getResultingFile()
		);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(searchDbTask, quameterTask, fileSearch.getId());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuameterDbTask other = (QuameterDbTask) obj;
		return Objects.equal(this.searchDbTask, other.searchDbTask)
				&& Objects.equal(this.quameterTask, other.quameterTask)
				&& Objects.equal(this.fileSearch.getId(), other.fileSearch.getId());
	}
}
