/*-
 * Copyright © 2009 Diamond Light Source Ltd.
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

package gda.device.robot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.DeviceBase;
import gda.device.DeviceException;
import gda.epics.connection.EpicsChannelManager;
import gda.epics.connection.EpicsController;
import gda.epics.connection.InitializationListener;
import gda.factory.FactoryException;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * CurrentSamplePosition Class
 */
public class CurrentSamplePosition extends DeviceBase implements InitializationListener {

	private static final Logger logger = LoggerFactory.getLogger(CurrentSamplePosition.class);

	// control fields
	/**
	 * sample position demanding channel
	 */
	private Channel sampleNumberChannel;
	/**
	 * EPICS controller for CA methods
	 */
	private EpicsController controller;
	/**
	 * EPICS Channel Manager
	 */
	private EpicsChannelManager channelManager;
	private String pvName;
	private CurrentSamplePositionListener csposls;
	private double sampleNumber;

	/**
	 * Constructor
	 */
	public CurrentSamplePosition() {
		controller = EpicsController.getInstance();
		channelManager = new EpicsChannelManager(this);
		csposls = new CurrentSamplePositionListener();
	}

	@Override
	public void configure() throws FactoryException {
		if (!isConfigured()) {
			if (getPvName() == null) {  // Nothing specified in Server XML file
				logger.error("Missing PV for Actual Carousel sample position {}", getName());
				throw new FactoryException("Missing PV for Actual Carousel sample position " + getName());
			}
			createChannelAccess(getPvName());
			channelManager.tryInitialize(100);
			setConfigured(true);
		}
	}

	/**
	 * creates all required channels
	 *
	 * @param config
	 * @throws FactoryException
	 */
	private void createChannelAccess(String pv) throws FactoryException {
		try {
			sampleNumberChannel = channelManager.createChannel(pv, csposls, false);
			// acknowledge that creation phase is completed
			channelManager.creationPhaseCompleted();
		} catch (Throwable th) {
			throw new FactoryException("failed to create all channels", th);
		}
	}

	/**
	 * gets the actual sample position number
	 *
	 * @return actual sample position
	 * @throws DeviceException
	 */
	public double getActualSamplePosition() throws DeviceException {
		try {
			return controller.cagetDouble(sampleNumberChannel);
		} catch (Throwable e) {
			throw new DeviceException("Failed to get actual sample position number. ", e);
		}
	}

	@Override
	public void initializationCompleted() {

		logger.info("Actual Carousel Sample position number is initialised.");

	}

	/**
	 * CurrentSamplePositionListener Class
	 */
	public class CurrentSamplePositionListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			DBR state = arg0.getDBR();
			double s = -1.0;
			if (state.isDOUBLE()) {
				s = ((DBR_Double) state).getDoubleValue()[0];
			} else {
				logger.error("Expecting double from EPICS but got {} ", state.getType());
				throw new IllegalStateException("Sample State returns wrong value type" + state.getType());
			}
			if (s != -1.0) {
				sampleNumber = s;
			} else {
				logger.error("Expecting value in range(1,200) from EPICS but got {} ", s);
				throw new IllegalStateException("Sample State returns wrong value : " + s);
			}
			notifyIObservers(this, sampleNumber);

		}
	}

	public String getPvName() {
		return pvName;
	}

	public void setPvName(String pvName) {
		this.pvName = pvName;
	}

}
