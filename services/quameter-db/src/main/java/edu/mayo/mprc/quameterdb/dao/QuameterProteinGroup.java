package edu.mayo.mprc.quameterdb.dao;

import com.google.common.base.Objects;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.EvolvableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Stores information about a named protein group.
 * <p/>
 * A protein group has a short, user-assigned name, and a regex that matches all the accession numbers
 * for proteins in this group.
 * <p/>
 * The group is stored in the database so it can be referenced by {@link QuameterResult} objects.
 * These store spectral counts for each protein group.
 *
 * @author Roman Zenka
 */
public final class QuameterProteinGroup extends EvolvableBase {
	private String name;
	private String regex;

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("name", getName()))
				.add(DaoBase.nullSafeEq("regex", getRegex()));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name, regex);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuameterProteinGroup other = (QuameterProteinGroup) obj;
		return Objects.equal(this.name, other.name) && Objects.equal(this.regex, other.regex);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}
}