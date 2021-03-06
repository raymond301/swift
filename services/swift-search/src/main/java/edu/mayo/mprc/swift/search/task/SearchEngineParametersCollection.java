package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of all parameters anyone has given to our search engines to process.
 * <p/>
 * For each set of parameters and engine it is capable of producing a file name of file that contains
 * the search engine parameters, serialized to the disk.
 * <p/>
 * A parameters folder is created for saving these files. The tool makes sure that if same engine
 * needs multiple different parameter sets, the file names will not collide and the files will appear in the order
 * in which the search was requested (==deterministic)
 *
 * @author Roman Zenka
 */
public final class SearchEngineParametersCollection {
	private static final String DEFAULT_PARAMS_FOLDER = "params";

	/**
	 * Key: Saved parameter set string
	 * Value: A string uniquely identifying the parameter set.
	 * <p/>
	 * For the first parameter set, the string is "".
	 * Second parameter set gets '2', and so on.
	 */
	private Map<String, String> parameterSuffix;
	/**
	 * Key: {@link #getParamFileHash(SearchEngine, String)}
	 * Value: parameter file name
	 */
	private final Map<String, File> parameterFiles;
	private final ParamsInfo paramsInfo;
	private final File paramsFolder;

	public SearchEngineParametersCollection(final File outputFolder, final ParamsInfo paramsInfo) {
		this.paramsInfo = paramsInfo;
		paramsFolder = new File(outputFolder, DEFAULT_PARAMS_FOLDER);
		parameterSuffix = new HashMap<String, String>(10);
		parameterFiles = new HashMap<String, File>(10);

		FileUtilities.ensureFolderExists(paramsFolder);
	}

	/**
	 * Saves a parameter file.
	 *
	 * @param engine      Engine for which to save the parameters.
	 * @param paramString Saved parameters
	 * @return Where were the parameters saved to
	 */
	public File saveParamFile(final SearchEngine engine, final String paramString) {
		final String suffix = getParameterSuffix(paramString);
		final String hash = getParamFileHash(engine, suffix);
		File result = parameterFiles.get(hash);
		if (result == null) {
			result = engine.getParameterFile(paramsFolder, suffix);
			FileUtilities.writeStringToFile(result, paramString, true);
			parameterFiles.put(hash, result);
		}
		return result;
	}

	/**
	 * Just get the parameter string.
	 *
	 * @param engine     Engine to get the string for
	 * @param parameters Parameters to turn to string
	 * @return Parameters serialized into a single string
	 */
	public String getParamString(final SearchEngine engine, final SearchEngineParameters parameters) {
		return engine.parametersToString(parameters, null, paramsInfo);
	}

	private String getParameterSuffix(final String paramString) {
		String suffix = parameterSuffix.get(paramString);
		if (suffix == null) {
			suffix = parameterSuffix.isEmpty() ? "" : String.valueOf(parameterSuffix.size() + 1);
			parameterSuffix.put(paramString, suffix);
		}
		return suffix;
	}

	private static String getParamFileHash(final SearchEngine searchEngine, final String parametersName) {
		return searchEngine.getCode() + ":" + searchEngine.getVersion() + ":" + parametersName;
	}
}
