package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.scaffold3.Scaffold3WorkPacket;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldSpectraReader;
import edu.mayo.mprc.scafml.ScafmlScaffold;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class Scaffold3Task extends AsyncTaskBase implements ScaffoldTaskI {

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

	public Scaffold3Task(String experiment, SwiftSearchDefinition definition, DaemonConnection scaffoldDaemon,
	                     SwiftDao swiftDao, SearchRun searchRun,
	                     File outputFolder, FileTokenFactory fileTokenFactory, boolean fromScratch) {
		super(scaffoldDaemon, fileTokenFactory, fromScratch);
		this.experiment = experiment;
		this.swiftSearchDefinition = definition;
		this.outputFolder = outputFolder;
		this.swiftDao = swiftDao;
		this.searchRun = searchRun;
		setName("Scaffold3");
		setDescription("Scaffold 3 search " + this.experiment);
	}

	/**
	 * Which input file/search parameters tuple gets outputs from which engine search.
	 */
	public void addInput(FileSearch fileSearch, EngineSearchTask search) {
		InputFileSearches searches = inputs.get(fileSearch);
		if (searches == null) {
			searches = new InputFileSearches();
			inputs.put(fileSearch, searches);
		}
		searches.addSearch(search);
	}

	public void addDatabase(String id, DatabaseDeployment dbDeployment) {
		databases.put(id, dbDeployment);
	}

	@Override
	public void setReportData(ReportData reportData) {
		this.reportData = reportData;
	}

	@Override
	public ReportData getReportData() {
		return reportData;
	}

	/**
	 * @return Work packet to be sent asynchronously. If it returns null, it means the work was done without a need
	 *         to send a work packet.
	 */
	public WorkPacket createWorkPacket() {
		setDescription("Scaffold 3 search " + this.experiment);
		File scaffoldFile = new File(outputFolder, experiment + ".sf3");
		if (!isFromScratch() && scaffoldFile.exists() && scaffoldFile.isFile() && scaffoldFile.length() > 0) {
			storeReportFile();
			return null;
		}

		for (Map.Entry<String, DatabaseDeployment> entry : databases.entrySet()) {
			if (entry.getValue() == null || entry.getValue().getFastaFile() == null) {
				throw new DaemonException("Scaffold 3 deployer probably returned invalid data - null fasta path for database " + entry.getKey());
			}
		}

		// Sanity check - make sure that Scaffold gets some input files
		if (inputs.size() == 0) {
			throw new DaemonException("There are no files defined for this experiment");
		}

		Map<String, File> fastaFiles = new HashMap<String, File>();
		for (Map.Entry<String, DatabaseDeployment> entry : databases.entrySet()) {
			fastaFiles.put(entry.getKey(), entry.getValue().getFastaFile());
		}
		SearchResults searchResults = new SearchResults();
		for (Map.Entry<FileSearch, InputFileSearches> entry : inputs.entrySet()) {
			FileSearchResult result = new FileSearchResult(entry.getKey().getInputFile());
			for (EngineSearchTask search : entry.getValue().getSearches()) {
				result.addResult(
						search.getSearchEngine().getCode(),
						search.getOutputFile());
			}
			searchResults.addResult(result);
		}

		final ScafmlScaffold scafmlFile = ScafmlDump.dumpScafmlFile(experiment, swiftSearchDefinition, inputs, outputFolder, searchResults, fastaFiles);
		scafmlFile.setVersionMajor(3);
		scafmlFile.setVersionMinor(0);
		return new Scaffold3WorkPacket(
				outputFolder,
				scafmlFile,
				this.experiment,
				getFullId(),
				isFromScratch());
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

	@Override
	public File getScaffoldSpectraFile() {
		return new File(outputFolder, experiment + ScaffoldSpectraReader.EXTENSION);
	}

	public void onSuccess() {
		completeWhenFileAppears(getResultingFile());
		completeWhenFileAppears(getScaffoldSpectraFile());
		storeReportFile();
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

	public void onProgress(ProgressInfo progressInfo) {
	}

}
