package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Roman Zenka
 */
@Component("check-config-command")
public final class CheckConfigCommand implements SwiftCommand {
	@Override
	public String getDescription() {
		return "Make sure the Swift configuration for the current daemon is correct";
	}

	@Override
	public ExitCode run(SwiftEnvironment environment) {
		try {
			final File configFile = environment.getConfigFile();
			if (configFile == null) {
				// Config is done standalone
				FileUtilities.err("Please create and provide the configuration file first");
				return ExitCode.Error;
			}

			final DaemonConfig config = environment.getDaemonConfig();
			FileUtilities.out("Checking configuration file: " + environment.getConfigFile().getAbsolutePath() + " for daemon " + environment.getDaemonConfig().getName());

			final Daemon daemon = environment.createDaemon(config);
			final String check = daemon.check();
			if (check != null) {
				FileUtilities.err(check);
				return ExitCode.Error;
			}
		} catch (MprcException e) {
			FileUtilities.err(e.getMessage());
			return ExitCode.Error;
		}
		FileUtilities.out("Check passed");
		return ExitCode.Ok;
	}
}
