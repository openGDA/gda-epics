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

package gda.device.enumpositioner;

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.ControllerRecord;
import gda.device.DeviceException;
import gda.device.EnumPositioner;
import gda.device.EnumPositionerStatus;
import gda.epics.connection.EpicsChannelManager;
import gda.epics.connection.EpicsController;
import gda.epics.connection.InitializationListener;
import gda.factory.FactoryException;
import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;
import uk.ac.gda.api.remoting.ServiceInterface;

/**
 * This class maps onto the EPICS PneumaticCallback template.
 */
@ServiceInterface(EnumPositioner.class)
public class EpicsSimplePneumatic extends EnumPositionerBase implements InitializationListener, ControllerRecord {
	private static final Logger logger = LoggerFactory.getLogger(EpicsSimplePneumatic.class);

	private EpicsController controller;
	private EpicsChannelManager channelManager;

	private Channel control = null;

	protected String pvName;

	private PutCallbackListener pcl;
	private Status callbackstatus = Status.NO_ALARM;
	private Severity callbackseverity = Severity.NO_ALARM;

	private StatusMonitorListener statusMonitor;

	private Object lock = new Object();

	protected Vector<String> statuspositions = new Vector<String>();

	private boolean readOnly = false;

	/**
	 * constructor
	 */
	public EpicsSimplePneumatic() {
		super();
		channelManager = new EpicsChannelManager(this);
		controller = EpicsController.getInstance();
		statusMonitor = new StatusMonitorListener();
		pcl = new PutCallbackListener();
	}


	@Override
	public void configure() throws FactoryException {
		try {
			if (!isConfigured()) {
				this.inputNames = new String[] { getName() };
				this.outputFormat = new String[]{"%s"};

//				control = channelManager.createChannel(pvName, false);
				control = channelManager.createChannel(pvName, statusMonitor, false);

				channelManager.creationPhaseCompleted();
				channelManager.tryInitialize(100);

				setConfigured(true);
			} // end of if(!configured)

		} catch (Exception e) {
			throw new FactoryException("failed to create channel " + pvName, e);
		}
	}

	/**
	 * gets the current status position of this device.
	 *
	 * @return position in String
	 * @throws DeviceException
	 */
	@Override
	public String getPosition() throws DeviceException {
		try {
			if (control != null) {
				short test = controller.cagetEnum(control);
				return statuspositions.get(test);
			}

			return getName() + " : NOT Available.";

		} catch (Exception e) {
			throw new DeviceException("failed to get status position from " + control.getName(), e);
		}
	}


	@Override
	public void rawAsynchronousMoveTo(Object position) throws DeviceException {
		if (isReadOnly()) {
			throw new DeviceException("Cannot move " + getName() + " as it is configured (within the gda software) to be read only");
		}

		// find in the positionNames array the index of the string
		if (containsPosition(position.toString())) {
			int target = getPositionIndex(position.toString());
			try {
				if (getStatus() == EnumPositionerStatus.MOVING) {
					logger.warn("{} is busy", getName());
					return;
				}

				setPositionerStatus(EnumPositionerStatus.MOVING);
				controller.caput(control, target, pcl);
			} catch (CAException e) {
				setPositionerStatus(EnumPositionerStatus.ERROR);
				throw new DeviceException(control.getName() + " failed to moveTo " + position.toString() + "\n!!! Epics Channel Access problem", e);
			} catch (Exception e) {
				setPositionerStatus(EnumPositionerStatus.ERROR);
				throw new DeviceException(control.getName() + " failed to moveTo " + position.toString() + "\n!!! ", e);
			}
			return;
		}

		// if get here then wrong position name supplied
		throw new DeviceException("Position called \'" + position.toString() + "\' not found.");

	}

	@Override
	public void stop() throws DeviceException {
		//throw new DeviceException("stop() operation is not available for " + getName());
	}

	/**
	 * gets the available positions from this device.
	 *
	 * @return the available positions from this device.
	 * @throws DeviceException
	 */
	@Override
	public String[] getPositions() throws DeviceException {
		try {
			return controller.cagetLabels(control);
		} catch (Exception e) {
			throw new DeviceException(getName() + " exception in getPositions",e);
		}
	}

	/**
	 * gets the available status positions from this device.
	 *
	 * @return the available status positions from this device.
	 * @throws DeviceException
	 */
	public String[] getControlPositions() throws DeviceException {
		try {
			return controller.cagetLabels(control);
		} catch (Exception e) {
			throw new DeviceException(getName() + " exception in getStatusPositions",e);
		}
	}

	@Override
	public void initializationCompleted() throws DeviceException {
		for (String position : getPositions()) {
			if (position != null && !position.isEmpty()) {
				addPosition(position);
			}
		}
		for (String statusPosition : getControlPositions()) {
			if (statusPosition != null && !statusPosition.isEmpty()) {
				this.statuspositions.add(statusPosition);
			}
		}
		logger.info("{} is initialised.", getName());
	}

	/**
	 * update PneumaticCallback status from EPICS.
	 */
	private class StatusMonitorListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			short value = -1;
			DBR dbr = arg0.getDBR();
			if (dbr.isENUM()) {
				value = ((DBR_Enum) dbr).getEnumValue()[0];
			}
			if (value == 0) {
				synchronized (lock) {
					setPositionerStatus(EnumPositionerStatus.ERROR);
				}
			} else if (value == 1 || value == 3) {
				synchronized (lock) {
					setPositionerStatus(EnumPositionerStatus.IDLE);
				}
			} else if (value == 2 || value == 4) {
				synchronized (lock) {
					setPositionerStatus(EnumPositionerStatus.MOVING);
				}
			}
			notifyIObservers(this, getPositionerStatus());
		}
	}

	private class PutCallbackListener implements PutListener {
		volatile PutEvent event = null;

		@Override
		public void putCompleted(PutEvent ev) {
			try {
				logger.debug("caputCallback complete for {}", getName());
				event = ev;

				if (event.getStatus() != CAStatus.NORMAL) {
					logger.error("Put failed. Channel {} : Status {}", ((Channel) event.getSource()).getName(), event
							.getStatus());
					setPositionerStatus(EnumPositionerStatus.ERROR);
				} else {
					logger.info("{} move done", getName());
					setPositionerStatus(EnumPositionerStatus.IDLE);
				}

				if (callbackstatus == Status.NO_ALARM && callbackseverity == Severity.NO_ALARM) {
					logger.info("{} moves OK", getName());
					setPositionerStatus(EnumPositionerStatus.IDLE);
				} else {
					// if Alarmed, check and report MSTA status
					logger.error("{} reports Alarm: {}", getName(), control);
					setPositionerStatus(EnumPositionerStatus.ERROR);
				}

			} catch (Exception ex) {
				logger.error("Error in putCompleted for {}", getName(), ex);
			}
		}
	}

	public String getPvName() {
		return pvName;
	}

	public void setPvName(String pvName) {
		this.pvName = pvName;
	}

	@Override
	public String getControllerRecordName() {
		return getPvName();
	}


	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
