package edu.mayo.mprc.swift.ui.client;

import edu.mayo.mprc.swift.ui.client.rpc.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class InitialPageData implements Serializable {
	private static final long serialVersionUID = 6769766487310788649L;

	private ClientUser[] users;
	private ClientLoadedSearch loadedSearch;
	private String userMessage;
	private ClientParamSetList paramSetList;
	private HashMap<String, List<ClientValue>> allowedValues;
	private boolean databaseUndeployerEnabled;
	private List<ClientSearchEngine> searchEngines;
	private List<SpectrumQaParamFileInfo> spectrumQaParamFileInfo;
	private boolean scaffoldReportEnabled;

	public InitialPageData() {
	}

	public InitialPageData(ClientUser[] users,
	                       ClientLoadedSearch loadedSearch,
	                       String userMessage,
	                       ClientParamSetList paramSetList,
	                       HashMap<String, List<ClientValue>> allowedValues,
	                       boolean databaseUndeployerEnabled,
	                       List<ClientSearchEngine> searchEngines,
	                       List<SpectrumQaParamFileInfo> spectrumQaParamFileInfo,
	                       boolean scaffoldReportEnabled) {
		this.users = users;
		this.loadedSearch = loadedSearch;
		this.userMessage = userMessage;
		this.paramSetList = paramSetList;
		this.allowedValues = allowedValues;
		this.databaseUndeployerEnabled = databaseUndeployerEnabled;
		this.searchEngines = searchEngines;
		this.spectrumQaParamFileInfo = spectrumQaParamFileInfo;
		this.scaffoldReportEnabled = scaffoldReportEnabled;
	}

	List<ClientSearchEngine> getSearchEngines() {
		return searchEngines;
	}

	ClientUser[] listUsers() {
		return users;
	}

	ClientLoadedSearch loadedSearch() {
		return loadedSearch;
	}

	String getUserMessage() {
		return userMessage;
	}

	ClientParamSetList getParamSetList() {
		return paramSetList;
	}

	public HashMap<String, List<ClientValue>> getAllowedValues() {
		return allowedValues;
	}

	boolean isDatabaseUndeployerEnabled() {
		return databaseUndeployerEnabled;
	}

	public List<SpectrumQaParamFileInfo> getSpectrumQaParamFileInfo() {
		return spectrumQaParamFileInfo;
	}

	public boolean isScaffoldReportEnabled() {
		return scaffoldReportEnabled;
	}
}