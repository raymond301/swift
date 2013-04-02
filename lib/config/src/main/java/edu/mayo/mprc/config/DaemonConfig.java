package edu.mayo.mprc.config;

import com.google.common.base.Splitter;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * A daemon is simply a running java VM which exposes one or more services.
 */
@XStreamAlias("daemon")
public final class DaemonConfig implements ResourceConfig, NamedResource {
	public static final String WINE_CMD = "wine";
	public static final String WINECONSOLE_CMD = "wineconsole";
	public static final String XVFB_CMD = "bin/util/unixXvfbWrapper.sh";

	public static final String NAME = "name";
	public static final String HOST_NAME = "hostName";
	public static final String OS_NAME = "osName";
	public static final String OS_ARCH = "osArch";
	public static final String SHARED_FILE_SPACE_PATH = "sharedFileSpacePath";
	public static final String TEMP_FOLDER_PATH = "tempFolderPath";
	public static final String DUMP_ERRORS = "dumpErrors";
	public static final String DUMP_FOLDER_PATH = "dumpFolderPath";
	public static final String SERVICES = "services";
	public static final String RESOURCES = "resources";

	@XStreamAlias(NAME)
	@XStreamAsAttribute
	private String name;

	@XStreamAlias(HOST_NAME)
	private String hostName;

	@XStreamAlias(OS_NAME)
	private String osName;

	@XStreamAlias(OS_ARCH)
	private String osArch;

	@XStreamAlias(SHARED_FILE_SPACE_PATH)
	private String sharedFileSpacePath;

	@XStreamAlias(TEMP_FOLDER_PATH)
	private String tempFolderPath;

	/**
	 * When enabled, the daemon would dump a file on every error. This dump contains
	 * the work packet + information about the environment and machine where the error occurred +
	 */
	@XStreamAlias(DUMP_ERRORS)
	private boolean dumpErrors;

	/**
	 * Where should the daemon dump files when an error occurs. If not set, the tempFolderPath is used.
	 */
	@XStreamAlias(DUMP_FOLDER_PATH)
	private String dumpFolderPath;

	// Services this daemon provides
	@XStreamAlias("services")
	private List<ServiceConfig> services = new ArrayList<ServiceConfig>();

	// Resources this daemon defines locally
	@XStreamAlias("resources")
	private List<ResourceConfig> resources = new ArrayList<ResourceConfig>();

	/**
	 * This is not being serialized - recreated on the fly when {@link ApplicationConfig} is loaded.
	 */
	@XStreamOmitField()
	private ApplicationConfig applicationConfig;

	public DaemonConfig() {
	}

