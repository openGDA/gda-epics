/*-
 * Copyright © 2012 Diamond Light Source Ltd.
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

package uk.ac.gda.epics.adviewer.views;

import java.net.URL;

import org.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import uk.ac.gda.epics.adviewer.ADController;
import uk.ac.gda.epics.adviewer.composites.Histogram;

public class HistogramView extends ViewPart implements InitializingBean{
	private static final Logger logger = LoggerFactory.getLogger(HistogramView.class);

	private Histogram histogram;
	ADController config;

	private String name;

	private Image image;
	
	public HistogramView(ADController config, IConfigurationElement configurationElement) {
		this.config = config;
		name = configurationElement.getAttribute("name");
		try{
			String icon = configurationElement.getAttribute("icon");
			if( icon.isEmpty()){
				image = config.getTwoDarrayViewImageDescriptor().createImage();
			} else {
				URL iconURL = Platform.getBundle(configurationElement.getContributor().getName()).getResource(icon);
				ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(iconURL);
				image = imageDescriptor.createImage();
			}
		}catch (Exception e){
			logger.warn("Unable to get image for view",e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if( config == null)
			throw new Exception("Config is null");
		
	}

	@Override
	public void createPartControl(Composite parent) {

		try {
			parent.setLayout(new FillLayout());

			histogram = new Histogram(this, parent, SWT.NONE);
			histogram.setADController(config);
			histogram.start();
			histogram.startStats();
			histogram.showLeft(true);
/*			List<IAction> actions = new Vector<IAction>();			
			{
				IAction action = new Action("", IAction.AS_CHECK_BOX) {
					@Override
					public void run() {
						histogram.showLeft(!histogram.getShowLeft());
						this.setChecked(histogram.getShowLeft());
					}
				};
				action.setChecked(histogram.getShowLeft());
				action.setToolTipText("Show/Hide Left Panel");
				action.setImageDescriptor(Activator.getImageDescriptor("icons/show_left.png"));
				actions.add(action);
			}	
			for (IAction iAction : actions) {
				getViewSite().getActionBars().getToolBarManager().add(iAction);
			}*/
			
		} catch (Exception e) {
			logger.error("Error starting  areaDetectorProfileComposite", e);
		}
		if( image != null) {
			setTitleImage(image);
		}
		setPartName(name );
	}

	@Override
	public void setFocus() {
		histogram.setFocus();
	}

	@Override
	public void dispose() {
		if( image != null){
			image.dispose();
			image=null;
		}
		super.dispose();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class clazz) {
		if (clazz == IToolPageSystem.class) {
			return this.histogram.getPlottingSystem();
		}
		return super.getAdapter(clazz);
	}


	
	
}
