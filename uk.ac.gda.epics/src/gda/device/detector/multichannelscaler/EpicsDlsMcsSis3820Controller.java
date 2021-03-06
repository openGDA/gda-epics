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

package gda.device.detector.multichannelscaler;

import static java.util.stream.IntStream.rangeClosed;

import java.util.HashMap;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.DeviceBase;
import gda.device.DeviceException;
import gda.device.MCAStatus;
import gda.epics.connection.EpicsChannelManager;
import gda.epics.connection.EpicsController;
import gda.epics.connection.InitializationListener;
import gda.factory.FactoryException;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

/**
 * mapping class for EPICS dlsMcsSIS3820 template. Note the current implementation treats MCA records in this template
 * as PV only because we only use it to collect spectrum data from detectors, all the controls for MCAs are wrapped by
 * this template. This may change in the future.
 */
public class EpicsDlsMcsSis3820Controller extends DeviceBase implements InitializationListener {

	private static final Logger logger = LoggerFactory.getLogger(EpicsDlsMcsSis3820Controller.class);

	/** Maximum number of MCA record supported by EPICS DlsMcsSis3820 template */
	public static final int MAXIMUM_NUMBER_OF_MCA = 32;

	/** Maximum number of bins in spectrum */
	public static final int MAXIMUM_NUMBER_BINS = 60000;

	/** Acquisition flag */
	private boolean acquisitionDone = true;

	private double elapsedRealTimeValue;

	public enum AcquisitionProperty {
		STATUS,
		ELAPSEDTIME,
		DATA
	}

	// control fields
	/** Clear all spectrum and start acquiring, 1 start, 0 rest */
	private Channel eraseStartChannel;

	/** Hardware to erase data array, 1 to erase */
	private Channel eraseChannel;

	/** Start acquiring, 1 start, 0 rest */
	private Channel startChannel;

	/** Stop acquiring, 1 stop, 0 rest */
	private Channel stopChannel;

	/** Number of bins to use in the spectrum */
	private Channel nbinsChannel;

	/** Data update rate */
	private Channel readrateChannel;

	/** Real time since start of acquisition */
	private Channel trealChannel;

	/** Acquiring status, 1 acquiring, 0 done */
	private Channel acqChannel;

	/** Total acquisition time */
	private Channel tacqChannel;

	/** Integration time [s] for incrementing bin number */
	private Channel tdwellChannel;

	/** Channel-advance source, 0 internal, 1 external */
	private Channel binadvChannel;

	/** External bin advance prescaler */
	private Channel extpreChannel;

	/** EPICS controller for CA methods */
	private EpicsController controller;

	/** EPICS Channel Manager */
	private EpicsChannelManager channelManager;

	/** EPICS record name */
	private String recordName;

	/** EPICS MCA controller array */
	private Channel[] data;

	private AcqStatusListener acqlistener;

	private RealTimeListener rtimelistener;

	private HashMap<Integer, Channel> channelIndexMap = new HashMap<Integer, Channel>();

	/** The actual number of active MCA records used in this class */
	private int numberOfMca = Integer.MIN_VALUE;

	private Vector<String> readUpdateRates = new Vector<String>();
	private boolean pollElapsedRealTime=false;

	private Monitor realtimemonitor;

	public EpicsDlsMcsSis3820Controller() {
		controller = EpicsController.getInstance();
		channelManager = new EpicsChannelManager(this);
		data = new Channel[MAXIMUM_NUMBER_OF_MCA];
		acqlistener = new AcqStatusListener();
		rtimelistener = new RealTimeListener();
	}

	@Override
	public void configure() throws FactoryException {
		if (!isConfigured()) {
			if (getRecordName() == null) { // Nothing specified in Server XML file
				logger.error("Missing EPICS configuration for EpicsDlsMcs3820Controller {}", getName());
				throw new FactoryException("Missing EPICS configuration for EpicsDlsMcs3820Controller " + getName());
			}
			createChannelAccess(getRecordName());
			channelManager.tryInitialize(100);
			setConfigured(true);
		}
	}

