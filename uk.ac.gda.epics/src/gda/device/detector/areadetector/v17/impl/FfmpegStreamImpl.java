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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import gda.device.detector.areadetector.v17.FfmpegStream;
import gda.epics.connection.EpicsController;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.TimeoutException;

public class FfmpegStreamImpl extends NDBaseImpl implements InitializingBean, FfmpegStream {
	// Setup the logging facilities
	static final Logger logger = LoggerFactory.getLogger(FfmpegStreamImpl.class);

	private final static EpicsController EPICS_CONTROLLER = EpicsController.getInstance();

	private String basePVName;

	/**
	 * Map that stores the channel against the PV name
	 */
	private Map<String, Channel> channelMap = new HashMap<String, Channel>();

	private double initialQuality = Double.NaN;

	private int initialFalseColor;

	private int initialAlwaysOn;

	/**
	*
	*/
	@Override
	public double getQUALITY() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(QUALITY));
		} catch (Exception ex) {
			logger.warn("Cannot getQUALITY", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setQUALITY(double quality) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(QUALITY), quality);
		} catch (Exception ex) {
			logger.warn("Cannot setQUALITY", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public double getQUALITY_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(QUALITY_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getQUALITY_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getFALSE_COL() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(FALSE_COL));
		} catch (Exception ex) {
			logger.warn("Cannot getFALSE_COL", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setFALSE_COL(int false_col) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(FALSE_COL), false_col);
		} catch (Exception ex) {
			logger.warn("Cannot setFALSE_COL", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getFALSE_COL_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(FALSE_COL_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getFALSE_COL_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getALWAYS_ON() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(ALWAYS_ON));
		} catch (Exception ex) {
			logger.warn("Cannot getALWAYS_ON", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public void setALWAYS_ON(int always_on) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(ALWAYS_ON), always_on);
		} catch (Exception ex) {
			logger.warn("Cannot setALWAYS_ON", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public short getALWAYS_ON_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetEnum(getChannel(ALWAYS_ON_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getALWAYS_ON_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public double getHTTP_PORT_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(HTTP_PORT_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getHTTP_PORT_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getHOST_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(HOST_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getHOST_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public int getCLIENTS_RBV() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(CLIENTS_RBV));
		} catch (Exception ex) {
			logger.warn("Cannot getCLIENTS_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getJPG_URL_RBV() throws Exception {
		try {
			return new String(EPICS_CONTROLLER.cagetByteArray(getChannel(JPG_URL_RBV))).trim();
		} catch (Exception ex) {
			logger.warn("Cannot getJPG_URL_RBV", ex);
			throw ex;
		}
	}

	/**
	*
	*/
	@Override
	public String getMJPG_URL_RBV() throws Exception {

		try {
			return new String(EPICS_CONTROLLER.cagetByteArray(getChannel(MJPG_URL_RBV))).trim();
		} catch (Exception ex) {
			logger.warn("Cannot getMJPG_URL_RBV", ex);
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
		if (getPluginBase() == null) {
			throw new IllegalArgumentException("'ndPluginBase' should be declared.");
		}
	}

	/**
	 * @param basePVName
	 *            The basePVName to set.
	 */
	public void setBasePVName(String basePVName) {
		this.basePVName = basePVName;
	}

	public void setInitialQuality(double initialQuality ){
		this.initialQuality=initialQuality;
	}
	public double getInitialQuality(){
		return initialQuality;
	}
	public void setInitialFalseColor(int initialFalseColor ){
		this.initialFalseColor=initialFalseColor;
	}
	public int getInitialFalseColor(){
		return initialFalseColor;
	}
	public void setInitialAlwaysOn(int initialAlwaysOn ){
		this.initialAlwaysOn=initialAlwaysOn;
	}
	public int getInitialAlwaysOn(){
		return initialAlwaysOn;
	}
	/**
	 * This method allows to toggle between the method in which the PV is acquired.
	 *
	 * @param pvElementName
	 * @param args
	 * @return {@link Channel} to talk to the relevant PV.
	 * @throws Exception
	 */
	private String getFullPV(String pvElementName, String... args) {
		String pvPostFix = null;
		if (args.length > 0) {
			// PV element name is different from the pvPostFix
			pvPostFix = args[0];
		} else {
			pvPostFix = pvElementName;
		}

		return basePVName + pvPostFix;
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
		getPluginBase().reset();
		if (!Double.isNaN(initialQuality)) {
			setQUALITY(initialQuality);
		}
		if (initialAlwaysOn == 1) {
			setALWAYS_ON((short)1);
		} else {
			setALWAYS_ON((short) 0);
		}
		if (initialFalseColor == 1){
			setFALSE_COL((short)1);
		} else {
			setFALSE_COL((short)0);
		}
	}



	@Override
	public void setMAXW(int maxw) throws Exception {
		String pv = getFullPV(MAXW);
		Channel ch = createChannel(pv);
		EPICS_CONTROLLER.caput(ch, maxw);
	}

	@Override
	public void setMAXH(int maxh) throws Exception {
		String pv = getFullPV(MAXH);
		Channel ch = createChannel(pv);
		EPICS_CONTROLLER.caput(ch, maxh);
	}

	@Override
	public int getMAXW_RBV() throws Exception {
		String pv = getFullPV(MAXW_RBV);
		Channel ch = createChannel(pv);
		return EPICS_CONTROLLER.cagetInt(ch);
	}

	@Override
	public int getMAXH_RBV() throws Exception {
		String pv = getFullPV(MAXH_RBV);
		Channel ch = createChannel(pv);
		return EPICS_CONTROLLER.cagetInt(ch);
	}

}
