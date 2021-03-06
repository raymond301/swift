package edu.mayo.mprc.mascot;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

public final class MascotResultUrl implements ProgressInfo {
	private static final long serialVersionUID = 20101101;

	private String mascotUrl;

	public MascotResultUrl() {
	}

	public MascotResultUrl(final String mascotUrl) {
		this.mascotUrl = mascotUrl;
	}

	public String getMascotUrl() {
		return mascotUrl;
	}

	public void setMascotUrl(final String mascotUrl) {
		this.mascotUrl = mascotUrl;
	}
}
