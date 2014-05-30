/*-
 * Copyright © 2014 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.gda.epics.adviewer;

import gda.rcp.util.OSGIServiceRegister;

import java.util.Dictionary;

import org.springframework.util.StringUtils;

import uk.ac.gda.epics.adviewer.views.ADUtils;
import uk.ac.gda.util.dictionary.MapBasedDictionary;


public class ADControllerFactory {
	
	//TODO Make the following match convention

	private static ADControllerFactory instance;
	
	
	public static ADControllerFactory getInstance(){
		if( instance == null){
			instance = new ADControllerFactory();
		}
		return instance;
	}
	public void registerADController(String serviceName) throws Exception{
		
		String detectorName = ADUtils.getDetectorNameFromPVServiceName(serviceName);
		String suffixType = ADUtils.getSuffixTypeFromPVServiceName(serviceName);
		//from pv prefix get plugin suffices
		ADPVSuffices adPVSuffices=null;;
		if( StringUtils.hasText(suffixType)){
			adPVSuffices = (ADPVSuffices)Activator.getNamedService(ADPVSuffices.class, suffixType);
		}
		if (adPVSuffices == null)
			adPVSuffices = new DLSADPVSuffices();
		
		DynamicADControllerImpl impl = new DynamicADControllerImpl(serviceName, detectorName, ADUtils.getPVFromPVServiceName(serviceName), adPVSuffices);
		
		OSGIServiceRegister modelReg = new OSGIServiceRegister();
		modelReg.setClass(ADController.class);
		modelReg.setService(impl);
		@SuppressWarnings("unchecked")
		Dictionary<String, String> properties = new MapBasedDictionary();
		properties.put(Activator.SERVICE_NAME, serviceName);
		modelReg.setProperties(properties);
		modelReg.afterPropertiesSet();	
	}
	
	
	public ADController getADController(String serviceName) throws Exception {
		ADController config = (ADController)Activator.getNamedService(ADController.class, serviceName);
		if( config == null){
			ADControllerFactory.getInstance().registerADController(serviceName);
			config = (ADController)Activator.getNamedService(ADController.class, serviceName);
			if (config == null)
				throw new Exception("Unable to access ADController for service '" + serviceName + "'");
		}
		return config;
	}	
}
