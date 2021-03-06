package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract mass spec data from a map of {@link RawFileMetaData} objects embedded in the work packet.
 *
 * @author Roman Zenka
 */
public class MapMassSpecDataExtractor implements MassSpecDataExtractor {

	public static final String MUDPIT_PREFIX = "Mudpit_";
	private static final Pattern MUDPIT_PATTERN = Pattern.compile("Mudpit_ \\((.*)\\)");
	private final Map<String/*msmsSampleName*/, RawFileMetaData> metaDataMap;
	private final Map<String/*msmsSampleName*/, TandemMassSpectrometrySample> sampleMap = new TreeMap<String, TandemMassSpectrometrySample>();

	public MapMassSpecDataExtractor(final Map<String, RawFileMetaData> metaDataMap) {
		this.metaDataMap = metaDataMap;
	}

	@Override
	public TandemMassSpectrometrySample getTandemMassSpectrometrySample(final String biologicalSampleName, final String msmsSampleName) {
		final String cleanedMsmsSampleName = cleanMsmsSampleName(msmsSampleName);

		final RawFileMetaData rawFileMetaData = getMetadata(cleanedMsmsSampleName);
		if (rawFileMetaData == null) {
			return null;
		} else {
			final TandemMassSpectrometrySample result = rawFileMetaData.parse();
			sampleMap.put(cleanedMsmsSampleName, result);
			return result;
		}
	}

	/**
	 * For some strange reason, Scaffold sometimes refers to the sample using parentheses around its name
	 * We need to strip those
	 *
	 * @param msmsSampleName Sample name, possibly with parentheses around
	 * @return Same name without parentheses
	 */
	private String cleanMsmsSampleName(final String msmsSampleName) {
		final String cleanedMsmsSampleName;

		if (msmsSampleName.startsWith("(") && msmsSampleName.endsWith(")")) {
			cleanedMsmsSampleName = msmsSampleName.substring(1, msmsSampleName.length() - 1);
		} else {
			cleanedMsmsSampleName = msmsSampleName;
		}
		return cleanedMsmsSampleName;
	}

	@Override
	public Map<String, TandemMassSpectrometrySample> getMap() {
		return sampleMap;
	}

	/**
	 * Scaffold prefixes the name of the msms sample with "Mudpit_" in case multiple samples are pooled together.
	 *
	 * This format changed over time from Mudpit_name to Mudpit_ (name). We support both.
	 *
	 * @param msmsSampleName Name of the ms/ms sample.
	 * @return Metadata about the matching .RAW file.
	 */
	private RawFileMetaData getMetadata(final String msmsSampleName) {
		final RawFileMetaData metaData = metaDataMap.get(msmsSampleName);
		if (metaData != null) {
			return metaData;
		}
		if (msmsSampleName.startsWith(MUDPIT_PREFIX)) {
			final RawFileMetaData mudpitMetaData = metaDataMap.get(msmsSampleName.substring(MUDPIT_PREFIX.length()));
			if (mudpitMetaData != null) {
				return mudpitMetaData;
			}
		}
		final Matcher matcher = MUDPIT_PATTERN.matcher(msmsSampleName);
		if (matcher.matches()) {
			final RawFileMetaData mudpitMetaData = metaDataMap.get(matcher.group(1));
			if (mudpitMetaData != null) {
				return mudpitMetaData;
			}
		}
		return null;
	}
}
