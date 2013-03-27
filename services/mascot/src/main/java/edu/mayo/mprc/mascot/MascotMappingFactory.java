package edu.mayo.mprc.mascot;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("mascotMappingFactory")
public final class MascotMappingFactory implements MappingFactory {
	private static final long serialVersionUID = 20121221L;
	public static final String MASCOT = "MASCOT";
	private ParamsInfo paramsInfo;

	public MascotMappingFactory() {
	}

	@Override
	public String getSearchEngineCode() {
		return MASCOT;
	}

	public String getCanonicalParamFileName() {
		return "mascot.params";
	}

	public ParamsInfo getParamsInfo() {
		return paramsInfo;
	}

	@Resource(name = "paramsInfo")
	public void setParamsInfo(final ParamsInfo paramsInfo) {
		this.paramsInfo = paramsInfo;
	}

	public Mappings createMapping() {
		return new MascotMappings(paramsInfo);
	}

}

