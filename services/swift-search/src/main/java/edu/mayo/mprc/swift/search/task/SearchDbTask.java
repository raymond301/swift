package edu.mayo.mprc.swift.search.task;

import com.google.common.base.Objects;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.searchdb.SearchDbWorkPacket;
import edu.mayo.mprc.searchdb.builder.RawFileMetaData;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Take Scaffold's spectrum report and loads it into a relational database.
 *
 * @author Roman Zenka
 */
public final class SearchDbTask extends AsyncTaskBase {

	private final Long reportId;
	private final ScaffoldTask scaffoldTask;
	private File scaffoldSpectraFile;

	private final Map<String, RAWDumpTask> rawDumpTaskMap = new HashMap<String, RAWDumpTask>(5);

	/**
	 * Create the task independently on Scaffold invocation.
	 */
	public SearchDbTask(final WorkflowEngine engine, final DaemonConnection daemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch, final Long reportId, final File scaffoldSpectraFile) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.reportId = reportId;
		scaffoldTask = null;
		setScaffoldSpectraFile(scaffoldSpectraFile);
		setName("SearchDb");
		setDescription("Load " + fileTokenFactory.fileToTaggedDatabaseToken(getScaffoldSpectraFile()) + " into database");
	}

	/**
	 * Create the task that depends on Scaffold invocation.
	 */
	public SearchDbTask(final WorkflowEngine engine, final DaemonConnection daemon, final DatabaseFileTokenFactory fileTokenFactory, final boolean fromScratch, final ScaffoldTask scaffoldTask) {
		super(engine, daemon, fileTokenFactory, fromScratch);
		this.scaffoldTask = scaffoldTask;
		reportId = null;
		setName("SearchDb");
		setDescription("Load " + fileTokenFactory.fileToTaggedDatabaseToken(getScaffoldSpectraFile()) + " into database");
	}

	/**
	 * @param task Raw dump task to add to the map. The results are mapped based on file name.
	 */
	public void addRawDumpTask(final RAWDumpTask task) {
		final String fileName = FileUtilities.stripExtension(task.getRawFile().getName());
		if (rawDumpTaskMap.containsKey(fileName)) {
			throw new MprcException("Two files of identical name: " + task.getRawFile().getName() + " cannot be distinguished in resulting analysis.");
		}
		rawDumpTaskMap.put(fileName, task);
	}

	private File getScaffoldSpectraFile() {
		return scaffoldSpectraFile == null ? scaffoldTask.getScaffoldSpectraFile() : scaffoldSpectraFile;
	}

	private File getScaffoldUnimodFile() {
		return scaffoldTask.getUnimod();
	}


	/**
	 * Override the spectra file if the scaffold task is not available.
	 *
	 * @param scaffoldSpectraFile Scaffold spectra file to load.
	 */
	public void setScaffoldSpectraFile(final File scaffoldSpectraFile) {
		this.scaffoldSpectraFile = scaffoldSpectraFile;
	}

	private Long getReportId() {
		return reportId == null ? scaffoldTask.getReportData().getId() : reportId;
	}

	@Override
	public WorkPacket createWorkPacket() {
		final HashMap<String, RawFileMetaData> metaDataMap = new HashMap<String, RawFileMetaData>(rawDumpTaskMap.size());
		for (final Map.Entry<String, RAWDumpTask> entry : rawDumpTaskMap.entrySet()) {
			final RAWDumpTask task = entry.getValue();
			metaDataMap.put(entry.getKey(), task.getRawFileMetadata());
		}

		return new SearchDbWorkPacket(getFullId(), isFromScratch(), getReportId(), getScaffoldSpectraFile(), getScaffoldUnimodFile(), metaDataMap);
	}

	@Override
	public void onSuccess() {
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(reportId, scaffoldTask);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final SearchDbTask other = (SearchDbTask) obj;
		return Objects.equal(reportId, other.reportId) && Objects.equal(scaffoldTask, other.scaffoldTask);
	}
}
