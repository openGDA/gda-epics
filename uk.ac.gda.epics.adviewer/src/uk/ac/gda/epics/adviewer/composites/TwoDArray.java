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

package uk.ac.gda.epics.adviewer.composites;

import gda.device.detector.areadetector.v17.NDPluginBase;
import gda.device.detector.areadetector.v17.NDROI;
import gda.observable.Observable;
import gda.observable.Observer;

import java.io.File;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Datatype;

import org.dawb.hdf5.HierarchicalDataFactory;
import org.dawb.hdf5.HierarchicalDataFileUtils;
import org.dawb.hdf5.IHierarchicalDataFile;
import org.dawb.hdf5.Nexus;
import org.dawnsci.io.h5.H5LazyDataset;
import org.dawnsci.io.h5.H5Utils;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.plotting.api.axis.IAxis;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ByteDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.FloatDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IntegerDataset;
import uk.ac.diamond.scisoft.analysis.dataset.LazyDataset;
import uk.ac.diamond.scisoft.analysis.dataset.LongDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.dataset.ShortDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.dataset.function.Histogram;
import uk.ac.diamond.scisoft.analysis.plotserver.AxisMapBean;
import uk.ac.diamond.scisoft.analysis.plotserver.DataBean;
import uk.ac.diamond.scisoft.analysis.plotserver.DataBeanException;
import uk.ac.diamond.scisoft.analysis.plotserver.DataSetWithAxisInformation;
import uk.ac.diamond.scisoft.analysis.plotserver.GuiBean;
import uk.ac.diamond.scisoft.analysis.plotserver.GuiParameters;
import uk.ac.diamond.scisoft.analysis.plotserver.GuiPlotMode;
import uk.ac.diamond.scisoft.analysis.rcp.views.PlotView;
import uk.ac.gda.epics.adviewer.ADController;
import uk.ac.gda.epics.adviewer.Activator;
import uk.ac.gda.epics.adviewer.ImageData;

public class TwoDArray extends Composite {

	public enum OptionIndex {
		I, I_MINUS_A, I_MUNIS_B, I_NORMALISED, I_OVER_A, I_OVER_B, A, B
	}

	private static final Logger logger = LoggerFactory.getLogger(TwoDArray.class);

	private ADController config;

	private IPlottingSystem plottingSystem;

	private Observable<Integer> arrayArrayCounterObservable;
	private Observer<Integer> arrayArrayCounterObserver;

	private boolean arrayMonitoring = false;
	private Button arrayMonitoringBtn;
	private Label arrayMonitoringLbl;

	protected boolean autoScale;

	Map<String, AbstractDataset> stores = new HashMap<String, AbstractDataset>();

	private ScrolledComposite leftScrolledComposite;

	private Button middle;
	
	UpdateArrayJob updateArrayJob;

	private IOCStatus statusComposite;

	private MinCallbackTimeComposite minCallbackTimeComposite;

	private boolean viewIsVisible;
	private Integer arrayCounter;
	private Button btnAutoscale;

	private Composite left;

	private Observable<Double> minCallbackTimeObservable;

	private NDPluginBase imageNDROIPluginBase;

	private Observer<Double> minCallbackTimeObserver;
	AbstractDataset ads = null;
	private Group grpStores;
	private Button btnA;
	private Button btnB;
	private ComboViewer comboShow;
	private Group grpShow;

	ShowOption showOptionDefault = new ShowOption("I", OptionIndex.I);
	ShowOption showOption = showOptionDefault;

	// id used in DataBinding
	static final String showOptionName = "showOption";
	private Composite composite;
	private Button btnSnapshot;
	