	/**
	 * Create required channels from a given base PV. Assumes standard suffixes
	 * @param basePv The PV up to (but not including) the final ':'
	 * @throws FactoryException if any channel can't be created
	 */
	private void createChannelAccess(String basePv) throws FactoryException {
		try {
			eraseStartChannel = channelManager.createChannel(basePv + ":EraseStart", false);
			eraseChannel = channelManager.createChannel(basePv + ":EraseAll", false);
			startChannel = channelManager.createChannel(basePv + ":StartAll", false);
			stopChannel = channelManager.createChannel(basePv + ":StopAll", false);
			nbinsChannel = channelManager.createChannel(basePv + ":NuseAll", false);
			readrateChannel = channelManager.createChannel(basePv + ":ReadAll.SCAN", false);
			trealChannel = channelManager.createChannel(basePv + ":ElapsedReal", rtimelistener, false);
			acqChannel = channelManager.createChannel(basePv + ":Acquiring", acqlistener, false);
			tacqChannel = channelManager.createChannel(basePv + ":PresetReal", false);
			tdwellChannel = channelManager.createChannel(basePv + ":Dwell", false);
			binadvChannel = channelManager.createChannel(basePv + ":ChannelAdvance", false);
			extpreChannel = channelManager.createChannel(basePv + ":Prescale", false);

			try {
				data = rangeClosed(1, 32)
						.mapToObj(i -> getChannel(basePv + ":mca" + i))
						.toArray(Channel[]::new);
			} catch (McsException me) {
				throw new FactoryException("Could not create MCA channels", me.getCause());
			}

			for (int i = 0; i < MAXIMUM_NUMBER_OF_MCA; i++) {
				channelIndexMap.put(i, data[i]);
			}
			// acknowledge that creation phase is completed
			channelManager.creationPhaseCompleted();
			setConfigured(true);
		} catch (Exception ex) {
			throw new FactoryException("Failed to create all channels", ex);
		}

	}

	/**
	 * Helper function to wrap {@link CAException}s in RuntimeExceptions so Channels can
	 * be created in a stream
	 */
	private Channel getChannel(String pv) {
		try {
			return channelManager.createChannel(pv, false);
		} catch (CAException e) {
			throw new McsException(e);
		}
	}

	/**
	 * Gets the data Channel ID.
	 *
	 * @param index
	 * @return data channel ID
	 */
	public Channel getDataChannel(int index) {
		return channelIndexMap.get(index);
	}

	/**
	 * Activates the MCA using the Erase & Start acquire.
	 *
	 * @throws DeviceException
	 */
	public void eraseStart() throws DeviceException {
		try {
			controller.caput(eraseStartChannel, 1);
			acquisitionDone = false;

		} catch (Throwable th) {
			throw new DeviceException("Failed to erase and start acquiring", th);
		}
	}

	/**
	 * Erases the data array of the MCA
	 *
	 * @throws DeviceException
	 */
	public void erase() throws DeviceException {
		try {
			controller.caput(eraseChannel, 1);
		} catch (Throwable th) {
			logger.error("Failed to erase all spectrum on {}.", getName());
			throw new DeviceException("Erase: fail to erase all spectrum", th);
		}
	}

	/**
	 * Starts data acquisition
	 *
	 * @throws DeviceException
	 */
	public void start() throws DeviceException {
		try {
			controller.caput(startChannel, 1);
			acquisitionDone = false;

		} catch (Throwable th) {
			throw new DeviceException("Failed to start acquiring", th);
		}
	}

	/**
	 * Stops data acquisition
	 *
	 * @throws DeviceException
	 */
	public void stop() throws DeviceException {
		try {
			controller.caput(stopChannel, 1);
		} catch (Throwable th) {
			throw new DeviceException("Failed to stop acquiring", th);
		}
	}

	/**
	 * Gets the number of bins to use in spectrum.
	 *
	 * @return the number of channels to use
	 * @throws DeviceException
	 */
	public long getNumberOfBins() throws DeviceException {
		try {
			return controller.cagetInt(nbinsChannel);
		} catch (Throwable th) {
			logger.error("Failed to get number of bins {}.", getName());
			throw new DeviceException("Failed get number of bins", th);
		}
	}

