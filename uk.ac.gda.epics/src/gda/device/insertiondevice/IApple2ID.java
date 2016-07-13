/*-
 * Copyright © 2016 Diamond Light Source Ltd.
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

package gda.device.insertiondevice;

import gda.device.DeviceException;
import gda.factory.Configurable;
import gda.observable.IObservable;

public interface IApple2ID extends Configurable, IObservable {

	void asynchronousMoveTo(Apple2IDPosition position) throws DeviceException;

	boolean isBusy();

	Apple2IDPosition getPosition() throws DeviceException;

	String getIDMode() throws DeviceException;

	boolean isEnabled() throws DeviceException;

	double getMaxPhaseMotorPos();

	boolean motorPositionsEqual(final double a, final double b);
}
