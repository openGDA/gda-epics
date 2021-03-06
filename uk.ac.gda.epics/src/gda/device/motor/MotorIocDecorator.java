/*-
 * Copyright © 2014 Diamond Light Source Ltd.
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

package gda.device.motor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import gda.device.Motor;
import gda.epics.connection.EpicsChannelManager;
import gda.epics.connection.EpicsController;
import gda.factory.FactoryException;
import gda.jython.InterfaceProvider;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBR_Enum;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import uk.ac.diamond.daq.concurrent.Async;

/**
 * Abstract decorator class for {@link EpicsMotor} that initialises and monitors IOC status PV.
 * It also supports connections of motor object to EPICS PVs after GDA had started if IOC status changed to RUNNING.
 */
public abstract class MotorIocDecorator extends MotorBase implements InitializingBean {
	private Logger logger = LoggerFactory.getLogger(MotorIocDecorator.class);
	protected Motor decoratedMotor;
	protected boolean iocRunning=false;
	protected String iocPv=null;
	private EpicsController controller;
	private EpicsChannelManager channelManager;
	private IOCStatusMonitorListener isml;

	public String getIocPv() {
		return iocPv;
	}

	public void setIocPv(String iocPv) {
		this.iocPv = iocPv;
	}

	public MotorIocDecorator() {
		controller=EpicsController.getInstance();
		channelManager=new EpicsChannelManager();
		isml=new IOCStatusMonitorListener();
	}

	public MotorIocDecorator(Motor decoratedMotor) {
		this();
		this.setDecoratedMotor(decoratedMotor);
	}

	public boolean isIocRunning() {
		return iocRunning;
	}

	public void setIocRunning(boolean iocRunning) {
		this.iocRunning = iocRunning;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (decoratedMotor == null) {
			throw new IllegalStateException("Motor to be detecorated must not be null");
		}

		if (iocPv == null) {
			throw new IllegalStateException("IOC status PV must not be null");
		}
		// initialise IOC status
		Channel iocChannel = channelManager.createChannel(iocPv, isml);
		int value = controller.cagetEnum(iocChannel);
		if (value == 0) {
			setIocRunning(true);
		} else {
			setIocRunning(false);
		}
	}

	protected class IOCStatusMonitorListener implements MonitorListener {

		@Override
		public void monitorChanged(MonitorEvent ev) {
			DBR dbr=ev.getDBR();
			short value=-1;
			if (dbr.isENUM()) {
				value=((DBR_Enum)dbr).getEnumValue()[0];
			}
			if (value==0) {
				setIocRunning(true);
				if (decoratedMotor instanceof EpicsMotor) {
					Async.execute(() -> {
						EpicsMotor decoratedMotor2 = (EpicsMotor)decoratedMotor;
						if (!decoratedMotor2.isConfigured()) {
							try {
								decoratedMotor.reconfigure();
							} catch (FactoryException e) {
								logger.error("reconfigure motor {} failed.", decoratedMotor.getName(), e);
							}
						}
					});
				}
				reportMonitorChanged("IOC " + getIocPv().split(":")[0] + " is running.");
			} else {
				setIocRunning(false);
				if (value==1) {
					reportMonitorChanged("IOC " + getIocPv().split(":")[0] + " is shutdown.");
				} else if (value==2) {
					reportMonitorChanged("procServ for IOC " + getIocPv().split(":")[0] + " is stopped.");
				}
			}
		}

		private void reportMonitorChanged(String out) {
			// This may be called before the logging system or Jython terminal is available, so log to stdout if unavailable.
			if (InterfaceProvider.getTerminalPrinter() != null) {
				InterfaceProvider.getTerminalPrinter().print(out);
			} else {
				System.out.println(out);
			}
			logger.info(out);
		}
	}

	public Motor getDecoratedMotor() {
		return decoratedMotor;
	}

	public void setDecoratedMotor(Motor decoratedMotor) {
		this.decoratedMotor = decoratedMotor;
	}

}
