package edu.mayo.mprc.dbcurator.model.curationsteps;

import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.dbcurator.model.CurationDaoImpl;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * A base for all {@link edu.mayo.mprc.dbcurator.model.CurationDao} test cases.
 *
 * @author Roman Zenka
 */
public abstract class CurationDaoTestBase extends DaoTest {
	protected CurationDaoImpl curationDao;

	@BeforeClass()
	public void setup() {
		curationDao = new CurationDaoImpl();
		initializeDatabase(curationDao);
	}

	@AfterClass()
	public void teardown() {
		teardownDatabase();
	}

}
