package edu.mayo.mprc.scaffoldparser;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.AbstractXmlDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Loads and saves Scaffold's .xml format. This format was extensively used by MPRC in the past as a way
 * of getting at Scaffold's data. Since Scaffold 3 version broke the internal .XML format and made it much less useful,
 * we stopped relying on this method - it is provided for backwards reading of large datasets we already own.
 *
 * @author Roman Zenka
 */
public final class ScaffoldParser {
	/**
	 * We need to retain underscores in class names when serializing without escaping them.
	 */
	static final XmlFriendlyReplacer KEEP_UNDERSCORES = new XmlFriendlyReplacer("-_", "_");

	private static final Class[] SCAFFOLD_CLASSES = new Class[]{
			BiologicalSample.class,
			DisplayThresholds.class,
			Experiment.class,
			MascotThresholds.class,
			Modification.class,
			PeptideAnalysisIdentification.class,
			PeptideGroupIdentification.class,
			PreferredProteinAnnotation.class,
			ProteinAnalysisIdentification.class,
			ProteinGroup.class,
			Scaffold.class,
			SequestThresholds.class,
			SpectrumAnalysisIdentification.class,
			TandemMassSpectrometrySample.class,
			TandemThresholds.class,
	};

	private ScaffoldParser() {
	}

	/**
	 * Load Scaffold .xml export from input stream.
	 *
	 * @param stream Stream to load the .xml from.
	 * @return The root object {@link Scaffold} representing the entire Scaffold .xml file.
	 */
	public static Scaffold loadScaffoldXml(final InputStream stream) {
		// Change the replacer to keep underscores.
		// We marshall the classes manually anyway, so this is not a big deal
		return loadScaffoldXml(stream, new XppDriver(KEEP_UNDERSCORES));
	}

	private static Scaffold loadScaffoldXml(final InputStream stream, final AbstractXmlDriver driver) {
		final XStream xs = new XStream(driver) {
			@Override
			protected MapperWrapper wrapMapper(final MapperWrapper next) {
				return new MapperWrapper(next) {
					public boolean shouldSerializeMember(final Class definedIn, final String fieldName) {
						try {
							return (!Object.class.equals(definedIn) || realClass(fieldName) != null) && super.shouldSerializeMember(definedIn, fieldName);
						} catch (Exception ignore) {
							return false;
						}
					}
				};
			}
		};

		xs.setMode(XStream.ID_REFERENCES);

		xs.processAnnotations(SCAFFOLD_CLASSES);

		return (Scaffold) xs.fromXML(stream);
	}

	/**
	 * Save given Scaffold .xml file representation to a give stream.
	 *
	 * @param scaffold Scaffold object to save
	 * @param stream   Stream to save into as .xml
	 */
	public static void saveScaffoldXml(final Scaffold scaffold, final OutputStream stream) {
		saveScaffoldXml(scaffold, stream, new XppDriver(KEEP_UNDERSCORES));
	}

	private static void saveScaffoldXml(final Scaffold data, final OutputStream stream, final AbstractXmlDriver driver) {
		final XStream xs = new XStream(driver);
		xs.setMode(XStream.ID_REFERENCES);
		xs.processAnnotations(SCAFFOLD_CLASSES);

		xs.toXML(data, stream);
	}
}
