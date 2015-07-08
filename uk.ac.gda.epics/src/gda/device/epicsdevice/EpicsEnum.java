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

package gda.device.epicsdevice;

import gov.aps.jca.dbr.DBR_STS_Enum;

import java.io.Serializable;

/**
 * EpicsEnum Class
 */
public class EpicsEnum implements Serializable {
	final static long serialVersionUID = 1;
	final EpicsValuedEnum _status;
	final EpicsValuedEnum _severity;
	/**
	 *
	 */
	final public EpicsDBR _dbr;

	EpicsEnum(DBR_STS_Enum obj) {
		_severity = new EpicsValuedEnum(obj.getSeverity());
		_status = new EpicsValuedEnum(obj.getStatus());
		_dbr = new EpicsDBR(obj);
	}
}
