package edu.mayo.mprc.swift.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.monitor.DaemonStatus;
import edu.mayo.mprc.daemon.monitor.PingResponse;
import edu.mayo.mprc.daemon.monitor.PingWorkPacket;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Contains links to all daemon connections defined to be able to monitor everything.
 * Pings them periodically and provides a list of daemon statuses.
 *
 * @author Roman Zenka
 */
public final class SwiftMonitor implements Runnable, Lifecycle {
	/**
	 * Guarded by {@link #connectionsLock}
	 */
	private final Map<DaemonConnection, DaemonStatus> monitoredConnections = new HashMap<DaemonConnection, DaemonStatus>(20);
	private final Map<DaemonConnection, ProgressListener> pingListeners = new HashMap<DaemonConnection, ProgressListener>(20);

	private RunningApplicationContext context;
	private MultiFactory factory;
	private ScheduledExecutorService scheduler;
	private final Object connectionsLock = new Object();

	public SwiftMonitor() {
	}

	private void initialize(final ApplicationConfig app) {
		if (app == null) {
			return;
		}
		clear();
		for (final DaemonConfig daemonConfig : app.getDaemons()) {
			final ServiceConfig pingServiceConfig = Daemon.getPingServiceConfig(daemonConfig);
			if (pingServiceConfig != null) {
				final DaemonConnection daemonConnection = (DaemonConnection) getFactory().createSingleton(pingServiceConfig, app.getDependencyResolver());
				monitoredConnections.put(daemonConnection, new DaemonStatus("No response yet"));
				pingListeners.put(daemonConnection, new PingListener(daemonConnection));
			}
		}
	}

	/**
	 * Start monitoring Swift.
	 */
	@Override
	public void start() {
		synchronized (connectionsLock) {
			if (!isRunning()) {
				initialize(context.getApplicationConfig());
				for (final DaemonConnection connection : monitoredConnections.keySet()) {
					connection.start();
				}
				if (scheduler == null) {
					scheduler = Executors.newScheduledThreadPool(1);
					scheduler.scheduleAtFixedRate(this, DaemonStatus.MONITOR_PERIOD_SECONDS, DaemonStatus.MONITOR_PERIOD_SECONDS, TimeUnit.SECONDS);
				}
			}
		}
	}

	/**
	 * Stop monitoring Swift.
	 */
	@Override
	public void stop() {
		synchronized (connectionsLock) {
			if (scheduler != null) {
				clear();
				scheduler.shutdown();
				scheduler = null;
				for (final DaemonConnection connection : monitoredConnections.keySet()) {
					connection.stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		synchronized (connectionsLock) {
			return scheduler != null;
		}
	}

	public void ping() {
		final List<DaemonConnection> copy;
		synchronized (connectionsLock) {
			copy = Lists.newArrayList(monitoredConnections.keySet());
		}
		for (final DaemonConnection connection : copy) {
			connection.sendWork(new PingWorkPacket(), pingListeners.get(connection));
		}
	}

	private void clear() {
		monitoredConnections.clear();
		pingListeners.clear();
	}

	/**
	 * @return Copy of the list of monitored connections. Includes the status information.
	 */
	public Map<DaemonConnection, DaemonStatus> getMonitoredConnections() {
		synchronized (connectionsLock) {
			return Maps.newHashMap(monitoredConnections);
		}
	}

	public MultiFactory getFactory() {
		return factory;
	}

	public void setFactory(final MultiFactory factory) {
		this.factory = factory;
	}

	public RunningApplicationContext getContext() {
		return context;
	}

	public void setContext(RunningApplicationContext context) {
		this.context = context;
	}

	@Override
	public void run() {
		ping();
	}

	private final class PingListener implements ProgressListener {
		private final DaemonConnection daemonConnection;

		PingListener(final DaemonConnection daemonConnection) {
			this.daemonConnection = daemonConnection;
		}

		@Override
		public void requestEnqueued(final String hostString) {
		}

		@Override
		public void requestProcessingStarted(final String hostString) {
		}

		@Override
		public void requestProcessingFinished() {
		}

		@Override
		public void requestTerminated(final Exception e) {
			synchronized (connectionsLock) {
				monitoredConnections.put(daemonConnection, new DaemonStatus(e.getMessage()));
			}
		}

		@Override
		public void userProgressInformation(final ProgressInfo progressInfo) {
			if (progressInfo instanceof PingResponse) {
				synchronized (connectionsLock) {
					monitoredConnections.put(daemonConnection, ((PingResponse) progressInfo).getStatus());
				}
			}
		}
	}
}
