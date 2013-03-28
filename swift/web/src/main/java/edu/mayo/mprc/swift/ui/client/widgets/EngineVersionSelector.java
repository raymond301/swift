package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class EngineVersionSelector extends HorizontalPanel implements Comparable<EngineVersionSelector> {
	private final String code;
	private final CheckBox checkBox;
	private ListBox versions;
	private final List<String> versionList;
	private final int order;

	public EngineVersionSelector(final ClientSearchEngine engine) {
		code = engine.getEngineConfig().getCode();
		order = engine.getOrder();
		checkBox = new CheckBox(engine.getFriendlyName());
		checkBox.setEnabled(engine.isOnByDefault());
		versionList = new ArrayList<String>(2);
		versionList.add(engine.getEngineConfig().getVersion());
		setStyleName("engine-version-selector", true);
	}

	public void addEngine(final ClientSearchEngine engine) {
		final String code = engine.getEngineConfig().getCode();
		if (this.code != code) {
			throw new RuntimeException("Adding engine with wrong code " + code + " to selector " + this.code + " - a programmer error");
		}
		versionList.add(engine.getEngineConfig().getVersion());
	}

	/**
	 * Call after last engine was added.
	 */
	public void done() {
		if (versionList.size() > 1) {
			versions = new ListBox(false);
			Collections.sort(versionList);
			for (final String version : versionList) {
				versions.addItem(version);
			}
			versions.setSelectedIndex(versions.getItemCount() - 1);
			add(checkBox);
			add(versions);
		} else {
			checkBox.setText(checkBox.getText() + " " + versionList.get(0));
			add(checkBox);
		}
	}

	public boolean isEnabled() {
		return Boolean.TRUE.equals(checkBox.getValue());
	}

	public String getVersion() {
		if (versions == null) {
			return versionList.get(0);
		} else {
			return versions.getValue(versions.getSelectedIndex());
		}
	}

	@Override
	public int compareTo(EngineVersionSelector o) {
		return this.order < o.order ? -1 : (this.order == o.order ? 0 : 1);
	}

	public String getCode() {
		return code;
	}
}
