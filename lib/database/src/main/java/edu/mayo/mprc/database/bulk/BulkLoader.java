package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.database.SessionProvider;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;

/**
 * Load a large list of values into the database.
 * <p/>
 * If the value already exist - use the existing one.
 * <p/>
 * If the value does not match existing - create new record.
 * <p/>
 * Fill in ids of all the values as they are in the database.
 * <p/>
 * Finally, and most importantly - do this FAST.
 *
 * @author Roman Zenka
 */
public abstract class BulkLoader<T extends PersistableBase> {
	public static final int BATCH_SIZE = 100;

	private final BulkLoadJobStarter jobStarter;
	private final SessionProvider sessionProvider;

	protected BulkLoader(final BulkLoadJobStarter jobStarter, final SessionProvider sessionProvider) {
		this.jobStarter = jobStarter;
		this.sessionProvider = sessionProvider;
	}

	/**
	 * @return name of the temporary table. The table must contain 'new_id', 'data_order'
	 */
	public abstract String getTempTableName();

	public abstract String getTableName();

	public String getTableIdColumn() {
		return getTableName() + "_id";
	}

	public abstract String getEqualityString();

	/**
	 * @param value value to wrap
	 * @return wrapped value
	 */
	public abstract Object wrapForTempTable(T value, TempKey key);

	/**
	 * Comma separated list of columns to transfer from temp table to the actual one.
	 */
	public abstract String getColumnsToTransfer();

	public void addObjects(final Collection<? extends T> values) {
		final BulkLoadJob bulkLoadJob = jobStarter.startNewJob();

		// Load data quickly into temp table
		final int numAddedValues = loadTempValues(values, bulkLoadJob);

		if (numAddedValues > 0) {
			final int recordsUpdated;
			try {
				recordsUpdated = updateExisting(bulkLoadJob);
			} catch (Exception e) {
				throw new MprcException("Bulk update step 1 failed", e);
			}

			if (recordsUpdated != numAddedValues) {
				throw new MprcException(MessageFormat.format("Programmer error: we were supposed to update {0}, instead updated {1}",
						numAddedValues, recordsUpdated));
			}

			final int lastId = getLastId();

			try {
				insertMissing(bulkLoadJob, lastId);
			} catch (Exception e) {
				throw new MprcException("Bulk insert step 2 failed", e);
			}

			getSession().flush();
			getSession().clear();

			loadNewIdsBack(values, bulkLoadJob, lastId);

			deleteTemp(bulkLoadJob, numAddedValues);
		}

		jobStarter.endJob(bulkLoadJob);
	}

	protected int loadTempValues(final Collection<? extends T> values, final BulkLoadJob bulkLoadJob) {
		int order = 0;
		int numAddedValues = 0;
		for (final T value : values) {
			if (value.getId() == null) {
				order++;
				final TempKey key = new TempKey(bulkLoadJob.getId(), order);
				final Object load = wrapForTempTable(value, key);
				getSession().save(load);
				getSession().setReadOnly(load, true);
				numAddedValues++;
				if (order % BATCH_SIZE == 0) {
					getSession().flush();
					getSession().clear();
				}
			}
		}
		getSession().flush();
		getSession().clear();
		return numAddedValues;
	}

	protected int updateExisting(final BulkLoadJob bulkLoadJob) {
		final String table = getTableName();
		final String tableId = getTableIdColumn();
		final String tempTableName = getTempTableName();
		final String equalityString = getEqualityString();

		final SQLQuery sqlQuery = getSession().createSQLQuery("UPDATE " + tempTableName + " AS t SET t.new_id = (select s." + tableId + " from " + table + " as s where " + equalityString + " and t.job = :job)");
		sqlQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		final int update1 = sqlQuery.executeUpdate();
		return update1;
	}

	protected int getLastId() {
		final String table = getTableName();
		final String tableId = getTableIdColumn();

		try {
			final Integer lastId = (Integer) getSession()
					.createSQLQuery("select max(" + tableId + ") from " + table)
					.uniqueResult();
			if (lastId == null) {
				return 0;
			}
			return lastId;
		} catch (Exception e) {
			throw new MprcException("Could not determine last id in table " + table, e);
		}
	}

	protected void insertMissing(final BulkLoadJob bulkLoadJob, final int lastId) {
		final String table = getTableName();
		final String tableId = getTableIdColumn();
		final String tempTableName = getTempTableName();
		final String columnsToTranfer = getColumnsToTransfer();
		final Query query = getSession()
				.createSQLQuery(
						MessageFormat.format(
								"INSERT INTO {0} ({1}, {2}) select data_order+{3}, {4} from {5} where job = :job and new_id is null",
								table, tableId, columnsToTranfer, lastId, columnsToTranfer, tempTableName))
				.setParameter("job", bulkLoadJob.getId());
		query.executeUpdate();
	}

	protected void loadNewIdsBack(final Collection<? extends T> values, final BulkLoadJob bulkLoadJob, final int lastId) {
		final Iterator<? extends T> iterator = values.iterator();
		int order = lastId;
		while (iterator.hasNext()) {
			final T value = nextNullIdValue(iterator);
			if (value == null) {
				break;
			}

			order++;
			value.setId(order);
		}
	}

	protected void deleteTemp(final BulkLoadJob bulkLoadJob, final int numAddedValues) {
		final String tempTableName = getTempTableName();
		final SQLQuery deleteQuery = getSession().createSQLQuery(
				MessageFormat.format(
						"DELETE FROM {0} WHERE job=:job", tempTableName));
		deleteQuery.setParameter("job", bulkLoadJob.getId()).setReadOnly(true);
		final int numDeleted = deleteQuery.executeUpdate();
		if (numDeleted != numAddedValues) {
			throw new MprcException("Could not delete all the elements from the temporary table");
		}
	}

	private T nextNullIdValue(final Iterator<? extends T> iterator) {
		T value = iterator.next();
		// Skip all the values already saved
		while (value != null && value.getId() != null) {
			value = iterator.next();
		}
		return value;
	}

	public Session getSession() {
		return sessionProvider.getSession();
	}
}