	public TwoDArray(IViewPart parentViewPart, Composite parent, int style) throws Exception {
		super(parent, style);

		this.setLayout(new GridLayout(3,false));
		
		leftScrolledComposite= new ScrolledComposite(this,SWT.V_SCROLL| SWT.H_SCROLL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(leftScrolledComposite);
		left = new Composite(leftScrolledComposite, SWT.NONE);
		leftScrolledComposite.setContent(left);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(left);
		RowLayout layout = new RowLayout(SWT.VERTICAL);
		layout.center = true;
		layout.pack = false;
		left.setLayout(new GridLayout(1, false));

		statusComposite = new IOCStatus(left, SWT.NONE);
		GridData gd_statusComposite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_statusComposite.widthHint = 154;
		statusComposite.setLayoutData(gd_statusComposite);

		minCallbackTimeComposite = new MinCallbackTimeComposite(left, SWT.NONE);
		Group stateGroup = new Group(left, SWT.NONE);
		GridData gd_stateGroup = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_stateGroup.widthHint = 150;
		stateGroup.setLayoutData(gd_stateGroup);
		stateGroup.setText("Array View");
		stateGroup.setLayout(new GridLayout(2, false));
		arrayMonitoringLbl = new Label(stateGroup, SWT.CENTER);
		GridData gd_arrayMonitoringLbl = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_arrayMonitoringLbl.widthHint = 81;
		arrayMonitoringLbl.setLayoutData(gd_arrayMonitoringLbl);
		arrayMonitoringBtn = new Button(stateGroup, SWT.PUSH | SWT.CENTER);
		GridData gd_arrayMonitoringBtn = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_arrayMonitoringBtn.widthHint = 48;
		arrayMonitoringBtn.setLayoutData(gd_arrayMonitoringBtn);

		composite = new Composite(left, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 155;
		composite.setLayoutData(gd_composite);
		RowLayout rl_composite = new RowLayout(SWT.HORIZONTAL);
		rl_composite.justify = true;
		rl_composite.center = true;
		rl_composite.wrap = false;
		composite.setLayout(rl_composite);

		grpStores = new Group(composite, SWT.NONE);
		grpStores.setLayoutData(new RowData(64, SWT.DEFAULT));
		grpStores.setLayout(new RowLayout(SWT.HORIZONTAL));
		grpStores.setText("Store As");

		btnA = new Button(grpStores, SWT.NONE);
		btnA.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AbstractDataset clone = ads.clone();
				clone.setName("A");
				TwoDArray.this.setupStores("A", clone);
			}
		});
		btnA.setText("A");

		btnB = new Button(grpStores, SWT.NONE);
		btnB.setText("B");

		btnB.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AbstractDataset clone = ads.clone();
				clone.setName("B");
				TwoDArray.this.setupStores("B", clone);
			}
		});
		btnSnapshot = new Button(composite, SWT.NONE);
		btnSnapshot.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					updateArrayJob.snapShot();
				} catch (Exception e1) {
					logger.error("Error taking snapshot", e1);
				}
			}
		});
		btnSnapshot.setText("Snapshot");

		grpShow = new Group(left, SWT.NONE);
		GridData gd_grpShow = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_grpShow.widthHint = 151;
		grpShow.setLayoutData(gd_grpShow);
		grpShow.setText("Show");
		grpShow.setLayout(new FillLayout(SWT.HORIZONTAL));

		comboShow = new ComboViewer(grpShow, SWT.READ_ONLY);
		comboShow.setContentProvider(ArrayContentProvider.getInstance());
		comboShow.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof ShowOption) {
					ShowOption opt = (ShowOption) element;
					return opt.getLabel();
				}
				return super.getText(element);
			}

		});

		ShowOption[] showOption = new ShowOption[] { showOptionDefault, new ShowOption("I-A", OptionIndex.I_MINUS_A),
				new ShowOption("I/A", OptionIndex.I_OVER_A), new ShowOption("I-B", OptionIndex.I_MUNIS_B),
				new ShowOption("I/B", OptionIndex.I_OVER_B), new ShowOption("I-B/A-B", OptionIndex.I_NORMALISED),
				new ShowOption("A", OptionIndex.A), new ShowOption("B", OptionIndex.B) };
		comboShow.setInput(showOption);

		IObservableValue comboShowObservableValue = ViewersObservables.observeSingleSelection(comboShow);
		IObservableValue showOptionObserveValue = PojoObservables.observeValue(this, showOptionName);

		DataBindingContext bindingContext = new DataBindingContext();
		bindingContext.bindValue(comboShowObservableValue, showOptionObserveValue);
		showOptionObserveValue.setValue(showOptionDefault);

		btnAutoscale = new Button(left, SWT.CHECK);
		btnAutoscale.setText("Fast Colour Map");
		btnAutoscale.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				autoScale = btnAutoscale.getSelection();
			}

		});
		autoScale = true;
		btnAutoscale.setSelection(autoScale);

		left.setSize(left.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		middle = new Button(this,SWT.PUSH | SWT.TOP);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.CENTER, SWT.BEGINNING).applyTo(middle);
		middle.setText(">");
		middle.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				showLeft(!getShowLeft());
			}});		
		
