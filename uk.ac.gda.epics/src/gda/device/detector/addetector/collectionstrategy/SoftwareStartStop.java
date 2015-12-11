/*-
 * Copyright © 2015 Diamond Light Source Ltd.
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

package gda.device.detector.addetector.collectionstrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Collection strategy can be used where the acquisition must be started during collectData() and stopped during
 * completeCollection().
 *
 * This strategy does not set the Trigger Mode, so should be wrapped with a {@link TriggerModeDecorator} as appropriate (use 
 * {@link InternalTriggerModeDecorator} for the equivalent of the old SingleExposureStandard with the default internal trigger mode).
 *
 * This strategy does not set the Image Mode, so should be wrapped with an ImageModeDecorator as appropriate (use
 * {@link SingleImageModeDecorator} for the equivalent of the old SimpleAcquire or SingleExposureStandard). 
 *
 * Note, this collection strategy ignores the now deprecated NXCollectionStrategyPlugin.configureAcquireAndPeriodTimes method,
 * so support for AbstractADTriggeringStrategy properties such as accumulation Mode and readoutTime will have to be implemented
 * by decorators.
 */
public class SoftwareStartStop extends AbstractADCollectionStrategy {

	private static final Logger logger = LoggerFactory.getLogger(SoftwareStartStop.class);

	// NXCollectionStrategyPlugin interface

	@Override
	public void collectData() throws Exception {
		getAdBase().startAcquiring();
	}

	@Override
	public int getNumberImagesPerCollection(double collectionTime) throws Exception {
		logger.trace("getNumberImagesPerCollection({}) called, ignoring collectionTime & returning 1.", collectionTime);
		return 1;
	}

	@Override
	public void completeCollection() throws Exception {
		getAdBase().stopAcquiring();
	}

	@Override
	public void atCommandFailure() throws Exception {
		completeCollection();
	}

	@Override
	public void stop() throws Exception {
		completeCollection();
	}
}