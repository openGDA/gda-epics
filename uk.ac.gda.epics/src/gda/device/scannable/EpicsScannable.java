/*-
 * Copyright © 2010 Diamond Light Source Ltd.
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

package gda.device.scannable;

import java.io.Serializable;

import javax.measure.Quantity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import gda.device.DeviceException;
import gda.device.ScannableMotionUnits;
import gda.device.epicsdevice.EpicsDBR;
import gda.epics.PVProvider;
import gda.epics.connection.EpicsController;
import gda.epics.connection.EpicsController.MonitorType;
import gda.factory.FactoryException;
import gda.observable.IObserver;
import gda.util.QuantityFactory;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_CTRL_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Double;
import gov.aps.jca.dbr.DBR_CTRL_Enum;
import gov.aps.jca.dbr.DBR_CTRL_Float;
import gov.aps.jca.dbr.DBR_CTRL_Int;
import gov.aps.jca.dbr.DBR_CTRL_Short;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;
import uk.ac.gda.api.remoting.ServiceInterface;

/**
 * EpicsScannable is a ScannableMotionUnits implementation giving access to a PV.
 * <p>
 * The class is for local use only as sending monitor events from EPICS over Corba can be very inefficient.
 * <p>
 * To use with an ENUM it may be useful to set getAsString=true, and set hasUnits=false.
 * <p>
 * The connection to the channel is only first made when access to the PV is required.
 * <p>
 * The monitor is only set on the channel if the class is being observed.
 */
@ServiceInterface(ScannableMotionUnits.class)
public class EpicsScannable extends ScannableMotionUnitsBase implements InitializingBean{
	private static final Logger logger = LoggerFactory.getLogger(EpicsScannable.class);

	private Channel channel;
	private Boolean busy = false;
	private PutListener putListener;
	private MonitorListener monitorListener;
	private Monitor monitor;
	private String units;
	private int elementCount=1;

	private boolean useNameAsInputName=false;
	private boolean useNameAsExtraName=false;

	private boolean hasUnits = true; // set false to not convert value to units - useful for enum with getAsString=true
	private PVProvider pvProvider;
	private String pvName = "";
	private boolean getAsString = false;

	public EpicsScannable() {
		super();
		setInputNames(new String[]{}); //clear the entry put in by ScannableBase
		putListener = new PutListener() {

			@Override
			public void putCompleted(PutEvent arg0) {
				busy = false;

			}
		};
		monitorListener = new MonitorListener() {

			@Override
			public void monitorChanged(MonitorEvent arg0) {
				DBR dbr = arg0.getDBR();
				Object arg = getAsString ? convertDBRToString(dbr) : valFromDBR(dbr);
				notifyIObservers(this, arg);//TODO remove this.
				notifyIObservers(this, new ScannablePositionChangeEvent((Serializable) arg));
			}

			Object valFromDBR(DBR dbr){
				Object value = dbr.getValue();
				if (elementCount > 1)
					return value;
				if (value instanceof double[]) {
					return ((double[])value)[0];
				}
				if (value instanceof float[]) {
					return ((float[])value)[0];
				}
				if (value instanceof short[]) {
					return ((short[])value)[0];
				}
				if (value instanceof int[]) {
					return ((int[])value)[0];
				}
				if (value instanceof byte[]) {
					return ((byte[])value)[0];
				}
				if (value instanceof String[]) {
					return ((String[])value)[0];
				}
				return value.toString();
			}
		};
	}

	public void setUseNameAsInputName(boolean useNameAsInputName) {
		this.useNameAsInputName = useNameAsInputName;
	}

	public void setUseNameAsExtraName(boolean useNameAsExtraName) {
		this.useNameAsExtraName = useNameAsExtraName;
	}

	@Override
	public void configure() throws FactoryException {
		if(isConfigured())
			return;
		super.configure();
		if(useNameAsInputName){
			if( inputNames.length > 0){
				logger.warn("useNameAsInputName is true although inputNames are already set for " + getName());
			}
			setInputNames(new String[]{getName()});
		}
		if(useNameAsExtraName){
			if( extraNames.length > 0){
				logger.warn("useNameAsExtraName is true although extraNames are already set for " + getName());
			}
			setExtraNames(new String[]{getName()});
		}

		if(hasUnits && !unitsComponent.unitHasBeenSet()){
			try {
				unitsComponent.setHardwareUnitString(getUnits());
			} catch (DeviceException e) {
				throw new FactoryException("Error setting hardware units",e);
			} finally {
				destroy();
			}
		}
		setConfigured(true);
	}

