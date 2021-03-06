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

package gda.device.enumpositioner;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gda.device.DeviceException;
import gda.device.EditableEnumPositioner;
import gda.factory.FactoryException;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;

/**
 * Version of EpicsPositionerCallback which enables the GDA user to edit the names of the positions in the positioner.
 */
public class EpicsEditablePositionerCallback extends EpicsPositionerCallback implements EditableEnumPositioner{

	private static final Logger logger = LoggerFactory.getLogger(EpicsEditablePositionerCallback.class);

	private String[] recordNames = new String[EpicsEnumConstants.CHANNEL_NAMES.length];
	private Channel[] namesChannels = new Channel[EpicsEnumConstants.CHANNEL_NAMES.length];

	@Override
	protected void setRecordNamesUsingBasePv(String recordName) {

		for (int i = 0; i < EpicsEnumConstants.CHANNEL_NAMES.length; i++) {
			recordNames[i] = recordName + ":SELECT." + EpicsEnumConstants.CHANNEL_NAMES[i];
		}

		super.setRecordNamesUsingBasePv(recordName);

	}

	@Override
	protected void createChannelAccess() throws FactoryException {

		for (int i = 0; i < EpicsEnumConstants.CHANNEL_NAMES.length; i++) {
			try {
				namesChannels[i] = channelManager.createChannel(recordNames[i], false);
			} catch (CAException e) {
				throw new FactoryException("failed to connect to all channels", e);
			}
		}
		super.createChannelAccess();
	}

	@Override
	public void setPositions(String[] newPositions) {

		if (controller == null || channelManager == null || !isConfigured()) {
			logger.error("Trying to over write EPICS positions when not yet connected. Do you mean to do this? Consider using EpicsSimplePositioner instead.");
		}

		try {
			for (int i = 0; i < newPositions.length; i++) {
				if (!newPositions[i].isEmpty()) {
					controller.caput(namesChannels[i], newPositions[i]);
				}
			}
		} catch (Exception e) {
			logger.error("failed to get position from " + select.getName());
		}
	}

	@Override
	public void setPositions(Collection<String> positions) {
		setPositions(positions.toArray(new String[positions.size()]));
	}

	/**
	 * Change the label of a single position in the positioner.
	 *
	 * @param index
	 * @param newPosition
	 * @throws DeviceException
	 */
	public void setPosition(int index, String newPosition) throws DeviceException {
		String[] positions = getPositions();
		positions[index] = newPosition;
		setPositions(positions);
	}
}