	/**
	 * Sets the number of bins (array elements) to use in spectrum.
	 *
	 * @param nbins
	 * @throws DeviceException
	 */
	public void setNumberOfBins(long nbins) throws DeviceException {
		if (nbins > MAXIMUM_NUMBER_BINS) {
			throw new IllegalArgumentException("Invalid number of bins, maximum bins allowed is "
					+ MAXIMUM_NUMBER_BINS);
		}
		try {
			controller.caput(nbinsChannel, nbins);
		} catch (Throwable th) {
			logger.error("Failed to set number of bins {}.", getName());
			throw new DeviceException("Failed to set number of bins", th);
		}
	}

	/**
	 * Sets a new read update rate for DlsMcsSIS3820.
	 *
	 * @param value
	 * @throws DeviceException
	 */
	public void setReadRate(String value) throws DeviceException {
		if (!readUpdateRates.contains(value)) {
			throw new IllegalArgumentException("Input must be in range: " + getReadRates());
		}
		try {
			controller.caput(readrateChannel, value);
		} catch (Throwable th) {
			logger.error("Failed to set read update rate on {}.", getName());
			throw new DeviceException("Failed to set read update rate", th);
		}
	}

	/**
	 * Gets the current read update rate from DlsMcsSIS3820.
	 *
	 * @param value
	 * @throws DeviceException
	 */
	public void getReadRate(@SuppressWarnings("unused") String value) throws DeviceException {
		try {
			controller.caget(readrateChannel);
		} catch (Throwable th) {
			logger.error("Failed to get read update rate on {}.", getName());
			throw new DeviceException("Failed to get read update rate", th);
		}
	}

	/**
	 * Gets the real time since the start of acquisition
	 *
	 * @return elapsed real time
	 * @throws DeviceException
	 */
	public double getRealTime() throws DeviceException {
		try {
			return controller.cagetDouble(trealChannel);
		} catch (Throwable th) {
			logger.error("Failed to get elapsed real time from {}.", getName());
			throw new DeviceException("Failed to get elapsed real time", th);
		}
	}

	/**
	 * Gets acquire status from MCA (poll from hardware)
	 *
	 * @return 0 done, 1 Acquire
	 * @throws DeviceException
	 */
	public int getAcquiringStatus() throws DeviceException {
		try {
			return controller.cagetInt(acqChannel);
		} catch (Throwable th) {
			throw new DeviceException("Failed to get acquiring status", th);
		}
	}

	/**
	 * Gets the total count/acquisition time
	 *
	 * @return elapsed real time
	 * @throws DeviceException
	 */
	public double getTotalTime() throws DeviceException {
		try {
			return controller.cagetDouble(tacqChannel);
		} catch (Throwable th) {
			logger.error("Failed to get total count time from {}.", getName());
			throw new DeviceException("Failed to get total count time", th);
		}
	}

	/**
	 * Sets the total count/acquisition time
	 *
	 * @param value
	 * @throws DeviceException
	 */
	public void setTotalTime(double value) throws DeviceException {
		try {
			controller.caput(tacqChannel, value);
		} catch (Throwable th) {
			logger.error("Failed to set total count time from {}.", getName());
			throw new DeviceException("Failed to set total count time", th);
		}
	}

	/**
	 * Gets the integration time in seconds for incrementing bin number. i.e. the Dwell Time (DWEL) per bin.
	 *
	 * @return Dwell Time
	 * @throws DeviceException
	 */
	public double getDwellTime() throws DeviceException {
		try {
			return controller.cagetDouble(tdwellChannel);
		} catch (Throwable th) {
			logger.error("Failed to get integration time from {}.", getName());
			throw new DeviceException("Failed get integration time", th);
		}
	}

	/**
	 * Sets the integration time in seconds for incrementing bin number, i.e.
	 *
	 * @param time
	 * @throws DeviceException
	 */
	public void setDwellTime(double time) throws DeviceException {
		try {
			controller.caput(tdwellChannel, time);
		} catch (Throwable th) {
			logger.error("Failed to set integration time for {}.", getName());
			throw new DeviceException("Failed to set integration time", th);
		}
	}