/*		Composite right = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(right);
		right.setLayout(new FillLayout());
*/
		Composite right = new Composite(this, SWT.NONE);		
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(right);
		GridLayoutFactory.fillDefaults().applyTo(right);
		
		Composite plotArea = new Composite(right, SWT.NONE);
		plotArea.setLayout(new FillLayout());
		{
			GridData gridData = new GridData();
			gridData.horizontalAlignment = SWT.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			gridData.verticalAlignment = SWT.FILL;
			plotArea.setLayoutData(gridData);
		}		


		this.plottingSystem = PlottingFactory.getLightWeightPlottingSystem();
		plottingSystem.createPlotPart(plotArea, "", parentViewPart.getViewSite().getActionBars(), PlotType.IMAGE,
				parentViewPart);
		for (IAxis axis : plottingSystem.getAxes()) {
			axis.setTitle("");
		}
		addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				try {
					stop();
				} catch (Exception ee) {
					logger.error("Error stopping histogram computation", ee);
				}
				if (minCallbackTimeObservable != null && minCallbackTimeObserver != null) {
					minCallbackTimeObservable.removeObserver(minCallbackTimeObserver);
					minCallbackTimeObserver = null;
				}
				if (plottingSystem != null) {
					plottingSystem.dispose();
					plottingSystem = null;
				}
			}
		});
	}
	private boolean showLeft;

	/**
	 * @param showLeft
	 */
	public void showLeft(Boolean showLeft) {
		this.showLeft = showLeft;
		GridData data = (GridData) leftScrolledComposite.getLayoutData();
		data.exclude = !showLeft;
		leftScrolledComposite.setVisible(showLeft);
		middle.setText(showLeft ? "<" : ">");
		layout(false);
	}

	/**
	 * @return true if left is hidden
	 */
	public Boolean getShowLeft() {
		return showLeft;
	}
	protected void setupStores(String storeName, AbstractDataset ads) {
		stores.put(storeName, ads);
		AbstractDataset storeA = stores.get("A");
		AbstractDataset storeB = stores.get("B");
		stores.remove("A-B");
		if (storeA != null && storeB != null && Arrays.equals(storeA.getShape(), storeB.getShape())) {
			stores.put("A-B", new DoubleDataset(storeA.isubtract(storeB)));
		}
	}

	public void setADController(ADController config) throws Exception {
		this.config = config;

		// Configure AreaDetector
		NDPluginBase imageNDArrayBase = config.getImageNDArray().getPluginBase();
		minCallbackTimeComposite.setPluginBase(imageNDArrayBase);
		try {
			minCallbackTimeObservable = imageNDArrayBase.createMinCallbackTimeObservable();
			minCallbackTimeComposite.setMinTimeObservable(minCallbackTimeObservable);
			minCallbackTimeComposite.setMinCallbackTime(config.getArrayMinCallbackTime());
		} catch (Exception e2) {
			logger.error("Error setting min callback time", e2);
		}

		String sourcePortName = config.getImageNDArrayPortInput();
		String imageNDArrayPort = imageNDArrayBase.getNDArrayPort_RBV();
		if (imageNDArrayPort == null || !imageNDArrayPort.equals(sourcePortName))
			imageNDArrayBase.setNDArrayPort(sourcePortName);
		if (!imageNDArrayBase.isCallbacksEnabled_RBV())
			imageNDArrayBase.enableCallbacks();

		arrayMonitoringBtn.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					if (arrayMonitoring) {
						stop();
					} else {
						start();
					}
				} catch (Exception ex) {
					logger.error("Error responding to start_stop button", ex);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		setStarted(arrayMonitoring);
		try {
			start();
		} catch (Exception e) {
			logger.error("Error starting  areaDetectorViewComposite", e);
		}
		try {
			statusComposite.setObservable(imageNDArrayBase.createConnectionStateObservable());
		} catch (Exception e1) {
			logger.error("Error monitoring connection state", e1);
		}

		NDROI imageNDROI = config.getImageNDROI();
		if (imageNDROI != null) {
			TwoDArrayROI twoDArrayROI;
			twoDArrayROI = new TwoDArrayROI(left, SWT.NONE);
			twoDArrayROI.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			GridDataFactory.fillDefaults().grab(true, false).applyTo(twoDArrayROI);
			twoDArrayROI.setVisible(true);
			layout(true);

			try {
				twoDArrayROI.setNDRoi(imageNDROI, getPlottingSystem());

				// setup Port for NDROI to match that of the Proc plugin - this should be the camera.
				imageNDROIPluginBase = imageNDROI.getPluginBase();
				String roiNDArrayPort = imageNDROIPluginBase.getNDArrayPort_RBV();
				if (roiNDArrayPort == null || !roiNDArrayPort.equals(sourcePortName))
					imageNDROIPluginBase.setNDArrayPort(sourcePortName);
				config.getImageNDArray().getPluginBase().setNDArrayPort(imageNDROIPluginBase.getPortName_RBV());

				imageNDROIPluginBase.enableCallbacks();

				twoDArrayROI.addMonitoringbtnSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						super.widgetSelected(e);
						if (!arrayMonitoring) {
							try {
								start();
							} catch (Exception e1) {
								logger.error("Error starting", e1);
							}
						}
					}

				});
				if (minCallbackTimeObservable != null) {
					minCallbackTimeObserver = new Observer<Double>() {

						@Override
						public void update(Observable<Double> source, Double mincallbacktime) {
							try {
								imageNDROIPluginBase.setMinCallbackTime(mincallbacktime);
							} catch (Exception e) {
								logger.error("Error setting minCallbackTime", e);
							}

						}
					};
					minCallbackTimeObservable.addObserver(minCallbackTimeObserver);
					minCallbackTimeObserver.update(null, config.getImageNDArray().getPluginBase()
							.getMinCallbackTime_RBV());
				}
			} catch (Exception e1) {
				logger.error("Error configuring the ROI", e1);
			}
		}
	}

	public void stop() throws Exception {
		config.getImageNDArray().getPluginBase().disableCallbacks();
		if (arrayArrayCounterObservable != null && arrayArrayCounterObserver != null) {
			arrayArrayCounterObservable.removeObserver(arrayArrayCounterObserver);
			arrayArrayCounterObserver = null;
			arrayArrayCounterObservable = null;
		}
		setStarted(false);
	}

	public ShowOption getShowOption() {
		return showOption;
	}

	public void setShowOption(ShowOption showOption) {
		this.showOption = showOption;
	}

	public void start() throws Exception {
		config.getImageNDArray().getPluginBase().enableCallbacks();
		if (arrayArrayCounterObservable == null) {
			arrayArrayCounterObservable = config.getImageNDArray().getPluginBase().createArrayCounterObservable();
		}
		if (updateArrayJob == null) {
			updateArrayJob = new UpdateArrayJob("Update array");
			updateArrayJob.setUser(false);
			updateArrayJob.setPriority(Job.SHORT);
		}
		if (arrayArrayCounterObserver == null) {
			arrayArrayCounterObserver = new Observer<Integer>() {

				@Override
				public void update(Observable<Integer> source, Integer arg) {
					if (isDisposed() || !viewIsVisible)
						return;
					if (arg == null)
						return;
					TwoDArray.this.arrayCounter = arg;
					updateArrayJob.schedule(); // rate is limited by min update time already

				}
			};
		}
		arrayArrayCounterObservable.addObserver(arrayArrayCounterObserver);
		setStarted(true);
	}

	private void setStarted(boolean b) {
		arrayMonitoring = b;
		arrayMonitoringBtn.setText(b ? "Stop" : "Start");
		arrayMonitoringLbl.setText(b ? "Running" : "Stopped");
		if( !viewIsVisible){
			arrayMonitoringLbl.setText("INACTIVE");
		}
		arrayMonitoringLbl.setForeground(getDisplay().getSystemColor(viewIsVisible ? 
				(arrayMonitoring ? SWT.COLOR_GREEN: SWT.COLOR_BLACK) : SWT.COLOR_RED));
	}

	/**
	 * Needed for the adapter of the parent view to return IToolPageSystem.class
	 */
	public IPlottingSystem getPlottingSystem() {
		return plottingSystem;
	}

	public int getHistSize() throws Exception {
		return config.getImageHistSize();
	}

	public double getImageMin() throws Exception {
		return config.getImageMin();
	}

	public double getImageMax() throws Exception {
		return config.getImageMax();
	}

	public void setViewIsVisible(boolean b) {
		this.viewIsVisible = b;
		if (viewIsVisible)
			arrayArrayCounterObserver.update(null, arrayCounter);
		setStarted(arrayMonitoring);
	}

	private ncsa.hdf.object.Group createParentEntry(IHierarchicalDataFile file, String fullEntry) throws Exception {
		return HierarchicalDataFileUtils.createParentEntry(file, fullEntry, Nexus.DATA);
	}

	protected void saveStores(String name) {
		try {
			String fileName = Activator.getDefault().getStateLocation().append(name + ".hdf").toOSString();
			IHierarchicalDataFile writer = HierarchicalDataFactory.getWriter(fileName);
			try {
				ncsa.hdf.object.Group parent = createParentEntry(writer, "/entry/stores");
				for (Entry<String, AbstractDataset> store : stores.entrySet()) {
					AbstractDataset data = store.getValue();
					String dataName = store.getKey();
					final Datatype datatype = H5Utils.getDatatype(data);
					final long[] shape = H5Utils.getLong(data.getShape());
					final Dataset dataset = writer.replaceDataset(dataName, datatype, shape, data.getBuffer(), parent);
					writer.setNexusAttribute(dataset, Nexus.SDS);
				}
			} finally {
				if (writer != null)
					writer.close();
			}
		} catch (Exception e) {
			logger.error("Error saving state", e);
		}

	}

	void restoreStores(String name) {
		stores.clear();
		String fileName = Activator.getDefault().getStateLocation().append(name + ".hdf").toOSString();
		File file = new File(fileName);
		if (file.exists()) {
			try {
				IHierarchicalDataFile reader = HierarchicalDataFactory.getReader(fileName);
				try {
					final List<String> fullPaths = reader.getDatasetNames(IHierarchicalDataFile.NUMBER_ARRAY);
					for (String fullPath : fullPaths) {
						String[] entries = fullPath.split("/");
						String dsName = entries[entries.length - 1];
						if (dsName.equals("A-B"))
							continue;
						Dataset set = (Dataset) reader.getData(fullPath);
						LazyDataset lazy = new H5LazyDataset(set);
						AbstractDataset store = DatasetUtils.convertToAbstractDataset(lazy.getSlice((Slice) null));
						store.setName(dsName);
						setupStores(dsName, store);
					}
				} finally {
					if (reader != null)
						reader.close();
				}
			} catch (Exception e) {
				logger.error("Error reading cache from " + fileName);
			}
		}
	}

	public void save(String name) {
		saveStores(name);
	}

	public void restore(String name) {
		restoreStores(name);
	}

	private class UpdateArrayJob extends Job {

		public UpdateArrayJob(String name) {
			super(name);
		}

		private IImageTrace trace;

		Boolean setMinMax;
		Integer min = null;
		Integer max = null;

		private Runnable updateUIRunnable;

		volatile boolean runnableScheduled = false;
		private AbstractDataset nonNullDSToPlot;
		PlotView plotView;

		void snapShot() throws Exception {
			if (nonNullDSToPlot != null) {
				final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IViewPart showView = window.getActivePage().showView("uk.ac.gda.epics.adviewer.snapshotView");
				if (showView instanceof PlotView) {
					plotView = (PlotView) showView;
					Display.getDefault().asyncExec(new Runnable() {

						@Override
						public void run() {
							try {
								GuiBean guiBean = new GuiBean();
								guiBean.put(GuiParameters.PLOTMODE, GuiPlotMode.TWOD);
								guiBean.put(GuiParameters.TITLE, nonNullDSToPlot.getName());
								plotView.processGUIUpdate(guiBean);
								plotView.updatePlotMode(GuiPlotMode.TWOD);
								DataBean dataBean = new DataBean(GuiPlotMode.TWOD);

								DataSetWithAxisInformation axisData = new DataSetWithAxisInformation();
								AxisMapBean amb = new AxisMapBean();
								axisData.setAxisMap(amb);
								axisData.setData(nonNullDSToPlot);
								dataBean.addData(axisData);
								plotView.processPlotUpdate(dataBean);
							} catch (DataBeanException e) {
								logger.error("Error updating snapshot view", e);
							}
						}
					});

					/*
					 * if (xValues != null) { dataBean.addAxis(AxisMapBean.XAXIS, xValues); } if (yValues != null) {
					 * dataBean.addAxis(AxisMapBean.YAXIS, yValues); }
					 */
				}
			}
		}

		@Override
		public boolean belongsTo(Object family) {
			return super.belongsTo(family);
		}

		private int getPosToIncludeFractionOfPopulation(AbstractDataset yData, Double fractionOfPopulationToInclude) {
			Double sum = (Double) yData.sum();
			double popIncluded = 0;
			int j = 0;
			double popRequired = sum * fractionOfPopulationToInclude;
			int size = yData.getSize();
			while (popIncluded < popRequired && j < size) {
				popIncluded += yData.getDouble(j);
				if (popIncluded < popRequired)
					j++;
			}
			return Math.min(j, size - 1);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				ImageData imageData;
				imageData = config.getImageData();
				imageData.toString();
				if (imageData.data.getClass().isArray()) {
					Object object = Array.get(imageData.data, 0);
					if (object instanceof Short) {
						ads = new ShortDataset((short[]) (imageData.data), imageData.dimensions);
					} else if (object instanceof Double) {
						ads = new DoubleDataset((double[]) (imageData.data), imageData.dimensions);
					} else if (object instanceof Long) {
						ads = new LongDataset((long[]) (imageData.data), imageData.dimensions);
					} else if (object instanceof Byte) {
						ads = new ByteDataset((byte[]) (imageData.data), imageData.dimensions);
					} else if (object instanceof Float) {
						ads = new FloatDataset((float[]) (imageData.data), imageData.dimensions);
					} else if (object instanceof Integer) {
						ads = new IntegerDataset((int[]) (imageData.data), imageData.dimensions);
					} else {
						throw new IllegalArgumentException("Type of data not recognised: "
								+ object.getClass().getName());
					}
					ads.setName(arrayCounter.toString());
					AbstractDataset dsToShow = null;
					String explanation = "";

					OptionIndex showIndex = showOption.getIndex();
					switch (showIndex) {
					case I: {
						dsToShow = ads;
						break;
					}
					case B:
					case I_OVER_B:
					case I_MUNIS_B: {
						AbstractDataset store = stores.get("B");
						if (store != null) {
							if (showIndex == OptionIndex.B) {
								dsToShow = store;
							} else {
								if (Arrays.equals(store.getShape(), ads.getShape())) {
									boolean isOver = showIndex == OptionIndex.I_OVER_B;
									dsToShow = isOver ? Maths.divide(ads, store) : ads.isubtract(store);
									dsToShow.setName(ads.getName() + (isOver ? " / " : " - ") + store.getName());
								} else {
									explanation = new String("B does not match current image");
								}
							}
						} else {
							explanation = new String("B is empty");
						}
						break;
					}
					case A:
					case I_OVER_A:
					case I_MINUS_A: {
						AbstractDataset store = stores.get("A");
						if (store != null) {
							if (showIndex == OptionIndex.A) {
								dsToShow = store;
							} else {
								if (Arrays.equals(store.getShape(), ads.getShape())) {
									boolean isOver = showIndex == OptionIndex.I_OVER_A;
									dsToShow = isOver ? Maths.divide(ads, store) : ads.isubtract(store);
									dsToShow.setName(ads.getName() + (isOver ? " / " : " - ") + store.getName());
								} else {
									explanation = new String("A does not match current image");
								}
							}
						} else {
							explanation = new String("A is empty");
						}
						break;
					}
					case I_NORMALISED: {
						// I-B/A-B
						AbstractDataset storeB = stores.get("B");
						DoubleDataset storeA_B = (DoubleDataset) stores.get("A-B");
						if (storeB != null && storeA_B != null && Arrays.equals(storeB.getShape(), ads.getShape())) {
							DoubleDataset ds = new DoubleDataset(ads.isubtract(storeB));
							dsToShow = Maths.dividez(ds, storeA_B);
							dsToShow.setName("(" + ads.getName() + "-B)/(A-B)");
						} else {
							explanation = new String("A or B does not match current image");
						}
						break;
					}
					}
					if (dsToShow == null) {
						dsToShow = new IntegerDataset(new int[] { 0, 0, 0, 0 }, 2, 2);
						dsToShow.setName("Invalid selection:" + explanation);
					}
					nonNullDSToPlot = dsToShow;
					setMinMax = autoScale;
					if (min == null || setMinMax) {
						min = dsToShow.min().intValue();
					}
					if (max == null || setMinMax) {
						max = dsToShow.max().intValue();
					}
					if (max == min) {
						max = min + 1; // to ensure a range does exist
					}
					if (setMinMax) {
						// set min to .05 percentile value
						// set max to .95 percentile value
						// if these work out the same then resort to min and max of dataset
						int num_bins = 100;
						Histogram hist = new Histogram(num_bins, min, max, true);
						List<AbstractDataset> histogram_values = hist.value(dsToShow);
						if (histogram_values.size() > 1) {
							DoubleDataset histogramX = (DoubleDataset) histogram_values.get(1).getSlice(
									new int[] { 0 }, new int[] { num_bins }, new int[] { 1 });
							histogramX.setName("Intensity");
							AbstractDataset histogramY = histogram_values.get(0);
							int jMax = getPosToIncludeFractionOfPopulation(histogramY, .95);
							jMax = Math.min(jMax + 1, histogramY.getSize() - 1);
							int jMin = getPosToIncludeFractionOfPopulation(histogramY, .05);
							jMin = Math.min(jMin - 1, histogramY.getSize() - 1);
							int lmin = min;
							int lmax = max;
							if (jMax >= 0) {
								lmax = (int) histogramX.getDouble(jMax);
							}
							if (jMin >= 0) {
								lmin = (int) histogramX.getDouble(jMin);
							}
							if (lmax != lmin) {
								max = lmax;
								min = lmin;
							}
						}
					}

					if (updateUIRunnable == null) {
						updateUIRunnable = new Runnable() {

							@Override
							public void run() {
								runnableScheduled = false;
								AbstractDataset dataToPlot = getDataToPlot();
								if (trace == null || !Arrays.equals(trace.getData().getShape(), dataToPlot.getShape())) {
									trace = (IImageTrace) plottingSystem.updatePlot2D(dataToPlot, null, null);
								}
								String title = dataToPlot.getName();
								trace.setName(title);

								trace.setMin(getMin());
								trace.setMax(getMax());
								trace.setRescaleHistogram(false);
								plottingSystem.setTitle(title);
								plottingSystem.updatePlot2D(dataToPlot, null, null);
							}

						};
					}
					if (!runnableScheduled) {
						if (!isDisposed()) {
							runnableScheduled = true;
							getDisplay().asyncExec(updateUIRunnable);
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error reading image data", e);
			}
			return Status.OK_STATUS;
		}

		private AbstractDataset getDataToPlot() {
			return nonNullDSToPlot;
		}

		private Integer getMin() {
			return min;
		}

		private Integer getMax() {
			return max;
		}
	}
}

class ShowOption {
	final String label;
	final TwoDArray.OptionIndex index;

	public ShowOption(String label, TwoDArray.OptionIndex index) {
		super();
		this.label = label;
		this.index = index;
	}

	public String getLabel() {
		return label;
	}

	public TwoDArray.OptionIndex getIndex() {
		return index;
	}

}