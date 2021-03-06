package edu.mayo.mprc.heme.dao;

import com.google.common.base.Preconditions;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoBase;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Order;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.*;

/**
 * @author Roman Zenka
 */
@Repository("hemeDao")
public final class HemeDaoHibernate extends DaoBase implements HemeDao {
	@Override
	public Collection<String> getHibernateMappings() {
		final Collection<String> list = new ArrayList<String>(Arrays.asList("edu/mayo/mprc/heme/dao/HemeTest.hbm.xml"));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	@Override
	public List<HemeTest> getAllTests() {
		return listAndCast(getSession()
				.createCriteria(HemeTest.class)
				.setFetchMode("searchRun", FetchMode.JOIN)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
				.addOrder(Order.desc("date"))
				.addOrder(Order.asc("name")));
	}

	@Override
	public HemeTest addTest(final HemeTest test) {
		try {
			Preconditions.checkNotNull(test, "test must not be null");
			return save(test, true);
		} catch (Exception e) {
			throw new MprcException("Could not add " + test, e);
		}
	}

	@Override
	public void removeTest(final HemeTest test) {
		final Object o = getSession().createCriteria(HemeTest.class).add(test.getEqualityCriteria()).uniqueResult();
		if (o != null) {
			getSession().delete(o);
		}
	}

	@Override
	public long countTests() {
		return rowCount(HemeTest.class);
	}

	@Override
	public void saveOrUpdate(HemeTest test) {
		getSession().saveOrUpdate(test);
	}

	@Override
	public HemeTest getTestForId(final int testId) {
		final Object result = getSession().get(HemeTest.class, testId);
		if (!(result instanceof HemeTest)) {
			throw new MprcException(MessageFormat.format("Could not find {0} object of id {1}", HemeTest.class.getSimpleName(), testId));
		}
		return (HemeTest) result;
	}

	@Override
	public String check() {
		// Nothing to do
		return null;
	}

	@Override
	public void install(Map<String, String> params) {
		// Nothing to do
	}
}