	/**
	 * Gets the internal or external bin advance signal
	 *
	 * @return internal or external
	 * @throws DeviceException
	 */
	public String getBinAdv() throws DeviceException {

		try {
			return controller.caget(binadvChannel);
		} catch (Throwable th) {
			logger.error("Failed to get bin advance setting on {}.", getName());
			throw new DeviceException("Failed to get bin advance setting", th);
		}
	}

	/**
	 * Sets the internal or external bin advance signal
	 *
	 * @param value
	 * @throws DeviceException
	 */
	public void setBinAdv(String value) throws DeviceException {
		if (!(value.equalsIgnoreCase("Internal") || value.equalsIgnoreCase("External"))) {
			throw new IllegalArgumentException("Input must be in range: [Internal, External]");
		}
		try {
			controller.caput(binadvChannel, value);
		} catch (Throwable th) {
			logger.error("Failed to set bin advance setting on {}.", getName());
			throw new DeviceException("Failed to set bin advance setting", th);
		}
	}

	/**
	 * Gets the external bin advance pre-scaler, i.e. advance step.
	 *
	 * @return bin advance pre-scaler
	 * @throws DeviceException
	 */
	public double getExternalPreScaler() throws DeviceException {

		try {
			return controller.cagetDouble(extpreChannel);
		} catch (Throwable th) {
			logger.error("Failed to get bin advance pre-scaler on {}.", getName());
			throw new DeviceException("Failed to get bin advance pre-scaler", th);
		}
	}

	/**
	 * Sets the external bin advance per-scaler, i.e. advance step size, default is 1.
	 *
	 * @param value
	 * @throws DeviceException
	 */
	public void setExternalPreScaler(String value) throws DeviceException {
		try {
			controller.caput(extpreChannel, value);
		} catch (Throwable th) {
			logger.error("Failed to set bin advance pre-scaler on {}.", getName());
			throw new DeviceException("Failed to set bin advance pre-scaler", th);
		}
	}

	/**
	 * Gets all spectrum data from all channels
	 *
	 * @return spectrum data
	 * @throws DeviceException
	 */
	public int[][] getData() throws DeviceException {
		int[][] data = new int[MAXIMUM_NUMBER_OF_MCA][MAXIMUM_NUMBER_BINS];
		for (int i = 0; i < MAXIMUM_NUMBER_OF_MCA; i++) {
			data[i] = getData(i);
		}
		return data;
	}

	/**
	 * Gets the spectrum data for the specified channel
	 *
	 * @param channel
	 * @return spectrum data
	 * @throws DeviceException
	 */
	public int[] getData(int channel) throws DeviceException {
		try {
			return controller.cagetIntArray(data[channel]);
		} catch (Throwable th) {
			logger.error("Failed to get the spectrum data on {} for channel {}.", getName(), channel);
			throw new DeviceException("Failed to get the spectrum data", th);
		}
	}

	/**
	 * @param channel
	 * @param ml
	 * @throws DeviceException
	 */
	public void addDataMonitor(int channel, DataMonitor ml) throws DeviceException {
		try {
			controller.setMonitor(data[channel], ml);
			channelIndexMap.put(channel, data[channel]);
		} catch (Throwable th) {
			logger.error("Failed to add data monitor on MCA{} for {}.", channel, getName());
			throw new DeviceException("Failed to add data monitor on MCA" + channel, th);
		}
	}

	/**
	 * Get current dlsMcsSIS3820 status
	 *
	 * @return MCA status
	 */
	public MCAStatus getStatus() {
		return (acquisitionDone) ? MCAStatus.READY : MCAStatus.BUSY;
	}

	@Override
	public void initializationCompleted() {
		try {
			for (String readRate : getReadRates()) {
				if (readRate != null && !readRate.isEmpty()) {
					readUpdateRates.add(readRate);
				}
			}
		} catch (DeviceException e) {
			logger.error("Failed to initialise available Read Update Rates.");
		}
		if (isPollElapsedRealTime()) {
			enablePollRealTime();
		} else {
			disablePollRealTime();
		}

		logger.info("{} is initialised.", getName());
	}

