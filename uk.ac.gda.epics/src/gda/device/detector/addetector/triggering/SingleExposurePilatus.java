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

package gda.device.detector.addetector.triggering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.detector.areadetector.v17.ADBase;
import gda.device.detector.areadetector.v17.ADDriverPilatus.PilatusTriggerMode;
import gda.scan.ScanInformation;

public class SingleExposurePilatus extends SingleExposureStandard {

	private static final Logger logger = LoggerFactory.getLogger(SingleExposurePilatus.class);

	private int sleepMillis = 0;			// ~1000-3000 ms are required on i12 in some circumstances (I12-452, I12-457)

	public SingleExposurePilatus(ADBase adBase, double readoutTime) {
		super(adBase, readoutTime);
	}

	/**
	 * Override in order stop acquisition before setting up. This is to work around strange
	 * epics Area Detector behaviours!
	 */
	@Override
	public void prepareForCollection(double collectionTime, int numImages, ScanInformation scanInfo) throws Exception {
		getAdBase().stopAcquiring();
		super.prepareForCollection(collectionTime, numImages, scanInfo);
		if (sleepMillis > 0) {
			logger.trace("Sleeping for {} ms...", sleepMillis);
			Thread.sleep(sleepMillis);
		}
	}

	@Override
	protected void configureTriggerMode() throws Exception {
		getAdBase().setTriggerMode(PilatusTriggerMode.INTERNAL.ordinal());
	}

	public int getSleepMillis() {
		return sleepMillis;
	}

	public void setSleepMillis(int sleepMillis) {
		this.sleepMillis = sleepMillis;
	}
}
