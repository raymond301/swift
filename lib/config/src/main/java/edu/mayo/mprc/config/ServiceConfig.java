package edu.mayo.mprc.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * The service manager allows the application to send a service a work packet. The service does the work
 * and responds.
 * <p/>
 * A service configuration on its own does not define how is the service provided. It is just an identified endpoint.
 * How the service is run is defined by a runner, which contains a link to the worker itself.
 */
public final class ServiceConfig implements ResourceConfig, NamedResource {
	public static final String RUNNER = "runner";
	@XStreamAlias("name")
	@XStreamAsAttribute
	private String name;

	@XStreamAlias(RUNNER)
	private RunnerConfig runner;

	public ServiceConfig() {
	}

	public ServiceConfig(final String name, final RunnerConfig runner) {
		this.name = name;
		this.runner = runner;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public RunnerConfig getRunner() {
		return runner;
	}

	public void setRunner(final RunnerConfig runner) {
		this.runner = runner;
	}

	@Override
	public void save(final ConfigWriter writer) {
		writer.put("name", getName());
		writer.put(RUNNER, writer.save(runner));
	}

	@Override
	public void load(final ConfigReader reader) {
		setName(reader.get("name"));
		setRunner((RunnerConfig) reader.getObject(RUNNER));
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public String toString() {
		return "ServiceConfig{" +
				"name='" + name + '\'' +
				", runner=" + runner +
				'}';
	}
}
