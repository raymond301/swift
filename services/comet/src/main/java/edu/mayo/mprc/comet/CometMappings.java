package edu.mayo.mprc.comet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Terminus;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;
import edu.mayo.mprc.utilities.StringUtilities;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CometMappings implements Mappings {
	private static final Pattern WHITESPACE = Pattern.compile("^\\s*$");
	private static final Pattern COMMENT = Pattern.compile("^\\s*(#.*)$");
	private static final Pattern EQUALS = Pattern.compile("^.*\\=.*$");
	private static final Pattern PARSE_LINE = Pattern.compile("^\\s*([^\\s=]+)\\s*=\\s*([^#]*)(\\s*#.*)?$");
	private static final Pattern ENZYME_SECTION = Pattern.compile("^\\s*\\[COMET_ENZYME_INFO\\].*$");

	// Number, Name, direction, before, NOT after
	// 1. Trypsin 1 KR P
	private static final Pattern ENZYME_ROW = Pattern.compile("^\\s*(\\d+)\\.\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+).*$");

	private static final String PEP_TOL_UNIT = "peptide_mass_units";
	private static final String PEP_TOL_VALUE = "peptide_mass_tolerance";
	private static final String MISSED_CLEAVAGES = "allowed_missed_cleavage";

	private static final String FRAGMENT_BIN_TOL = "fragment_bin_tol";
	private static final String FRAGMENT_BIN_OFFSET = "fragment_bin_offset";

	private static final String DATABASE = "database_name";

	private static final String NUM_ENZYME_TERMINI = "num_enzyme_termini";

	private static final Pattern FIXED = Pattern.compile("^add_([A-Z]|Nterm|Cterm)_(.*)");
	private static final String[] FIXED_MODS = new String[]{
			"add_Cterm_peptide",
			"add_Cterm_protein",
			"add_Nterm_peptide",
			"add_Nterm_protein",
			"add_G_glycine",
			"add_A_alanine",
			"add_S_serine",
			"add_P_proline",
			"add_V_valine",
			"add_T_threonine",
			"add_C_cysteine",
			"add_L_leucine",
			"add_I_isoleucine",
			"add_N_asparagine",
			"add_D_aspartic_acid",
			"add_O_ornithine",
			"add_Q_glutamine",
			"add_K_lysine",
			"add_E_glutamic_acid",
			"add_M_methionine",
			"add_H_histidine",
			"add_F_phenylalanine",
			"add_R_arginine",
			"add_Y_tyrosine",
			"add_W_tryptophan",
			"add_B_user_amino_acid",
			"add_J_user_amino_acid",
			"add_U_user_amino_acid",
			"add_X_user_amino_acid",
			"add_Z_user_amino_acid",
	};

	/**
	 * Native params are stored in order in which they were defined.
	 */
	private LinkedHashMap<String, String> nativeParams = new LinkedHashMap<String, String>();

	/**
	 * Holds all the default enzymes Comets defines by default.
	 */
	private LinkedHashMap<String, Protease> defaultEnzymes = new LinkedHashMap<String, Protease>(11);

	/**
	 * The extra custom enzyme if the default set was not enough or null otherwise.
	 */
	private Protease customEnzyme = null;

	/**
	 * Number to use for the custom enzyme.
	 */
	private int customEnzymeNumber = -1;

	@Override
	public Reader baseSettings() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/comet/base.comet.params", getClass());
	}

	@Override
	public void read(final Reader isr) {
		final BufferedReader reader = new BufferedReader(isr);
		boolean enzymeSection = false;
		try {
			while (true) {
				final String it = reader.readLine();
				if (it == null) {
					break;
				}

				if (WHITESPACE.matcher(it).matches() || COMMENT.matcher(it).matches()) {
					// Comment, ignore
					continue;
				}

				if (enzymeSection) {
					Matcher row = ENZYME_ROW.matcher(it);
					if (row.matches()) {
						// Number, Name, direction, before, after
						final String number = row.group(1);
						final String name = row.group(2);
						final String direction = row.group(3); // 1 - before==towards N, 0 - before==towards C
						final String before = row.group(4); // What amino acids allowed BEFORE the cleavage point
						final String notAfter = row.group(5); // What amino acids NOT allowed AFTER the cleavage point

						Protease protease;
						if ("1".equals(direction)) {
							// before == N end
							protease = new Protease(name, before, "!" + notAfter);
						} else {
							protease = new Protease(name, "!" + notAfter, before);
						}
						defaultEnzymes.put(number, protease);
						int num = Integer.valueOf(number);
						if (this.customEnzymeNumber < num + 1) {
							this.customEnzymeNumber = num + 1;
						}
					}
					continue;
				}

				if (ENZYME_SECTION.matcher(it).matches()) {
					// We are entering the enzyme definition section
					enzymeSection = true;
					continue;
				}

				if (EQUALS.matcher(it).matches()) {
					// basically, we want to match: a keyword followed by equals followed by an optional value
					// followed by optional whitespace and comment.
					final Matcher matcher = PARSE_LINE.matcher(it);
					if (!matcher.matches()) {
						throw new MprcException("Can't understand '" + it + "'");
					}
					final String id = matcher.group(1);
					String value = matcher.group(2);
					if (value == null) {
						value = "";
					} else {
						value = value.trim();
					}

					// We store absolutely all parameters, because makedb depends on it
					nativeParams.put(id, value);
				} else {
					throw new MprcException("Can't understand '" + it + "'");
				}
			}
		} catch (Exception t) {
			throw new MprcException("Failure reading sequest parameter collection", t);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	@Override
	public void write(final Reader oldParams, final Writer out) {
		BufferedReader reader = null;
		PrintWriter pw = null;
		try {
			reader = new BufferedReader(oldParams);
			pw = new PrintWriter(out);
			while (true) {
				final String it = reader.readLine();
				if (it == null) {
					break;
				}

				if (WHITESPACE.matcher(it).matches() || COMMENT.matcher(it).matches()) {
					pw.println(it);
				} else if (ENZYME_SECTION.matcher(it).matches()) {
					// We are entering the enzyme definition section

					for (Map.Entry<String, Protease> entry : defaultEnzymes.entrySet()) {
						final Protease enzyme = entry.getValue();
						final String direction;
						final String prev;
						final String next;
						if (enzyme.getRnminus1().startsWith("!")) {
							direction = "1";
							prev = enzyme.getRn();
							next = enzyme.getRnminus1().substring(1);
						} else {
							direction = "0";
							prev = enzyme.getRnminus1();
							if (enzyme.getRn().startsWith("!")) {
								next = enzyme.getRn().substring(1);
							} else {
								next = enzyme.getRn();
							}
						}

						pw.println(spaces(4, entry.getKey() + ".")
								+ spaces(23, enzyme.getName())
								+ spaces(7, direction)
								+ spaces(12, prev)
								+ next);
					}

					// This is the last section. Quit now
					break;
				} else if (EQUALS.matcher(it).matches()) {
					// basically, we want to match: a keyword followed by equals followed by an optional value
					// followed by optional whitespace and comment.
					final Matcher matcher = PARSE_LINE.matcher(it);
					if (!matcher.matches()) {
						throw new MprcException("Can't understand '" + it + "'");
					}
					final String id = matcher.group(1);
					if (nativeParams.keySet().contains(id)) {
						// Replace the value
						if (matcher.group(2) != null) {
							// We have the value defined
							pw.print(it.substring(0, matcher.start(2)));
							pw.print(nativeParams.get(id));
							pw.println(it.substring(matcher.end(2)));
						} else {
							// The value is missing
							pw.print(it.trim());
							pw.println(nativeParams.get(id));
						}
					} else {
						pw.println(it);
					}
				} else {
					pw.println(it);
				}
			}
		} catch (Exception t) {
			throw new MprcException("Failure reading sequest parameter collection", t);
		} finally {
			FileUtilities.closeQuietly(reader);
			FileUtilities.closeQuietly(pw);
		}
	}

	/**
	 * @param columns How many columns we need to take total
	 * @param s       What are we printing
	 * @return s + as many spaces as it takes to get to required columns
	 */
	private String spaces(int columns, String s) {
		int extra = columns - s.length();
		if (extra > 0) {
			return s + StringUtilities.repeat(' ', extra);
		}
		return s;
	}

	/**
	 * @return The complete map of native parameters.
	 */
	public Map<String, String> getNativeParams() {
		return Collections.unmodifiableMap(nativeParams);
	}

	/**
	 * Returns value of a given native parameter.
	 *
	 * @param name Name of the parameter.
	 * @return Value of the native parameter.
	 */
	@Override
	public String getNativeParam(final String name) {
		return nativeParams.get(name).trim();
	}

	/**
	 * Allows the sequest to makedb converter to overwrite the native parameter values directly.
	 *
	 * @param name  Name of the native parameter.
	 * @param value Value of the native parameter.
	 */
	@Override
	public void setNativeParam(final String name, final String value) {
		nativeParams.put(name, value);
	}

	@Override
	public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
		setNativeParam(PEP_TOL_VALUE, String.valueOf(peptideTolerance.getValue()));
		if (MassUnit.Da.equals(peptideTolerance.getUnit())) {
			setNativeParam(PEP_TOL_UNIT, "0");
			// MMU = 1 but we never use MMU
		} else if (MassUnit.Ppm.equals(peptideTolerance.getUnit())) {
			setNativeParam(PEP_TOL_UNIT, "2");
		}
	}

	@Override
	public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
		double value = fragmentTolerance.getValue();
		if (fragmentTolerance.getUnit() == MassUnit.Ppm) {
			value = value / 1000.0;
			context.reportWarning("Comet does not support ppm, using " + value + " Da", ParamName.FragmentTolerance);
		}
		setNativeParam(FRAGMENT_BIN_TOL, String.valueOf(value));
		setNativeParam(FRAGMENT_BIN_OFFSET, value <= 0.8 ? "0.0" : "0.4");
	}

	@Override
	public void setVariableMods(final MappingContext context, final ModSet variableMods) {
		// Sort the mods to get consistent output
		List<ModSpecificity> mods = new ArrayList<ModSpecificity>(variableMods.getModifications());
		Collections.sort(mods);

		int modNumber = 1;
		for (ModSpecificity specificity : mods) {
			if (modNumber > 6) {
				context.reportWarning("Comet supports 6 variable modifications. The list will be truncated", ParamName.VariableMods);
				break;
			}

			if (specificity.isSiteCTerminus()) {
				context.reportWarning("Comet skipped unsupported C-term mod '" + specificity.toString() + "'", ParamName.VariableMods);
				break;
			}

			if (specificity.isSiteNTerminus()) {
				context.reportWarning("Comet skipped unsupported N-term mod '" + specificity.toString() + "'", ParamName.VariableMods);
				break;
			}

			if (!specificity.isSiteSpecificAminoAcid()) {
				context.reportWarning("Comet skipped unlocalized variable mod '" + specificity.toString() + "'", ParamName.VariableMods);
				break;
			}

			if (specificity.isPositionProteinSpecific() || specificity.isPositionNTerminus() || specificity.isPositionCTerminus()) {
				final ModSpecificity copy = new ModSpecificity(specificity.getModification(), specificity.getSite(), Terminus.Anywhere, false, specificity.getHidden(), specificity.getClassification(), specificity.getSpecificityGroup(), specificity.getComments());
				context.reportWarning("Comet replaced '" + specificity.toString() + "' mod with '" + copy.toString() + "'", ParamName.VariableMods);
			}

			String modString = specificity.getModification().getMassMono() + " " + specificity.getSite() + " 0 3";
			setNativeParam("variable_mod" + modNumber, modString);
			modNumber++;
		}

		// Wipe the rest
		while (modNumber <= 6) {
			setNativeParam("variable_mod" + modNumber, "0.0 X 0 3 ");
			modNumber++;
		}
	}

	/**
	 * @param builder Builder to append next string to.
	 * @param text    String to append. Strings are separated by {@code ", "}
	 */
	private void appendCommaSeparated(final StringBuilder builder, final String text) {
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(text);
	}

	@Override
	public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
		// The key is in form [AA|Cterm|Nterm]_[protein|peptide]
		// We sum all the fixed mod contributions
		final Map<String, Double> masses = new HashMap<String, Double>();
		for (final ModSpecificity ms : fixedMods.getModifications()) {
			final String key;
			if (ms.isPositionAnywhere() && ms.isSiteSpecificAminoAcid()) {
				key = String.valueOf(ms.getSite()); // The key is single letter corresponding to the amino acid
			} else if (ms.isPositionCTerminus() && !ms.isSiteSpecificAminoAcid()) {
				key = "Cterm_" + (ms.isProteinOnly() ? "protein" : "peptide");
			} else if (ms.isPositionNTerminus() && !ms.isSiteSpecificAminoAcid()) {
				key = "Nterm_" + (ms.isProteinOnly() ? "protein" : "peptide");
			} else {
				context.reportWarning("Comet does not support fixed mod '" + ms.toString() + "' - skipping", null);
				break;
			}

			final double mass = ms.getModification().getMassMono();
			Double d = 0.0;
			if (masses.containsKey(key)) {
				d = masses.get(key);
			}
			masses.put(key, d + mass);
		}

		for (final String param : FIXED_MODS) {
			final Matcher matcher = FIXED.matcher(param);
			if (matcher.matches()) {
				final String ssite = matcher.group(1);
				final String pos = matcher.group(2);
				final String site;
				if (pos.startsWith("protein") || pos.startsWith("peptide")) {
					site = ssite + "_" + pos;
				} else {
					site = ssite;
				}
				if (masses.containsKey(site)) {
					setNativeParam(matcher.group(), String.valueOf(masses.get(site)));
				} else {
					setNativeParam(matcher.group(), "0.0");
				}
			}
		}
	}

	@Override
	public void setSequenceDatabase(final MappingContext context, final String shortDatabaseName) {
		setNativeParam(DATABASE, "${DB:" + shortDatabaseName + "}");
	}

	@Override
	public void setProtease(final MappingContext context, final Protease protease) {

		final String name = protease.getName().replaceAll("\\s", "_");
		String direction = "1"; // Forward (cut C-terminus from the residue) or reverse (cut N-terminus)
		String rn = protease.getRn();
		String rnminus1 = protease.getRnminus1();

		// how do we recognize sense == 0 (ie inverted match)?
		if ("Non-Specific".equals(protease.getName())) {
			direction = "0";
		} else if (rnminus1.isEmpty()) {
			// if there's no rnminus1, then we assume this is an inverted match
			direction = "0";
			final String swap = rn;
			rn = rnminus1;
			rnminus1 = swap;
		} else if (rn.startsWith("!")) {
			rn = rn.substring(1);
		} else if (!rn.isEmpty()) { // RN.length = 0 is fine, allowed
			// can't deal with this enzyme
			throw new MprcException("Enzyme " + protease.getName() + " cannot be used by Comet");
		}

		// TODO: Set enzymes
	}

	@Override
	public void setMinTerminiCleavages(MappingContext context, Integer minTerminiCleavages) {
		if (minTerminiCleavages == 0) {
			context.reportWarning("Comet does not support non-specific enzymes", ParamName.Enzyme);
		}
		if (minTerminiCleavages > 2) {
			context.reportWarning("Number of tryptic termini must be <= 2", ParamName.Enzyme);
		}
		setNativeParam(NUM_ENZYME_TERMINI, String.valueOf(minTerminiCleavages));
	}

	@Override
	public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
		String value = String.valueOf(missedCleavages);
		if (missedCleavages > 5) {
			value = "5";
			context.reportWarning("Comet does not support > 5 missed cleavages", null);
		}
		setNativeParam(MISSED_CLEAVAGES, value);
	}

	private final Set<String> ALLOWED_SERIES = new ImmutableSet.Builder<String>().add("a", "b", "c", "x", "y", "z").build();

	public void setInstrument(final MappingContext context, final Instrument it) {
		for (String series : ALLOWED_SERIES) {
			setNativeParam(seriesVariableName(series), "0");
		}
		final List<String> droppedSeries = new ArrayList<String>(3);
		for (IonSeries series : it.getSeries()) {
			final String name = series.getName();
			if (ALLOWED_SERIES.contains(name)) {
				setNativeParam(seriesVariableName(name), "1");
			} else {
				droppedSeries.add(series.getName());
			}
		}
		if (droppedSeries.size() > 0) {
			Collections.sort(droppedSeries);
			context.reportWarning("Comet does not support ion series '" + Joiner.on("', '").join(droppedSeries) + "', dropping", ParamName.Instrument);
		}
	}

	private String seriesVariableName(String series) {
		return "use_" + series.toUpperCase(Locale.US) + "_ions";
	}

	@Override
	public void checkValidity(MappingContext context) {
	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		final CometMappings mappings = (CometMappings) super.clone();
		mappings.nativeParams = new LinkedHashMap<String, String>(nativeParams.size());
		mappings.nativeParams.putAll(nativeParams);
		mappings.defaultEnzymes = new LinkedHashMap<String, Protease>(defaultEnzymes.size());
		mappings.defaultEnzymes.putAll(defaultEnzymes);
		return mappings;
	}
}
