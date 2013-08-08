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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.plotting.api.PlottingFactory;
import org.dawnsci.plotting.api.axis.IAxis;
import org.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ByteDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.FloatDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IntegerDataset;
import uk.ac.diamond.scisoft.analysis.dataset.LongDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ShortDataset;
import uk.ac.diamond.scisoft.analysis.dataset.function.Histogram;
import uk.ac.gda.epics.adviewer.ADController;
import uk.ac.gda.epics.adviewer.ImageData;

public class TwoDArray extends Composite {

	private static final Logger logger = LoggerFactory.getLogger(TwoDArray.class);

	private ADController config;

	private IPlottingSystem plottingSystem;

	private Observable<Integer> arrayArrayCounterObservable;
	private Observer<Integer> arrayArrayCounterObserver;

	private boolean arrayMonitoring = false;
	private Button arrayMonitoringBtn;
	private Label arrayMonitoringLbl;

	protected boolean autoScale, subtractStore;

	protected AbstractDataset store;

	private Button btnMinusStore;


	public TwoDArray(IViewPart parentViewPart, Composite parent, int style) {
		super(parent, style);

		this.setLayout(new GridLayout(2, false));
		left = new Composite(this, SWT.NONE);
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


		btnAutoscale = new Button(left, SWT.CHECK);
		btnAutoscale.setText("Auto Colour Range");
		btnAutoscale.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				autoScale = btnAutoscale.getSelection();
			}

		});
		autoScale = true;
		btnAutoscale.setSelection(autoScale);

		Button btnStore = new Button(left, SWT.PUSH);
		btnStore.setText("Store");
		btnStore.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if( ads != null){
					store = ads;
				}
				btnMinusStore.setEnabled(store!=null);
			}

		});
		
		btnMinusStore = new Button(left, SWT.CHECK);
		btnMinusStore.setText("Subtract Store");
		btnMinusStore.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				subtractStore = btnMinusStore.getSelection();
			}

		});
		subtractStore = false;
		btnMinusStore.setSelection(subtractStore);
		btnMinusStore.setEnabled(store!=null);
		
		
		Composite right = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(right);
		right.setLayout(new FillLayout());

		try {
			this.plottingSystem = PlottingFactory.getLightWeightPlottingSystem();
		} catch (Exception ne) {
			logger.error("Cannot create a plotting system!", ne);
			return;
		}
		plottingSystem.createPlotPart(right, "", parentViewPart.getViewSite().getActionBars(), PlotType.IMAGE,
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
				if(minCallbackTimeObservable != null && minCallbackTimeObserver!=null ){
					minCallbackTimeObservable.removeObserver(minCallbackTimeObserver);
					minCallbackTimeObserver=null;
				}
				if (plottingSystem != null) {
					plottingSystem.dispose();
					plottingSystem = null;
				}
			}
		});
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
				if( minCallbackTimeObservable != null){
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
					minCallbackTimeObserver.update(null, config.getImageNDArray().getPluginBase().getMinCallbackTime_RBV());
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

	Job updateArrayJob;

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

	public void start() throws Exception {
		config.getImageNDArray().getPluginBase().enableCallbacks();
		if (arrayArrayCounterObservable == null) {
			arrayArrayCounterObservable = config.getImageNDArray().getPluginBase().createArrayCounterObservable();
		}
		if (arrayArrayCounterObserver == null) {
			arrayArrayCounterObserver = new Observer<Integer>() {

				private IImageTrace trace;

				private String getArrayName() {
					return arrayCounter.toString();
				}

				@Override
				public void update(Observable<Integer> source, Integer arg) {
					if (isDisposed() || !viewIsVisible)
						return;
					if (arg == null)
						return;
					TwoDArray.this.arrayCounter = arg;
					if (updateArrayJob == null) {
						updateArrayJob = new Job("Update array") {

							Boolean setMinMax;
							Integer min = null;
							Integer max = null;

							private Runnable updateUIRunnable;
							volatile boolean runnableScheduled = false;

							@Override
							public boolean belongsTo(Object family) {
								return super.belongsTo(family);
							}

							private int getPosToIncludeFractionOfPopulation(AbstractDataset yData,
									Double fractionOfPopulationToInclude) {
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
										ads.setName(getArrayName());
										
										if( subtractStore && store!= null){
											if( Arrays.equals(store.getShape(), ads.getShape() )){
												try{
													ads = ads.isubtract(store);
												} catch (Exception e){
													logger.error("Unable to substract store", e);
												}
											}
										}

										setMinMax = autoScale;
										if (min == null || setMinMax) {
											min = ads.min().intValue();
										}
										if (max == null || setMinMax) {
											max = ads.max().intValue();
										}
										if( max == min){
											max = min+1; //to ensure a range does exist
										}
										if (setMinMax) {
											//set min to .05 percentile value
											//set max to .95 percentile value
											//if these work out the same then resort to min and max of dataset
											int num_bins = 100;
											Histogram hist = new Histogram(num_bins, min, max, true);
											List<AbstractDataset> histogram_values = hist.value(ads);
											if(histogram_values.size()>1){
												DoubleDataset histogramX = (DoubleDataset) histogram_values.get(1)
														.getSlice(new int[] { 0 }, new int[] { num_bins }, new int[] { 1 });
												histogramX.setName("Intensity");
												AbstractDataset histogramY = histogram_values.get(0);
												int jMax = getPosToIncludeFractionOfPopulation(histogramY, .95);
												jMax = Math.min(jMax + 1, histogramY.getSize() - 1);
												int jMin = getPosToIncludeFractionOfPopulation(histogramY, .05);
												jMin = Math.min(jMin - 1, histogramY.getSize() - 1);
												int lmin=min;
												int lmax=max;
												if (jMax >= 0) {
													lmax = (int) histogramX.getDouble(jMax);
												}
												if (jMin >= 0) {
													lmin = (int) histogramX.getDouble(jMin);
												}
												if( lmax != lmin){
													max=lmax;
													min=lmin;
												}
											}
										}

										if (updateUIRunnable == null) {
											updateUIRunnable = new Runnable() {

												@Override
												public void run() {
													runnableScheduled = false;
													AbstractDataset dataToPlot = getDataToPlot();
													if (trace == null
															|| !Arrays.equals(trace.getData().getShape(),
																	dataToPlot.getShape())) {
														trace = (IImageTrace) plottingSystem.updatePlot2D(dataToPlot,
																null, null);
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
								return ads;
							}

							private Integer getMin() {
								return min;
							}

							private Integer getMax() {
								return max;
							}

						};
						updateArrayJob.setUser(false);
						updateArrayJob.setPriority(Job.SHORT);
					}
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
	}

	/**
	 * Needed for the adapter of the parent view to return IToolPageSystem.class
	 */
	public IPlottingSystem getPlottingSystem() {
		return plottingSystem;
	}

	public int getHistSize() {
		return config.getImageHistSize();
	}

	public int getImageMin() {
		return config.getImageMin();
	}

	public int getImageMax() {
		return config.getImageMax();
	}

	public void setViewIsVisible(boolean b) {
		this.viewIsVisible = b;
		if (viewIsVisible)
			arrayArrayCounterObserver.update(null, arrayCounter);
	}

}
