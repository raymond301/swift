package edu.mayo.mprc.swift.webservice;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.activation.FileTypeMap;
import java.io.File;

/**
 * @author Roman Zenka
 */
public final class TestQaResources {
	@Test
	public void shouldUseReasonableDefaultTypeMap() {
		FileTypeMap map = QaReport.TYPE_MAP;
		Assert.assertEquals(map.getContentType(new File("hello.png")), "image/png");
		Assert.assertEquals(map.getContentType(new File("hello.jpg")), "image/jpeg");
		Assert.assertEquals(map.getContentType(new File("hello.html")), "text/html");
		Assert.assertEquals(map.getContentType(new File("hello.xls")), "application/vnd.ms-excel");
	}
}
