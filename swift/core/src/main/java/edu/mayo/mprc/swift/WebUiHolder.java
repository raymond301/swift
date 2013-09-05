package edu.mayo.mprc.swift;

import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

/**
 * @author Roman Zenka
 */
public final class WebUiHolder {
	private RunningApplicationContext context;

	public WebUi getWebUi() {
		final RunningApplicationContext context = getContext();
		final ResourceConfig webUiConfig = context.getSingletonConfig(WebUi.Config.class);
		final Object resource = context.createResource(webUiConfig);
		if (!(resource instanceof WebUi)) {
			ExceptionUtilities.throwCastException(resource, WebUi.class);
			return null;
		}
		return (WebUi) resource;

	}

	public void stopSwiftMonitor() {
		if (getWebUi() != null) {
			getWebUi().stopSwiftMonitor();
		}
	}

	public RunningApplicationContext getContext() {
		return context;
	}

	public void setContext(final RunningApplicationContext context) {
		this.context = context;
	}
}
