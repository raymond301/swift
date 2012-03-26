package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.PersistableListBase;

import java.util.Collection;

/**
 * A list of protein groups.
 *
 * @author Roman Zenka
 */
public final class ProteinGroupList extends PersistableListBase<ProteinGroup> {
	public ProteinGroupList() {
	}

	public ProteinGroupList(final Collection<ProteinGroup> items) {
		super(items);
	}

	public ProteinGroupList(final int initialCapacity) {
		super(initialCapacity);
	}
}
