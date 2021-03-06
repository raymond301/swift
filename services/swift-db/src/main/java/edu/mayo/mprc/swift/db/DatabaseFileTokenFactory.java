package edu.mayo.mprc.swift.db;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.files.FileToken;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.database.FileTokenToDatabaseTranslator;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * Extension of {@link FileTokenFactory} which is aware of the location of
 * the database daemon.
 * <p/>
 * Using this class, links to files can be stored in the database and retrieved as strings.
 */
@Component("fileTokenFactory")
public final class DatabaseFileTokenFactory extends FileTokenFactory implements Lifecycle, FileTokenToDatabaseTranslator {
	public static final String FILE_TAG = "file";

	private DaemonConfigInfo databaseDaemonConfigInfo;
	private RunningApplicationContext applicationContext;

	public DatabaseFileTokenFactory() {
	}

	public DatabaseFileTokenFactory(final DaemonConfigInfo daemonConfigInfo) {
		super(daemonConfigInfo);
	}


	public String fileToDatabaseToken(final File file) {
		if (file == null) {
			return null;
		}
		// Initialize the configs before we run getFileToken
		final DaemonConfigInfo info = getDatabaseDaemonConfigInfo();

		final FileToken fileToken = getFileToken(file);

		return translateFileToken(fileToken, info).getTokenPath();
	}

	public File databaseTokenToFile(final String tokenPath) {
		if (tokenPath == null) {
			return null;
		}
		final FileToken fileToken = new SharedToken(getDatabaseDaemonConfigInfo(), tokenPath, false);
		return getFile(fileToken);
	}

	public String fileToTaggedDatabaseToken(final File file) {
		return "<" + FILE_TAG + ">" + fileToDatabaseToken(file) + "</" + FILE_TAG + ">";
	}

	public static String tagDatabaseToken(final String databaseToken) {
		return "<" + FILE_TAG + ">" + databaseToken + "</" + FILE_TAG + ">";
	}

	public void setDatabaseDaemonConfigInfo(DaemonConfigInfo databaseDaemonConfigInfo) {
		this.databaseDaemonConfigInfo = databaseDaemonConfigInfo;
	}

	public DaemonConfigInfo getDatabaseDaemonConfigInfo() {
		if (!isRunning()) {
			start();
		}
		return databaseDaemonConfigInfo;
	}

	public RunningApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Resource(name = "swiftEnvironment")
	public void setApplicationContext(final RunningApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Returns a config for a daemon that contains the database. There must be exactly one such daemon.
	 *
	 * @param swiftConfig Swift configuration.
	 * @return Daemon that contains the database module.
	 */
	static DaemonConfig getDatabaseDaemonConfig(final ApplicationConfig swiftConfig) {
		final Database.Config databaseResource = getDatabaseResource(swiftConfig);
		return swiftConfig.getDaemonForResource(databaseResource);
	}

	static Database.Config getDatabaseResource(final ApplicationConfig swiftConfig) {
		final List<Database.Config> configs = swiftConfig.getModulesOfConfigType(Database.Config.class);
		if (configs.size() > 1) {
			throw new MprcException("Swift has more than one database defined.");
		}
		if (configs.isEmpty()) {
			throw new MprcException("Swift does not define a database.");
		}
		return configs.get(0);
	}

	@Override
	public boolean isRunning() {
		return databaseDaemonConfigInfo != null;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			// Setup the actual daemon
			final DaemonConfig daemonConfig = applicationContext.getDaemonConfig();
			if (daemonConfig == null) {
				// We are most likely running without a config file.
				// Nothing can possibly work, but we must not crash.
				databaseDaemonConfigInfo = null;
			} else {
				setDaemonConfigInfo(daemonConfig.createDaemonConfigInfo());
				if (daemonConfig.getTempFolderPath() == null) {
					throw new MprcException("The temporary folder is not configured for this daemon. Swift cannot run.");
				}
				final DaemonConfig databaseDaemonConfig = getDatabaseDaemonConfig(applicationContext.getApplicationConfig());
				databaseDaemonConfigInfo = databaseDaemonConfig.createDaemonConfigInfo();
			}
		}
	}

	@Override
	public void stop() {
	}
}
