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

package uk.ac.gda.epics.client.views.model.impl;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.detector.areadetector.v17.FfmpegStream;
import gda.device.detector.areadetector.v17.NDPluginBase;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import uk.ac.gda.epics.client.views.controllers.IMJpegViewController;
import uk.ac.gda.epics.client.views.model.FfMpegModel;

/**
 * Client side model for FFMjpeg streamer corresponding to the IOC.
 */
public class FFMjpegModelImpl extends EPICSBaseModel implements FfMpegModel {
	static final Logger logger = LoggerFactory.getLogger(FFMjpegModelImpl.class);
	private Dim1SizeMonitorListener dim1SizeMonitorListener;
	private Dim0SizeMonitorListener dim0SizeMonitorListener;
	private TimeStampMonitorListener timestampMonitorListener;
	private NDArrayPortMonitorListener ndArrayPortMonitorListener;

	public FFMjpegModelImpl() {
		dim0SizeMonitorListener = new Dim0SizeMonitorListener();
		dim1SizeMonitorListener = new Dim1SizeMonitorListener();
		timestampMonitorListener = new TimeStampMonitorListener();
		ndArrayPortMonitorListener = new NDArrayPortMonitorListener();
	}

	private Set<IMJpegViewController> mJpegViewControllers = new HashSet<IMJpegViewController>();

	@Override
	public boolean registerMJpegViewController(IMJpegViewController viewController) {
		return mJpegViewControllers.add(viewController);
	}

	@Override
	public boolean removeMJpegViewController(IMJpegViewController viewController) {
		return mJpegViewControllers.remove(viewController);
	}

	private class NDArrayPortMonitorListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			DBR dbr = arg0.getDBR();
			if (dbr != null && dbr.isSTRING()) {
				for (IMJpegViewController controller : mJpegViewControllers) {
					controller.updateMJpegNDArrayPort(((DBR_String) dbr).getStringValue()[0]);
				}
			}
		}
	}

	@Override
	public String getNdArrayPort() throws Exception {
		try {
			return EPICS_CONTROLLER.caget(getChannel(NDPluginBase.NDArrayPort, ndArrayPortMonitorListener));
		} catch (Exception ex) {
			throw ex;
		}
	}

	@Override
	public void setNdArrayPort(String ndArrayPort) throws Exception {
		try {
			EPICS_CONTROLLER.caput(getChannel(NDPluginBase.NDArrayPort, ndArrayPortMonitorListener), ndArrayPort);
		} catch (Exception ex) {
			throw ex;
		}
	}

	private class Dim0SizeMonitorListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			DBR dbr = arg0.getDBR();
			if (dbr != null && dbr.isINT()) {
				for (IMJpegViewController controller : mJpegViewControllers) {
					controller.updateMJpegX(((DBR_Int) dbr).getIntValue()[0]);
				}
			}
		}
	}

	@Override
	public int getDim0Size() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(NDPluginBase.ArraySize0_RBV, dim0SizeMonitorListener));
		} catch (Exception ex) {
			throw ex;
		}
	}

	private class Dim1SizeMonitorListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			DBR dbr = arg0.getDBR();
			if (dbr != null && dbr.isINT()) {
				for (IMJpegViewController controller : mJpegViewControllers) {
					controller.updateMJpegY(((DBR_Int) dbr).getIntValue()[0]);
				}
			}
		}
	}

	@Override
	public int getDim1Size() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetInt(getChannel(NDPluginBase.ArraySize1_RBV, dim1SizeMonitorListener));
		} catch (Exception ex) {
			throw ex;
		}
	}

	private class TimeStampMonitorListener implements MonitorListener {
		@Override
		public void monitorChanged(MonitorEvent arg0) {
			DBR dbr = arg0.getDBR();
			if (dbr != null && dbr.isDOUBLE()) {
				for (IMJpegViewController controller : mJpegViewControllers) {
					controller.updateMJpegTimeStamp(((DBR_Double) dbr).getDoubleValue()[0]);
				}
			}
		}
	}

	@Override
	public double getTimeStamp() throws Exception {
		try {
			return EPICS_CONTROLLER.cagetDouble(getChannel(NDPluginBase.TimeStamp_RBV, timestampMonitorListener));
		} catch (Exception ex) {
			throw ex;
		}
	}

	/**
	 * This PV does not need to be monitored, so passing null as argument value for the monitor
	 *
	 * @return URL for video streaming
	 */
	@Override
	public String getMjpegUrl() throws Exception {
		try {
			return new String(EPICS_CONTROLLER.cagetByteArray(getChannel(FfmpegStream.MJPG_URL_RBV, null))).trim();
		} catch (Exception ex) {
			logger.warn("Cannot getMJPG_URL_RBV", ex);
			throw ex;
		}
	}

	/**
	 * This PV does not need to be monitored, so passing null as argument value for the monitor
	 *
	 * @return URL for video streaming
	 */
	@Override
	public String getJpegUrl() throws Exception {
		try {
			return new String(EPICS_CONTROLLER.cagetByteArray(getChannel(FfmpegStream.JPG_URL_RBV, null))).trim();
		} catch (Exception ex) {
			logger.warn("Cannot getJPG_URL_RBV", ex);
			throw ex;
		}
	}

	@Override
	protected void doCheckAfterPropertiesSet() throws Exception {
		// nothing to do
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
