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

package gda.device.simplearray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.DeviceException;
import gda.device.SimpleArray;
import gda.device.scannable.ScannableBase;
import gda.epics.connection.EpicsChannelManager;
import gda.epics.connection.EpicsController;
import gda.epics.connection.InitializationListener;
import gda.factory.FactoryException;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * Class implements EPICS SimpleArray interface (record type in GDA interface is 'array', in EPICS is 'waveform').
 * It supports variable length String IO to EPICS waveform record. It has build-in string to/from int array conversion.
 * The object of this class will monitor changes in waveform as well.
 */
public class EpicsSimpleArray extends ScannableBase implements SimpleArray, InitializationListener {

	private static final Logger logger = LoggerFactory.getLogger(EpicsSimpleArray.class);

	private String pvName;

	private String latestValue;

	private Channel theChannel;

	protected EpicsChannelManager channelManager;

	protected EpicsController controller;

	private ValueMonitorListener valueMonitor;

	/**
	 * The Constructor.
	 */
	public EpicsSimpleArray() {
		controller = EpicsController.getInstance();
		channelManager = new EpicsChannelManager(this);
		valueMonitor = new ValueMonitorListener();
	}

	@Override
	public void configure() throws FactoryException {
		if (isConfigured()) {
			return;
		}
		this.inputNames = new String[]{getName()};

		if (!isConfigured()) {
			if (getPvName() == null) {
				logger.error("Control point not properly specified", getName());
				throw new FactoryException("Control point not properly specified for the control point " + getName());
			}
			try {
				theChannel = channelManager.createChannel(getPvName(), valueMonitor, false);
			} catch (CAException e) {
				throw new FactoryException("Failed to create channel for SimpleArray " + getName(), e);
			}
		}
		channelManager.creationPhaseCompleted();

		setConfigured(true);
	}

	@Override
	public String getValue() throws DeviceException {
		char[] value=new char[theChannel.getElementCount()];
		try {
			int[] waveform = controller.cagetIntArray(theChannel);
			for (int i=0; i<waveform.length; i++){
				value[i]=(char)waveform[i];
			}
		} catch (Exception e) {
			throw new DeviceException(getName() + " exception in getValue", e);
		}
		return latestValue = new String(latestValue);
	}

	@Override
	public void setValue(String newValue) throws DeviceException {
		int[] waveform = new int[newValue.length()];
		try {
			for (int i=0; i<newValue.length(); i++){
				char c = newValue.charAt(i);
				waveform[i]=c;
			}
			controller.caput(theChannel, waveform);
		} catch (Exception e) {
			throw new DeviceException(getName() + " exception in setValue", e);
		}
	}

	/**
	 * Returns the name of the pv this object is monitoring
	 *
	 * @return the name of the pv
	 */
	public String getPvName() {
		return pvName;
	}

	/**
	 * Sets the name of the pv this object monitors. This must be called before the configure method makes the
	 * connections to the pv.
	 *
	 * @param pvName
	 */

	public void setPvName(String pvName) {
		this.pvName = pvName;
	}

	@Override
	public void initializationCompleted() {
		logger.debug("SimpleArray - {} is initialised.", getName());
	}

	private class ValueMonitorListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			// extract the value and confirm its type
			DBR dbr = arg0.getDBR();
			// update the latest value
			int count = dbr.getCount();
			char[] value=new char[count];
			int[] waveform =((DBR_Int)dbr).getIntValue();
			for (int i=0; i<count; i++){
				value[i]=(char)waveform[i];
			}
			latestValue = new String(value);
		}
	}

	public void destroy() throws IllegalStateException, CAException {
		theChannel.destroy();
	}

	@Override
	public void asynchronousMoveTo(Object position) throws DeviceException {
		if (position instanceof String) setValue((String)position);
		else logger.error("{} only supports a String input", getName());

	}

	@Override
	public Object getPosition() throws DeviceException {
		return latestValue;
	}

	@Override
	public boolean isBusy() throws DeviceException {
		return false;
	}
}
