package com.atlas.webservice;

import javax.ws.rs.ApplicationPath;
import com.dassault_systemes.platform.restServices.ModelerBase;

@ApplicationPath(ModelerBase.REST_BASE_PATH+ "/gapWebService")


public class gapWebServiceModeler extends ModelerBase{

	@Override
	public Class<?>[] getServices() {
		return new Class<?>[] {gapWebService.class};
	}
}