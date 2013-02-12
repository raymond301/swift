package edu.mayo.mprc.swift.report;

import com.google.common.base.Charsets;
import edu.mayo.mprc.chem.AminoAcidSet;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * List all amino acids defined in Swift + their masses.
 *
 * @author Roman Zenka
 */
public final class AminoAcidReport implements HttpRequestHandler {
	private AminoAcidSet aminoAcidSet;

	@Override
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(resp.getOutputStream(), Charsets.US_ASCII);
			writer.write("<html><head><title>Amino Acids defined in Swift</title>" +
					"<style>" +
					"table { border-collapse: collapse }" +
					"table td, table th { border: 1px solid black }" +
					"</style>" +
					"</head><body>");
			writer.write("<h1>Amino acids defined in Swift</h1>");
			writer.write(aminoAcidSet.report());
			writer.write("</body></html>");
		} finally {
			FileUtilities.closeQuietly(writer);
		}
	}

	public AminoAcidSet getAminoAcidSet() {
		return aminoAcidSet;
	}

	public void setAminoAcidSet(AminoAcidSet aminoAcidSet) {
		this.aminoAcidSet = aminoAcidSet;
	}
}