	public void setElementCount(int elementCount) {
		this.elementCount = elementCount;
	}

	public void destroy(){
		if( !isBeingObserved() && channel != null){
			Channel tmp = channel;
			channel = null;
			EpicsController.getInstance().destroy(tmp);
		}
	}
	private Channel getChannel() throws CAException, TimeoutException {
		if (channel == null) {
			channel = EpicsController.getInstance().createChannel(getPV());
		}
		return channel;
	}

	@Override
	public void rawAsynchronousMoveTo(Object value) throws DeviceException {
		try {
			busy = true;
			final EpicsController controller = EpicsController.getInstance();
			if (value instanceof Double) {
				controller.caput(getChannel(), (Double) value, putListener);
			} else if (value instanceof Integer) {
				controller.caput(getChannel(), (Integer) value, putListener);
			} else if (value instanceof Float) {
				controller.caput(getChannel(), (Float) value, putListener);
			} else if (value instanceof Short) {
				controller.caput(getChannel(), (Short) value, putListener);
			} else if (value instanceof double[]) {
				controller.caput(getChannel(), (double[]) value, putListener);
			} else if (value instanceof int[]) {
				controller.caput(getChannel(), (int[]) value, putListener);
			} else if (value instanceof float[]) {
				controller.caput(getChannel(), (float[]) value, putListener);
			} else if (value instanceof short[]) {
				controller.caput(getChannel(), (short[]) value, putListener);
			} else {
				controller.caput(getChannel(), value.toString(), putListener);
			}
		} catch (Exception e) {
			busy = false;
			throw new DeviceException("Error in caput to " + getPV() + " for " + getName() + ". val:" + value.toString(),e);
		}
	}

	@Override
	public Object rawGetPosition() throws DeviceException {
		try {
			return getAsString ? getValueAsString() : getNativeValue();
		} catch (Exception e) {
			throw new DeviceException("Error getting position for pv " + getPV() + " for " + getName(), e);
		}
	}


	@Override
	public Object internalToExternal(Object internalPosition) {
		if( !hasUnits)
			return internalPosition;
		return super.internalToExternal(internalPosition);
	}



	@Override
	public Object externalToInternal(Object externalPosition) {
		if( !hasUnits)
			return externalPosition;
		return super.externalToInternal(externalPosition);
	}

	private Object getNativeValue() throws DeviceException {
		try {
			Object value = null;
			final Channel ch = getChannel();
			final DBRType fieldType = ch.getFieldType();
			final EpicsController controller = EpicsController.getInstance();

			if (fieldType.isBYTE()) {
				byte[] val = controller.cagetByteArray(ch, elementCount);
				if (elementCount == 1) {
					value = val[0];
				}
			} else if (fieldType.isINT()) {
				int[] val = controller.cagetIntArray(ch, elementCount);
				if (elementCount == 1) {
					value = val[0];
				}
			} else if (fieldType.isFLOAT()) {
				float[] val = controller.cagetFloatArray(ch, elementCount);
				if (elementCount == 1) {
					value = val[0];
				}
			} else if (fieldType.isDOUBLE()) {
				double[] val = controller.cagetDoubleArray(ch, elementCount);
				if (elementCount == 1) {
					value = val[0];
				}
			} else if (fieldType.isSHORT() || fieldType.isENUM() ) {
				short[] val = controller.cagetShortArray(ch, elementCount);
				if (elementCount == 1) {
					value = val[0];
				}
			} else {
				String[] val = controller.cagetStringArray(ch, elementCount);
				if (elementCount == 1) {
					value = val[0];
				}
			}
			return value;
		} catch (Exception e) {
			throw new DeviceException("Error in caget to " + getPV() + " for " + getName(), e);
		}


	}