	public void disablePollRealTime() {
		if (trealChannel != null && rtimelistener != null) {
			try {
				realtimemonitor = trealChannel.addMonitor(DBRType.CTRL_DOUBLE, 0, Monitor.VALUE, rtimelistener);
				setPollElapsedRealTime(false);
			} catch (IllegalStateException e) {
				logger.error("Fail to add monitor to channel " + trealChannel.getName(), e);
			} catch (CAException e) {
				logger.error("Fail to add monitor to channel " + trealChannel.getName(), e);
			}
		}
	}

	public void enablePollRealTime() {
		if (realtimemonitor != null && rtimelistener != null) {
			realtimemonitor.removeMonitorListener(rtimelistener);
			setPollElapsedRealTime(true);
		}
	}

	/**
	 * Gets all available read update rates from EPICS IOC
	 *
	 * @return available read update rates
	 * @throws DeviceException
	 */
	public String[] getReadRates() throws DeviceException {
		String[] positionLabels = new String[readUpdateRates.size()];
		try {
			positionLabels = controller.cagetLabels(readrateChannel);
		} catch (Throwable th) {
			logger.error("Failed to get read update rates avalable on {}.", getName());
			throw new DeviceException("Failed to get read update rates avalable", th);
		}
		return positionLabels;
	}

	/**
	 * Monitoring current acquire status of the hardware
	 */
	private class AcqStatusListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent mev) {

			DBR dbr = mev.getDBR();
			if (dbr.isENUM()) {
				acquisitionDone = ((DBR_Enum) dbr).getEnumValue()[0] == 0;
			} else {
				logger.error("Expecting ENUM but got {} type.", dbr.getType());
			}
			notifyIObservers(AcquisitionProperty.STATUS, getStatus());
			logger.debug("{}: acquisition status updated to {}", getName(), getStatus().value());
		}
	}

	/**
	 * Monitors elapses real time
	 */
	private class RealTimeListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent mev) {

			DBR dbr = mev.getDBR();
			if (dbr.isDOUBLE()) {
				elapsedRealTimeValue = ((DBR_Double) dbr).getDoubleValue()[0];
			} else if (dbr.isFLOAT()) {
				elapsedRealTimeValue = ((DBR_Float) dbr).getFloatValue()[0];
			} else {
				logger.error("Expecting double or float but got {} type. ", dbr.getType());
			}
			notifyIObservers(AcquisitionProperty.ELAPSEDTIME, elapsedRealTimeValue);
			logger.trace("{}: Elapsed time updated to {}", getName(), elapsedRealTimeValue);
		}
	}

	/**
	 * Monitor value array - ????could this be removed as no observer is add to handle this event after sending to EpicsDlsMcsSis3820?????
	 */
	public class DataMonitor implements MonitorListener {

		@Override
		public void monitorChanged(MonitorEvent mev) {
			Channel source = (Channel) mev.getSource();
			DBR dbr = mev.getDBR();
			if (dbr != null && dbr.isINT()) {
				int[] data = ((DBR_Int) dbr).getIntValue();
				notifyIObservers(source, data);
			}
		}
	}

	/**
	 * Get Epics Record name
	 *
	 * @return record name
	 */
	public String getRecordName() {
		return recordName;
	}

	/**
	 * Sets EPICS record name
	 *
	 * @param recordName
	 */
	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}

	/**
	 * Gets number of MCA records in this template
	 *
	 * @return number of MCA record supported
	 */
	public int getNumberOfMca() {
		return numberOfMca;
	}

	/**
	 * Sets number of MCA record in this template.
	 *
	 * @param numberOfMca
	 */
	public void setNumberOfMca(int numberOfMca) {
		this.numberOfMca = numberOfMca;
	}

	public boolean isPollElapsedRealTime() {
		return pollElapsedRealTime;
	}

	public void setPollElapsedRealTime(boolean pollElapsedRealTime) {
		this.pollElapsedRealTime = pollElapsedRealTime;
	}

	/** Wrapper to allow CAExceptions to be thrown from a stream */
	private class McsException extends RuntimeException {
		public McsException(Throwable t) {
			super(t);
		}
	}

}