	/**
	 * Create daemon config with given index.
	 *
	 * @param name  Name of the daemon.
	 * @param local If true, the daemon is expected to run on the local computer.
	 * @return Default daemon setup.
	 */
	public static DaemonConfig getDefaultDaemonConfig(final String name, final boolean local) {
		final DaemonConfig daemon = new DaemonConfig();
		daemon.setName(name);
		daemon.setOsName(System.getProperty("os.name"));
		daemon.setOsArch(System.getProperty("os.arch"));
		daemon.setTempFolderPath("var/tmp");
		daemon.setDumpErrors(false);
		daemon.setDumpFolderPath("var/tmp/dump");
		daemon.setSharedFileSpacePath("/");

		if (local) {
			// Host name set by default to this computer
			final InetAddress localHost;
			try {
				localHost = InetAddress.getLocalHost();
				final String hostName = localHost.getHostName();
				daemon.setHostName(hostName);
			} catch (UnknownHostException ignore) {
				daemon.setHostName("localhost");
			}
		}

		return daemon;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(final String hostName) {
		this.hostName = hostName;
	}

	public String getOsName() {
		return osName;
	}

	public void setOsName(final String osName) {
		this.osName = osName;
	}

	public String getOsArch() {
		return osArch;
	}

	public void setOsArch(final String osArch) {
		this.osArch = osArch;
	}

	public String getSharedFileSpacePath() {
		return sharedFileSpacePath;
	}

	public void setSharedFileSpacePath(final String sharedFileSpacePath) {
		this.sharedFileSpacePath = sharedFileSpacePath;
	}

	public String getTempFolderPath() {
		return tempFolderPath;
	}

	public void setTempFolderPath(final String tempFolderPath) {
		this.tempFolderPath = tempFolderPath;
	}

	public boolean isDumpErrors() {
		return dumpErrors;
	}

	public void setDumpErrors(final boolean dumpErrors) {
		this.dumpErrors = dumpErrors;
	}

	public String getDumpFolderPath() {
		return dumpFolderPath;
	}

	public void setDumpFolderPath(final String dumpFolderPath) {
		this.dumpFolderPath = dumpFolderPath;
	}

	public List<ServiceConfig> getServices() {
		return services;
	}

	public DaemonConfig addService(final ServiceConfig service) {
		services.add(service);
		return this;
	}

	public boolean removeService(final ServiceConfig service) {
		return services.remove(service);
	}

	public List<ResourceConfig> getResources() {
		return resources;
	}

	public DaemonConfig addResource(final ResourceConfig resource) {
		resources.add(resource);
		return this;
	}

	public boolean removeResource(final ResourceConfig resource) {
		return resources.remove(resource);
	}

	public ApplicationConfig getApplicationConfig() {
		return applicationConfig;
	}

	public void setApplicationConfig(final ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	public boolean isWindows() {
		return isOs("windows");
	}

	public boolean isLinux() {
		return isOs("linux");
	}

	public boolean isMac() {
		return isOs("mac");
	}

	/**
	 * @return A wrapper script that executes a windows command on linux. On windows it is not necessary - return empty string.
	 */
	public String getWrapperScript() {
		if (isWindows()) {
			return "";
		} else {
			return WINECONSOLE_CMD;
		}
	}

	/**
	 * @return A wrapper script that executes a windows command that needs a graphical console on linux (using Xvfb) -
	 *         virtual frame buffer. On windows not necessary - return empty string.
	 */
	public String getXvfbWrapperScript() {
		if (isWindows()) {
			return "";
		} else {
			return XVFB_CMD;
		}
	}

	private boolean isOs(final String osString) {
		final String osName = getOsName() == null ? "" : getOsName().toLowerCase(Locale.ENGLISH);
		return osName.contains(osString);
	}

	public void save(final ConfigWriter writer) {
		writer.put(NAME, getName(), "User-friendly name of this daemon");
		writer.put(HOST_NAME, getHostName(), "Host the daemon runs on");
		writer.put(OS_NAME, getOsName(), "Host system operating system name: e.g. Windows or Linux.");
		writer.put(OS_ARCH, getOsArch(), "Host system architecture: x86, x86_64");
		writer.put(SHARED_FILE_SPACE_PATH, getSharedFileSpacePath(), "Directory on a shared file system can be accessed from all the daemons");
		writer.put(TEMP_FOLDER_PATH, getSharedFileSpacePath(), "Temporary folder that can be used for caching. Transferred files from other daemons with no shared file system with this daemon are cached to this folder.");
		writer.put(DUMP_ERRORS, getSharedFileSpacePath(), "Not implemented yet");
		writer.put(DUMP_FOLDER_PATH, getSharedFileSpacePath(), "Not implemented yet");

		writer.put(SERVICES, getResourceList(writer, getServices()), "Comma separated list of provided services");
		writer.put(RESOURCES, getResourceList(writer, getResources()), "Comma separated list of provided resources");
	}

	private static String getResourceList(final ConfigWriter writer, final Collection<? extends ResourceConfig> resources) {
		final StringBuilder result = new StringBuilder(resources.size() * 10);
		for (final ResourceConfig config : resources) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append(writer.save(config));
		}
		return result.toString();
	}

	public void load(final ConfigReader reader) {
		name = reader.get(NAME);
		hostName = reader.get(HOST_NAME);
		osName = reader.get(OS_NAME);
		osArch = reader.get(OS_ARCH);
		sharedFileSpacePath = reader.get(SHARED_FILE_SPACE_PATH);
		tempFolderPath = reader.get(TEMP_FOLDER_PATH);
		dumpErrors = reader.getBoolean(DUMP_ERRORS);
		dumpFolderPath = reader.get(DUMP_FOLDER_PATH);

		{
			final String servicesString = reader.get(SERVICES);
			final Iterable<String> servicesList = Splitter.on(",").trimResults().omitEmptyStrings().split(servicesString);
			services.clear();
			for (final String service : servicesList) {
				services.add((ServiceConfig) reader.getObject(service));
			}
		}

		{
			final String resourcesString = reader.get(RESOURCES);
			final Iterable<String> resourcesList = Splitter.on(",").trimResults().omitEmptyStrings().split(resourcesString);
			resources.clear();
			for (final String resource : resourcesList) {
				resources.add(reader.getObject(resource));
			}
		}
	}

	public DaemonConfigInfo createDaemonConfigInfo() {
		return new DaemonConfigInfo(name, sharedFileSpacePath);
	}

	@Override
	public int getPriority() {
		return 0;
	}

	public ResourceConfig firstResourceOfType(final Class<?> clazz) {
		for (final ResourceConfig resourceConfig : resources) {
			if (clazz.isAssignableFrom(resourceConfig.getClass())) {
				return resourceConfig;
			}
		}
		return null;
	}

	public ResourceConfig firstServiceOfType(final Class<?> clazz) {
		for (final ServiceConfig serviceConfig : services) {
			if (clazz.isAssignableFrom(serviceConfig.getRunner().getWorkerConfiguration().getClass())) {
				return serviceConfig;
			}
		}
		return null;
	}
}