	private String convertDBRToString(DBR dbr) {
		String valStr = null;
		if (dbr instanceof DBR_CTRL_Enum) {
			valStr = ((DBR_CTRL_Enum) dbr).getLabels()[((DBR_CTRL_Enum) dbr).getEnumValue()[0]];
		} else {
			EpicsDBR epicsDbr = new EpicsDBR(dbr);
			if (epicsDbr._value instanceof byte[]) {
				// convert to ASCII string
				byte[] data = (byte[]) epicsDbr._value;
				int length = 0;
				for (; length < epicsDbr._count; length++) {
					if (data[length] == 0)
						break;
				}
				valStr = new String((byte[]) epicsDbr._value, 0, length);
			} else {
				valStr = epicsDbr._toString();
			}
		}
		return valStr;
	}

	public String getValueAsString() throws TimeoutException, CAException, InterruptedException {
		Channel ch = getChannel();
		DBR dbr = EpicsController.getInstance().getCTRL(ch);
		return convertDBRToString(dbr);
	}

	@Override
	public void addIObserver(IObserver observer) {
		super.addIObserver(observer);
		if (isBeingObserved()) {
			addMonitor();
		}
	}

	private void addMonitor() {
		if (monitor == null) {
			try {
				monitor = EpicsController.getInstance().setMonitor(getChannel(), monitorListener, getAsString ? MonitorType.CTRL
						: MonitorType.NATIVE);
			} catch (Exception e) {
				logger.error("Error adding monitor of " + getPV() + " for " + getName(),e);
			}
		}
	}

	@Override
	public void deleteIObserver(IObserver observer) {
		super.deleteIObserver(observer);
		if (!isBeingObserved()) {
			delMonitor();
		}
	}

	private void delMonitor() {
		if (monitor != null) {
			try {
				EpicsController.getInstance().clearMonitor(monitor);
				monitor = null;
			} catch (Exception e) {
				logger.error("Error clearing monitor of " + getPV() + " for " + getName(),e);
			}
		}
	}

	@Override
	public void deleteIObservers() {
		super.deleteIObservers();
		if (!isBeingObserved()) {
			delMonitor();
		}
	}

	String getUnits() throws DeviceException {
		if( units == null){
			DBR dbr;
			try {
				dbr = EpicsController.getInstance().getCTRL(getChannel());
			} catch (Exception e) {
				throw new DeviceException("Error calling getCTRL for " + getPV(),e);
			}
			if (dbr != null) {
				if (dbr.isDOUBLE()) {
					units = ((DBR_CTRL_Double) dbr).getUnits();
				} else if (dbr.isINT()) {
					units = ((DBR_CTRL_Int) dbr).getUnits();
				} else if (dbr.isSHORT()) {
					units = ((DBR_CTRL_Short) dbr).getUnits();
				} else if (dbr.isFLOAT()) {
					units = ((DBR_CTRL_Float) dbr).getUnits();
				} else if (dbr.isSTRING()) {
					units = "";
				} else if (dbr.isCTRL() && dbr.isENUM()) {
					units = "";
				} else if (dbr.isBYTE()) {
					units = ((DBR_CTRL_Byte) dbr).getUnits();
				} else {
					units="";
				}
				if( !units.isEmpty()){
					Quantity<? extends Quantity<?>> quantity = QuantityFactory.createFromTwoStrings("1.0", units);
					units = quantity.getUnit().toString();
				}
			}
		}
		return units;
	}

	private String getPV() {
		return  pvName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if( !StringUtils.hasText(pvName) && pvProvider != null){
			pvName = pvProvider.getPV();
		}
		if( pvName ==null)
			throw new Exception("pvName or pvProvider not configured");
	}

	public boolean isHasUnits() {
		return hasUnits;
	}

	public void setHasUnits(boolean hasUnits) {
		this.hasUnits = hasUnits;
	}

	@Override
	public boolean isBusy() throws DeviceException {
		return busy;
	}

	public String getPvName() {
		return pvName;
	}

	public void setPvName(String pvName) {
		this.pvName = pvName;
	}

	public void setGetAsString(boolean getAsString) {
		this.getAsString = getAsString;
	}

	public void setPvProvider(PVProvider pvProvider) {
		this.pvProvider = pvProvider;
	}

}
