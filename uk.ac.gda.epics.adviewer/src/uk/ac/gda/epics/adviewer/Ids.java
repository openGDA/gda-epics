/*-
 * Copyright © 2013 Diamond Light Source Ltd.
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

package uk.ac.gda.epics.adviewer;

public class Ids {
	//Do not change without changing plugin.xml
	public static final String COMMAND_PARAMTER_ADCONTROLLER_SERVICE_NAME = "uk.ac.gda.epics.adviewer.commandParameters.adcontrollerServiceName";
	public static final String COMMANDS_SET_EXPOSURE="uk.ac.gda.epics.adviewer.commands.setExposure";
	public static final String COMMANDS_SET_LIVEVIEW_SCALE="uk.ac.gda.epics.adviewer.commands.setLiveViewScale";
	public static final String COMMANDS_SHOW_LIVEVIEW="uk.ac.gda.epics.adviewer.showLiveView";
	public static final String COMMANDS_FIT_IMAGE_TO_WINDOW="uk.ac.gda.epics.adviewer.command.zoomToFit";
	
	public static final String COMMANDS_SHOW_HISTOGRAM_VIEW="uk.ac.gda.epics.adviewer.histogramview";
	public static final String COMMANDS_SHOW_RAW_IMAGE_VIEW="uk.ac.gda.epics.adviewer.rawimageview";
}
