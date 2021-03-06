/*-
 * Copyright © 2011 Diamond Light Source Ltd.
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

package gda.device.detector.addetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.detector.addetector.filewriter.FileWriterBase;

public class ADPco extends ADDetector {

	private static Logger logger = LoggerFactory.getLogger(ADPco.class);

	public void initialiseFileWriterPluginImageSizeByTakingExposure() throws Exception {
		logger.info("Epics kludge: Exposing a single image to initialise image size in file writing plugin");
		// TODO if still required, move into FileWriters and add to interface
		getFileWriter().completeCollection(); // ensure the thing is not recording!
		((FileWriterBase) getFileWriter()).enableCallback(true);
		getCollectionStrategy().prepareForCollection(.01, 1, null);
		collectData();
		waitWhileBusy();
		endCollection();
		((FileWriterBase) getFileWriter()).enableCallback(false);
		logger.info("Epics kludge complete");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

}
