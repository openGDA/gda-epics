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

package uk.ac.gda.epics.client.views.model;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import uk.ac.gda.epics.client.views.controllers.IAdBaseViewController;

public interface AdBaseModel {

	String getPortName() throws Exception;

	/**
	 * @throws InterruptedException
	 * @throws CAException
	 * @throws TimeoutException
	 */
	short getDetectorState_RBV() throws Exception;

	int getArrayCounter_RBV() throws Exception;

	/**
	 *
	 */
	double getTimeRemaining_RBV() throws Exception;

	/**
	 *
	 */
	double getArrayRate_RBV() throws Exception;

	/**
	 *
	 */
	int getNumExposuresCounter_RBV() throws Exception;

	/**
	 *
	 */
	int getNumImagesCounter_RBV() throws Exception;

	double getAcqExposureRBV() throws Exception;

	/**
	 * Sets the exposure time
	 *
	 * @param exposureTime
	 * @throws Exception
	 */
	void setAcqExposure(double exposureTime) throws Exception;

	double getAcqPeriodRBV() throws Exception;

	short getAcquireState() throws Exception;

	String getDatatype() throws Exception;

	boolean registerAdBaseViewController(IAdBaseViewController takeFlatController);

	boolean removeAdBaseViewController(IAdBaseViewController takeFlatController);

	void setAcqPeriod(double periodTime) throws Exception;

	int getNumberOfExposuresPerImage_RBV() throws Exception;

	int getNumberOfImages_RBV() throws Exception;

}