package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.scaffold.ScaffoldWorkPacket;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraReader;
import edu.mayo.mprc.scafml.ScafmlScaffold;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.util.*;

final class ScaffoldTask extends AsyncTaskBase implements ScaffoldTaskI {

	private String scaffoldVersion;
	private String experiment;

	/**
	 * Key: Input file search specification.
	 * Value: List of searches performed on the file.
	 */
	private LinkedHashMap<FileSearch, InputFileSearches> inputs = new LinkedHashMap<FileSearch, InputFileSearches>();
	/**
	 * Key: Name of the database
	 * Value: The task that deployed the database
	 */
	private Map<String, DatabaseDeployment> databases = new HashMap<String, DatabaseDeployment>();
	private final File outputFolder;
	private final SwiftSearchDefinition swiftSearchDefinition;
	private ReportData reportData;
	private final SwiftDao swiftDao;
	private final SearchRun searchRun;
	private final File unimod;
	private final boolean reportDecoyHits;
	private final Object lock = new Object();

	public ScaffoldTask(final WorkflowEngine engine,
	                    final String scaffoldVersion, final String experiment, final SwiftSearchDefinition definition, final DaemonConnection scaffoldDaemon,
	                    final SwiftDao swiftDao, final SearchRun searchRun, final File scaffoldUnimod,
	                    final File outputFolder, final FileTokenFactory fileTokenFactory, final boolean reportDecoyHits, final boolean fromScratch) {
		super(engine, scaffoldDaemon, fileTokenFactory, fromScratch);
		this.scaffoldVersion = scaffoldVersion;
		this.experiment = experiment;
		this.swiftSearchDefinition = definition;
		this.outputFolder = outputFolder;
		this.swiftDao = swiftDao;
		this.searchRun = searchRun;
		this.unimod = scaffoldUnimod;
		this.reportDecoyHits = reportDecoyHits;
		setName("Scaffold");
		setDescription("Scaffold search " + this.experiment);
	}

	/**
	 * Which input file/search parameters tuple gets outputs from which engine search.
	 */
	public void addInput(final FileSearch fileSearch, final EngineSearchTask search) {
		InputFileSearches searches = inputs.get(fileSearch);
		if (searches == null) {
			searches = new InputFileSearches();
			inputs.put(fileSearch, searches);
		}
		searches.addSearch(search);
	}

	public void addDatabase(final String id, final DatabaseDeployment dbDeployment) {
		databases.put(id, dbDeployment);
	}

	@Override
	public void setReportData(final ReportData reportData) {
		// Data is being set asynchronously after the file appears
		synchronized (lock) {
			this.reportData = reportData;
		}
	}

	@Override
	public ReportData getReportData() {
		synchronized (lock) {
			return reportData;
		}
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	public WorkPacket createWorkPacket() {
		setDescription("Scaffold search " + this.experiment);
		final File scaffoldFile = new File(outputFolder, experiment + ".sf3");

		for (final Map.Entry<String, DatabaseDeployment> entry : databases.entrySet()) {
			if (entry.getValue() == null || entry.getValue().getFastaFile() == null) {
				throw new DaemonException("Scaffold deployer probably returned invalid data - null fasta path for database " + entry.getKey());
			}
		}

		// Sanity check - make sure that Scaffold gets some input files
		if (inputs.size() == 0) {
			throw new DaemonException("There are no files defined for this experiment");
		}

		final Map<String, File> fastaFiles = new HashMap<String, File>();
		for (final Map.Entry<String, DatabaseDeployment> entry : databases.entrySet()) {
			fastaFiles.put(entry.getKey(), entry.getValue().getFastaFile());
		}
		final SearchResults searchResults = new SearchResults();
		for (final Map.Entry<FileSearch, InputFileSearches> entry : inputs.entrySet()) {
			final FileSearchResult result = new FileSearchResult(entry.getKey().getInputFile());
			for (final EngineSearchTask search : entry.getValue().getSearches()) {
				result.addResult(
						search.getSearchEngine().getCode(),
						search.getOutputFile());
			}
			searchResults.addResult(result);
		}

		final ScafmlScaffold scafmlFile = ScafmlDump.dumpScafmlFile(scaffoldVersion, experiment, swiftSearchDefinition, inputs, outputFolder, searchResults, fastaFiles);
		scafmlFile.setVersionMajor(3);
		scafmlFile.setVersionMinor(0);
		scafmlFile.getExperiment().setReportDecoyHits(reportDecoyHits);
		final ScaffoldWorkPacket workPacket = new ScaffoldWorkPacket(
				outputFolder,
				scafmlFile,
				this.experiment,
				getFullId(),
				isFromScratch());

		if (isScaffoldValid(workPacket, scaffoldFile)) {
			storeReportFile();
			return null;
		}
		return workPacket;

	}

	/**
	 * @param workPacket   Work packet that is meant to re-create the existing Scaffold file.
	 * @param scaffoldFile Scaffold file to test.
	 * @return True if the file is valid - older than all its inputs, matches the input parameters.
	 */
	private boolean isScaffoldValid(final ScaffoldWorkPacket workPacket, final File scaffoldFile) {
		if (isFromScratch()) {
			return false;
		}

		final List<String> outputFiles = workPacket.getOutputFiles();
		return !workPacket.cacheIsStale(workPacket.getOutputFolder(), outputFiles);
	}

	@Override
	public String getScaffoldVersion() {
		return "3";
	}

	@Override
	public File getResultingFile() {
		return new File(outputFolder, experiment + ".sf3");
	}

	@Override
	public File getScaffoldXmlFile() {
		return new File(outputFolder, experiment + ".xml");
	}

	@Override
	public File getScaffoldPeptideReportFile() {
		return new File(outputFolder, experiment + ".peptide-report.xls");
	}

	File getUnimod() {
		return unimod;
	}

	@Override
	public File getScaffoldSpectraFile() {
		return new File(outputFolder, experiment + ScaffoldSpectraReader.EXTENSION);
	}

	public void onSuccess() {
		setWaitForFiles();
		// Store Scaffold report before we announce success
		FileUtilities.waitForFile(getResultingFile(), new FileListener() {
			@Override
			public void fileChanged(Collection<File> files, boolean timeout) {
				// This gets called from a different thread
				storeReportFile();
				completeWhenFilesAppear(getScaffoldSpectraFile());
			}
		});
	}

	/**
	 * Store information into the database that we produced a particular report file.
	 * This has to happen whenever Scaffold successfully finished (be it because it ran,
	 * or if it was done previously).
	 */
	private void storeReportFile() {
		swiftDao.begin();
		try {
			// Scaffold finished. Store the resulting file.
			setReportData(swiftDao.storeReport(searchRun.getId(), getResultingFile()));
			swiftDao.commit();
		} catch (Exception t) {
			swiftDao.rollback();
			throw new MprcException("Could not store change in task information", t);
		}
	}

	public void onProgress(final ProgressInfo progressInfo) {
	}

}
