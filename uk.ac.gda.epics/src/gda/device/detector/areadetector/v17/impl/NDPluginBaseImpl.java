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

package gda.device.detector.areadetector.v17.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import gda.device.DeviceException;
import gda.device.detector.areadetector.v17.NDPluginBase;
import gda.epics.LazyPVFactory;
import gda.epics.connection.EpicsController;
import gda.observable.Observable;
import gda.observable.ObservableUtil;
import gda.observable.Observer;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

public class NDPluginBaseImpl implements InitializingBean, NDPluginBase {

	private final static EpicsController EPICS_CONTROLLER = EpicsController.getInstance();

	private String initialArrayPort;

	private Integer initialArrayAddress;

	private Boolean initialBlockingCallbacks;

	private Integer initialEnableCallbacks;

	private String basePVName;

	// Setup the logging facilities
	static final Logger logger = LoggerFactory.getLogger(NDPluginBaseImpl.class);

	/**
	 * Map that stores the channel against the PV name
	 */
	private Map<String, Channel> channelMap = new HashMap<String, Channel>();

	/**
	*
	*/
	@Override
	public String getPortName_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(PortName_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getPortName_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getPluginType_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(PluginType_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getPluginType_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getNDArrayPort() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(NDArrayPort));
		} catch (Exception ex) {
			logger.warn("Cannot getNDArrayPort", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setNDArrayPort(String ndarrayport) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(NDArrayPort), ndarrayport);
		} catch (Exception ex) {
			logger.warn("Cannot setNDArrayPort", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getNDArrayPort_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(NDArrayPort_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getNDArrayPort_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getNDArrayAddress() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(NDArrayAddress));
		} catch (Exception ex) {
			logger.warn("Cannot getNDArrayAddress", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setNDArrayAddress(int ndarrayaddress) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(NDArrayAddress), ndarrayaddress);
		} catch (Exception ex) {
			logger.warn("Cannot setNDArrayAddress", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getNDArrayAddress_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(NDArrayAddress_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getNDArrayAddress_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public boolean isCallbackEnabled() throws Exception {
		return EPICS_CONTROLLER.cagetInt(getChannel(EnableCallbacks)) == 1;
	}

	/**
	*
	*/
	@Override
	public void enableCallbacks() throws Exception {
		EPICS_CONTROLLER.caputWait(getChannel(EnableCallbacks), 1);
	}

	@Override
	public void disableCallbacks() throws Exception {
		EPICS_CONTROLLER.caputWait(getChannel(EnableCallbacks), 0);
	}

	/**
	*
	*/
	@Override
	public boolean isCallbacksEnabled_RBV() throws Exception {
		return EPICS_CONTROLLER.cagetInt(getChannel(EnableCallbacks_RBV)) == 1;
	}

	/**
	*
	*/
	@Override
	public double getMinCallbackTime() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(MinCallbackTime));
		} catch (Exception ex) {
			logger.warn("Cannot getMinCallbackTime", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setMinCallbackTime(double mincallbacktime) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(MinCallbackTime), mincallbacktime);
		} catch (Exception ex) {
			logger.warn("Cannot setMinCallbackTime", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public double getMinCallbackTime_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(MinCallbackTime_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getMinCallbackTime_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getBlockingCallbacks() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(BlockingCallbacks));
		} catch (Exception ex) {
			logger.warn("Cannot getBlockingCallbacks", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setBlockingCallbacks(int blockingcallbacks) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(BlockingCallbacks), blockingcallbacks);
		} catch (Exception ex) {
			logger.warn("Cannot setBlockingCallbacks", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getBlockingCallbacks_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(BlockingCallbacks_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getBlockingCallbacks_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getArrayCounter() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(ArrayCounter));
		} catch (Exception ex) {
			logger.warn("Cannot getArrayCounter", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setArrayCounter(int arraycounter) throws Exception {
		try {
			EPICS_CONTROLLER.caputWait(getChannel(ArrayCounter), arraycounter);
		} catch (Exception ex) {
			logger.warn("Cannot setArrayCounter", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getArrayCounter_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(ArrayCounter_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getArrayCounter_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public double getArrayRate_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(ArrayRate_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getArrayRate_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getDroppedArrays() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(DroppedArrays));
		} catch (Exception ex) {
			logger.warn("Cannot getDroppedArrays", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setDroppedArrays(int droppedarrays) throws Exception {
		EPICS_CONTROLLER.caputWait(getChannel(DroppedArrays), droppedarrays);

	}

	/**
	*
	*/
	@Override
	public int getDroppedArrays_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(DroppedArrays_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getDroppedArrays_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getNDimensions_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(NDimensions_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getNDimensions_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getArraySize0_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(ArraySize0_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getArraySize0_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getArraySize1_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(ArraySize1_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getArraySize1_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getArraySize2_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(ArraySize2_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getArraySize2_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getDataType_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(DataType_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getDataType_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getColorMode_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(ColorMode_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getColorMode_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getBayerPattern_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(BayerPattern_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getBayerPattern_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getUniqueId_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(UniqueId_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getUniqueId_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public double getTimeStamp_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(TimeStamp_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getTimeStamp_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getNDAttributesFile() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(NDAttributesFile));
		} catch (Exception ex) {
			logger.warn("Cannot getNDAttributesFile", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setNDAttributesFile(String ndattributesfile) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(NDAttributesFile), (ndattributesfile + '\0').getBytes());
		} catch (Exception ex) {
			logger.warn("Cannot setNDAttributesFile", ex);
			throw ex;
		}
	}

	/**
	 * @return Returns the basePVName.
	 */
	public String getBasePVName() {
		return basePVName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (basePVName == null) {
			throw new IllegalArgumentException("'basePVName' needs to be declared");
		}
	}

	/**
	 * @param basePVName
	 *            The basePVName to set.
	 */
	public void setBasePVName(String basePVName) {
		this.basePVName = basePVName;
	}

	/**
	 * This method allows to toggle between the method in which the PV is acquired.
	 *
	 * @param pvElementName
	 * @param args
	 * @return {@link Channel} to talk to the relevant PV.
	 * @throws Exception
	 */
	private Channel getChannel(String pvElementName, String... args) throws Exception {
		try {
			String pvPostFix = null;
			if (args.length > 0) {
				// PV element name is different from the pvPostFix
				pvPostFix = args[0];
			} else {
				pvPostFix = pvElementName;
			}

			return createChannel(basePVName + pvPostFix);
		} catch (Exception exception) {
			logger.warn("Problem getting channel", exception);
			throw exception;
		}
	}

	public Channel createChannel(String fullPvName) throws CAException, TimeoutException {
		Channel channel = channelMap.get(fullPvName);
		if (channel == null) {
			try {
				channel = EPICS_CONTROLLER.createChannel(fullPvName);
			} catch (CAException cae) {
				logger.warn("Problem creating channel", cae);
				throw cae;
			} catch (TimeoutException te) {
				logger.warn("Problem creating channel", te);
				throw te;

			}
			channelMap.put(fullPvName, channel);
		}
		return channel;
	}

	@Override
	public void reset() throws Exception {
		if (initialArrayAddress != null) {
			setNDArrayAddress(initialArrayAddress);
		}
		if (initialArrayPort != null) {
			setNDArrayPort(initialArrayPort);
		}

		if (initialBlockingCallbacks != null){
			setBlockingCallbacks((short) (initialBlockingCallbacks ?  1 : 0));
		}

		if (initialEnableCallbacks != null) {
			if (initialEnableCallbacks > 0) {
				enableCallbacks();
			} else {
				disableCallbacks();
			}
		}
	}

	/**
	 * @param initialArrayPort
	 *            The initialArrayPort to set.
	 */
	public void setInitialArrayPort(String initialArrayPort) {
		this.initialArrayPort = initialArrayPort;
	}

	/**
	 * @return Returns the initialArrayPort.
	 */
	@Override
	public String getInitialArrayPort() {
		return initialArrayPort;
	}

	/**
	 * @param initialArrayAddress
	 *            The initialArrayAddress to set.
	 */
	public void setInitialArrayAddress(Integer initialArrayAddress) {
		this.initialArrayAddress = initialArrayAddress;
	}

	/**
	 * @return Returns the initialArrayAddress.
	 */
	@Override
	public Integer getInitialArrayAddress() {
		return initialArrayAddress;
	}

	/**
	 * @param initialBlockingCallbacks
	 *            The initialBlockingCallbacks to set.
	 */
	public void setInitialBlockingCallbacks(Boolean initialBlockingCallbacks) {
		this.initialBlockingCallbacks = initialBlockingCallbacks;
	}

	/**
	 * @return Returns the initialBlockingCallbacks.
	 */
	public Boolean getInitialBlockingCallbacks() {
		return initialBlockingCallbacks;
	}

	/**
	 * @param initialEnableCallbacks
	 *            The initialEnableCallbacks to set.
	 */
	public void setInitialEnableCallbacks(int initialEnableCallbacks) {
		this.initialEnableCallbacks = initialEnableCallbacks;
	}

	/**
	 * @return Returns the initialEnableCallbacks.
	 */
	public int getInitialEnableCallbacks() {
		return initialEnableCallbacks;
	}

	private String getChannelName(String pvElementName, String... args) {
		String pvPostFix = null;
		if (args.length > 0) {
			// PV element name is different from the pvPostFix
			pvPostFix = args[0];
		} else {
			pvPostFix = pvElementName;
		}

		return basePVName + pvPostFix;
	}

	@Override
	public Observable<Integer> createArrayCounterObservable() throws Exception {
		return LazyPVFactory.newReadOnlyIntegerPV(getChannelName(ArrayCounter_RBV));
	}

	@Override
	public Observable<Boolean> createConnectionStateObservable() throws Exception {
		return new ConnectionStateObservable( getChannelName(ArrayCounter_RBV));
	}

	@Override
	public Observable<String> createEnableObservable() throws Exception {
		return LazyPVFactory.newReadOnlyEnumPV(getChannelName(EnableCallbacks_RBV), String.class);
	}

	@Override
	public Observable<Double> createMinCallbackTimeObservable() throws Exception {
		return LazyPVFactory.newReadOnlyDoublePV(getChannelName(MinCallbackTime_RBV));
	}

	@Override
	public Observable<Integer> createDroppedFramesObservable() throws Exception {
		return LazyPVFactory.newReadOnlyIntegerPV(getChannelName(DroppedArrays_RBV));
	}

	private Observable<Integer> droppedFramesObservable;
	private Observer<Integer>  droppedFramesObserver;

	protected Integer droppedFrames=0;

	private int getDroppedFrames() throws Exception {
		if( droppedFramesObservable == null){
			droppedFramesObservable = createDroppedFramesObservable();
		}
		if( droppedFramesObserver == null){
			droppedFramesObserver = new Observer<Integer>() {

				@Override
				public void update(Observable<Integer> source, Integer arg) {
					droppedFrames = arg;
				}
			};
			droppedFramesObservable.addObserver(droppedFramesObserver);
			droppedFrames = getDroppedArrays_RBV();
		}
		return droppedFrames;
	}

	@Override
	public void checkDroppedFrames() throws Exception {
		int droppedFrames2 = getDroppedFrames();
		if (droppedFrames2 >0) {
			throw new DeviceException("NDPlugin dropped frames : " + droppedFrames2);
		}
	}
}

class ConnectionStateObservable implements Observable<Boolean>{

	Channel ch;

	ObservableUtil<Boolean> delegate= new ObservableUtil<Boolean>();

	ConnectionStateObservable(String pv) throws Exception{
		EpicsController.getInstance().createChannel(pv, new ConnectionListener() {

			@Override
			public void connectionChanged(ConnectionEvent arg0) {
				delegate.notifyIObservers(ConnectionStateObservable.this, arg0.isConnected());
			}
		});
	}

	@Override
	public void addObserver(Observer<Boolean> observer) {
		delegate.addObserver(observer);
	}

	@Override
	public void addObserver(Observer<Boolean> observer, Predicate<Boolean> predicate) {
		delegate.addObserver(observer, predicate);
	}

	@Override
	public void removeObserver(Observer<Boolean> observer) {
		delegate.removeObserver(observer);
	}

	@Override
	protected void finalize() throws Throwable {
		if( ch != null){
			EpicsController.getInstance().destroy(ch);
			ch = null;
		}
		super.finalize();
	}



}
