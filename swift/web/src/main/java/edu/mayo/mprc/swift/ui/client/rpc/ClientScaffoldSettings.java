package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.swift.params2.ScaffoldSettings;

/**
 * Client version of {@link ScaffoldSettings}.
 */
public final class ClientScaffoldSettings implements ClientValue {
	private static final long serialVersionUID = 20101221L;
	private double proteinProbability;
	private double peptideProbability;
	private int minimumPeptideCount;
	private int minimumNonTrypticTerminii;

	private ClientStarredProteins starredProteins;

	private boolean saveOnlyIdentifiedSpectra;
	private boolean saveNoSpectra;
	private boolean connectToNCBI;
	private boolean annotateWithGOA;
	private boolean useIndependentSampleGrouping;
	private boolean useFamilyProteinGrouping;
	private boolean mzIdentMlReport;
	private boolean use3xScoring;
	private boolean highMassAccuracyScoring;

	public ClientScaffoldSettings() {
	}

	public ClientScaffoldSettings(final double proteinProbability, final double peptideProbability, final int minimumPeptideCount, final int minimumNonTrypticTerminii, final ClientStarredProteins starredProteins, final boolean saveOnlyIdentifiedSpectra, final boolean saveNoSpectra, final boolean connectToNCBI, final boolean annotateWithGOA,
	                              final boolean useIndependentSampleGrouping, final boolean useFamilyProteinGrouping,
	                              final boolean mzIdentMlReport, final boolean highMassAccuracyScoring,
	                              final boolean use3xScoring) {
		this.proteinProbability = proteinProbability;
		this.peptideProbability = peptideProbability;
		this.minimumPeptideCount = minimumPeptideCount;
		this.minimumNonTrypticTerminii = minimumNonTrypticTerminii;
		this.starredProteins = starredProteins;
		this.saveOnlyIdentifiedSpectra = saveOnlyIdentifiedSpectra;
		this.saveNoSpectra = saveNoSpectra;
		this.connectToNCBI = connectToNCBI;
		this.annotateWithGOA = annotateWithGOA;
		this.useIndependentSampleGrouping = useIndependentSampleGrouping;
		this.useFamilyProteinGrouping = useFamilyProteinGrouping;
		this.mzIdentMlReport = mzIdentMlReport;
		this.use3xScoring = use3xScoring;
		this.highMassAccuracyScoring = highMassAccuracyScoring;
	}

	public double getProteinProbability() {
		return proteinProbability;
	}

	public void setProteinProbability(final double proteinProbability) {
		this.proteinProbability = proteinProbability;
	}

	public double getPeptideProbability() {
		return peptideProbability;
	}

	public void setPeptideProbability(final double peptideProbability) {
		this.peptideProbability = peptideProbability;
	}

	public int getMinimumPeptideCount() {
		return minimumPeptideCount;
	}

	public void setMinimumPeptideCount(final int minimumPeptideCount) {
		this.minimumPeptideCount = minimumPeptideCount;
	}

	public int getMinimumNonTrypticTerminii() {
		return minimumNonTrypticTerminii;
	}

	public void setMinimumNonTrypticTerminii(final int minimumNonTrypticTerminii) {
		this.minimumNonTrypticTerminii = minimumNonTrypticTerminii;
	}

	public ClientStarredProteins getStarredProteins() {
		return starredProteins;
	}

	public void setStarredProteins(final ClientStarredProteins starredProteins) {
		this.starredProteins = starredProteins;
	}

	public boolean isSaveOnlyIdentifiedSpectra() {
		return saveOnlyIdentifiedSpectra;
	}

	public void setSaveOnlyIdentifiedSpectra(final boolean saveOnlyIdentifiedSpectra) {
		this.saveOnlyIdentifiedSpectra = saveOnlyIdentifiedSpectra;
	}

	public boolean isSaveNoSpectra() {
		return saveNoSpectra;
	}

	public void setSaveNoSpectra(final boolean saveNoSpectra) {
		this.saveNoSpectra = saveNoSpectra;
	}

	public boolean isConnectToNCBI() {
		return connectToNCBI;
	}

	public void setConnectToNCBI(final boolean connectToNCBI) {
		this.connectToNCBI = connectToNCBI;
	}

	public boolean isAnnotateWithGOA() {
		return annotateWithGOA;
	}

	public void setAnnotateWithGOA(final boolean annotateWithGOA) {
		this.annotateWithGOA = annotateWithGOA;
	}

	public boolean isUseIndependentSampleGrouping() {
		return useIndependentSampleGrouping;
	}

	public void setUseIndependentSampleGrouping(final boolean useIndependentSampleGrouping) {
		this.useIndependentSampleGrouping = useIndependentSampleGrouping;
	}

	public boolean isUseFamilyProteinGrouping() {
		return useFamilyProteinGrouping;
	}

	public void setUseFamilyProteinGrouping(final boolean useFamilyProteinGrouping) {
		this.useFamilyProteinGrouping = useFamilyProteinGrouping;
	}

	public boolean isMzIdentMlReport() {
		return mzIdentMlReport;
	}

	public void setMzIdentMlReport(final boolean mzIdentMlReport) {
		this.mzIdentMlReport = mzIdentMlReport;
	}

	public boolean isUse3xScoring() {
		return use3xScoring;
	}

	public void setUse3xScoring(final boolean use3xScoring) {
		this.use3xScoring = use3xScoring;
	}

	public boolean isHighMassAccuracyScoring() {
		return highMassAccuracyScoring;
	}

	public void setHighMassAccuracyScoring(final boolean highMassAccuracyScoring) {
		this.highMassAccuracyScoring = highMassAccuracyScoring;
	}
}
