package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.swift.params2.mapping.Validation;
import edu.mayo.mprc.swift.params2.mapping.ValidationSeverity;

/**
 * Client side proxy for {@link Validation}.
 */
public final class ClientValidation implements ClientValue {
	private static final long serialVersionUID = 20101221L;
	private String message;
	private int severity;
	private String paramId;
	private ClientValue value;
	private String throwableMessage;
	private String throwableStackTrace;

	/**
	 * Note that these values must remain synced with {@link ValidationSeverity}.
	 */
	public static final int SEVERITY_NONE = 0;
	public static final int SEVERITY_INFO = 1;
	public static final int SEVERITY_WARNING = 2;
	public static final int SEVERITY_ERROR = 3;

	public ClientValidation() {
		this("No message");
	}

	public ClientValidation(final String message) {
		this.message = message;
	}

	public ClientValidation(final String message, final String paramId, final int severity) {
		this.message = message;
		this.paramId = paramId;
		this.severity = severity;
	}

	public ClientValidation shallowCopy() {
		final ClientValidation cv = new ClientValidation();
		cv.setMessage(getMessage());
		cv.setSeverity(getSeverity());
		cv.setParamId(getParamId());
		cv.setValue(getValue());
		cv.setThrowableMessage(getThrowableMessage());
		cv.setThrowableStackTrace(getThrowableStackTrace());
		// don't set next
		return cv;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public int getSeverity() {
		return severity;
	}

	public void setSeverity(final int severity) {
		this.severity = severity;
	}

	public String getParamId() {
		return paramId;
	}

	public void setParamId(final String paramId) {
		this.paramId = paramId;
	}

	public ClientValue getValue() {
		return value;
	}

	public void setValue(final ClientValue value) {
		this.value = value;
	}

	public String getThrowableMessage() {
		return throwableMessage;
	}

	public void setThrowableMessage(final String throwableMessage) {
		this.throwableMessage = throwableMessage;
	}

	public String getThrowableStackTrace() {
		return throwableStackTrace;
	}

	public void setThrowableStackTrace(final String throwableStackTrace) {
		this.throwableStackTrace = throwableStackTrace;
	}
}
