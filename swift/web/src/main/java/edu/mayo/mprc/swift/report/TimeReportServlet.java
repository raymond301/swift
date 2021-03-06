package edu.mayo.mprc.swift.report;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.searchdb.dao.SearchRunFilter;
import edu.mayo.mprc.swift.ReportUtils;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.db.TimeReport;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Controller
public final class TimeReportServlet {
	private transient SearchDbDao searchDbDao;
	private transient SwiftDao swiftDao;

	public TimeReportServlet() {
	}

	@RequestMapping(value = "/time-report", method = RequestMethod.GET)
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final SearchRunFilter filter;
		final boolean screen;
		try {
			screen = req.getParameter("screen") != null;
			filter = parseSearchRunFilter(req.getParameter("start"), req.getParameter("end"));
			prepareHeader(resp, filter, screen);
		} catch (final MprcException e) {
			throw new ServletException("Cannot provide time report", e);
		}

		try {
			final ServletOutputStream out = resp.getOutputStream();
			searchDbDao.begin();
			printReport(filter, out, screen ? '\t' : ',');
			searchDbDao.commit();
		} catch (final MprcException e) {
			searchDbDao.rollback();
			throw new ServletException("Cannot provide time report", e);
		}
	}

	private void prepareHeader(final HttpServletResponse resp, final SearchRunFilter filter, final boolean screen) {
		resp.setHeader("Cache-Control", "no-cache");
		if (!screen) {
			resp.setHeader("Content-disposition", "attachment; filename=" + getReportFilename(filter));
			resp.setContentType("text/csv");
		} else {
			resp.setContentType("text/plain");
		}
	}

	private static String getReportFilename(final SearchRunFilter filter) {
		final String startDate = new DateTime(filter.getStartDate()).toString("yyyy-MM-dd");
		final String endDate = new DateTime(filter.getEndDate()).toString("yyyy-MM-dd");
		return "swift_time_report__" + startDate + "__" + endDate + ".csv";
	}

	private static SearchRunFilter parseSearchRunFilter(final String startParam, final String endParam) {
		final SearchRunFilter filter;
		final DateTime start = ReportUtils.parseDate(startParam, "start");
		final DateTime end = ReportUtils.parseDate(endParam, "end");
		filter = new SearchRunFilter();
		filter.setStartDate(start.toDate());
		filter.setEndDate(end.toDate());
		filter.setShowHidden(true);
		return filter;
	}

	private void printReport(final SearchRunFilter filter, final ServletOutputStream out, final char separator) throws IOException {
		final List<SearchRun> searchRuns = searchDbDao.getSearchRunList(filter, false);
		out.println("Search run" + separator + "Start time" + separator + "Elapsed time" + separator + "Consumed time" + separator + "Productive time");
		for (final SearchRun searchRun : searchRuns) {
			// TODO: Optimize this - fetch the task data for all search runs at once, do not order
			final List<TaskData> taskDataList = swiftDao.getTaskDataList(searchRun.getId());
			out.print(searchRun.getTitle());
			out.print(separator);
			out.print(searchRun.getStartTimestamp().toString());
			out.print(separator);
			out.print(TimeReport.elapsedTime(searchRun));
			out.print(separator);
			out.print(TimeReport.consumedTime(taskDataList));
			out.print(separator);
			out.print(TimeReport.productiveTime(taskDataList));
			out.println();
		}
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	@Resource(name = "searchDbDao")
	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}
}
