package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;

import java.util.Date;

/**
 * @author Roman Zenka
 */
public class BulkLoadJob extends PersistableBase {
	private Date jobDate;

	public BulkLoadJob() {
		jobDate = new Date();
	}

	public Date getJobDate() {
		return jobDate;
	}

	public void setJobDate(Date date) {
		this.jobDate = date;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return DaoBase.nullSafeEq("id", getId());
	}
}
