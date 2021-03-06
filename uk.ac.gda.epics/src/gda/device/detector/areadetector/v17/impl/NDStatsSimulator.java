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

package gda.device.detector.areadetector.v17.impl;

import gda.device.detector.areadetector.v17.NDStats;
import gda.observable.Observable;

public class NDStatsSimulator extends NDBaseImpl implements NDStats {

	private double minValue=0.;
	private double maxValue=0.;
	private double meanValue=0.;
	private double total;
	private double net;
	private double sigma;

	@Override
	public short getComputeStatistics() throws Exception {

		return 0;
	}

	@Override
	public void setComputeStatistics(int computestatistics) throws Exception {

	}

	@Override
	public short getComputeStatistics_RBV() throws Exception {

		return 0;
	}

	@Override
	public int getBgdWidth() throws Exception {

		return 0;
	}

	@Override
	public void setBgdWidth(int bgdwidth) throws Exception {

	}

	@Override
	public int getBgdWidth_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getMinValue_RBV() throws Exception {
		return minValue* (1.0 - Math.random()*0.1);
	}

	@Override
	public double getMaxValue_RBV() throws Exception {

		return maxValue* (1.0 - Math.random()*0.1);
	}

	@Override
	public double getMeanValue_RBV() throws Exception {

		return meanValue* (1.0 - Math.random()*0.1);
	}

	@Override
	public double getSigma_RBV() throws Exception {

		return sigma * (1.0 - Math.random()*0.1);
	}

	@Override
	public double getTotal_RBV() throws Exception {

		return total * (1.0 - Math.random()*0.1);
	}

	@Override
	public double getNet_RBV() throws Exception {

		return net * (1.0 - Math.random()*0.1);
	}

	@Override
	public short getComputeCentroid() throws Exception {

		return 0;
	}

	@Override
	public void setComputeCentroid(int computecentroid) throws Exception {

	}

	@Override
	public short getComputeCentroid_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getCentroidThreshold() throws Exception {

		return 0;
	}

	@Override
	public void setCentroidThreshold(double centroidthreshold) throws Exception {

	}

	@Override
	public double getCentroidThreshold_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getCentroidX_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getCentroidY_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getSigmaX_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getSigmaY_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getSigmaXY_RBV() throws Exception {

		return 0;
	}

	@Override
	public short getComputeProfiles() throws Exception {

		return 0;
	}

	@Override
	public void setComputeProfiles(int computeprofiles) throws Exception {

	}

	@Override
	public short getComputeProfiles_RBV() throws Exception {

		return 0;
	}

	@Override
	public int getProfileSizeX_RBV() throws Exception {

		return 0;
	}

	@Override
	public int getProfileSizeY_RBV() throws Exception {

		return 0;
	}

	@Override
	public int getCursorX() throws Exception {

		return 0;
	}

	@Override
	public void setCursorX(int cursorx) throws Exception {

	}

	@Override
	public int getCursorX_RBV() throws Exception {

		return 0;
	}

	@Override
	public int getCursorY() throws Exception {

		return 0;
	}

	@Override
	public void setCursorY(int cursory) throws Exception {

	}

	@Override
	public int getCursorY_RBV() throws Exception {

		return 0;
	}

	@Override
	public double[] getProfileAverageX_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileAverageY_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileThresholdX_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileThresholdY_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileCentroidX_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileCentroidY_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileCursorX_RBV() throws Exception {

		return null;
	}

	@Override
	public double[] getProfileCursorY_RBV() throws Exception {

		return null;
	}

	@Override
	public short getComputeHistogram() throws Exception {

		return 0;
	}

	@Override
	public void setComputeHistogram(int computehistogram) throws Exception {

	}

	@Override
	public short getComputeHistogram_RBV() throws Exception {

		return 0;
	}

	@Override
	public int getHistSize() throws Exception {

		return 0;
	}

	@Override
	public void setHistSize(int histsize) throws Exception {

	}

	@Override
	public int getHistSize_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getHistMin() throws Exception {

		return 0;
	}

	@Override
	public void setHistMin(double histmin) throws Exception {

	}

	@Override
	public double getHistMin_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getHistMax() throws Exception {

		return 0;
	}

	@Override
	public void setHistMax(double histmax) throws Exception {

	}

	@Override
	public double getHistMax_RBV() throws Exception {

		return 0;
	}

	@Override
	public double getHistEntropy_RBV() throws Exception {

		return 0;
	}

	@Override
	public double[] getHistogram_RBV() throws Exception {
		return null;
	}

	@Override
	public double[] getHistogram_RBV(int length) throws Exception {
		return null;
	}

	@Override
	public int getMaxSizeX() throws Exception {

		return 0;
	}

	@Override
	public void setMaxSizeX(int maxsizex) throws Exception {

	}

	@Override
	public double getSetXHOPR() throws Exception {

		return 0;
	}

	@Override
	public void setSetXHOPR(double setxhopr) throws Exception {

	}

	@Override
	public int getMaxSizeY() throws Exception {

		return 0;
	}

	@Override
	public void setMaxSizeY(int maxsizey) throws Exception {

	}

	@Override
	public double getSetYHOPR() throws Exception {

		return 0;
	}

	@Override
	public void setSetYHOPR(double setyhopr) throws Exception {

	}

	@Override
	public void reset() throws Exception {
	}

	@Override
	public Observable<String> createComputeHistogramObservable() throws Exception {
		return null;
	}

	@Override
	public Observable<String> createComputeStatisticsObservable() throws Exception {
		return null;
	}

	@Override
	public Observable<Double> createMinObservable() throws Exception {
		return null;
	}

	@Override
	public Observable<Double> createMaxObservable() throws Exception {
		return null;
	}

	@Override
	public Observable<Double> createMeanObservable() throws Exception {
		return null;
	}

	@Override
	public Observable<Double> createTotalObservable() throws Exception {
		return null;
	}

	@Override
	public Observable<Double> createSigmaObservable() throws Exception {
		return null;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	public double getMeanValue() {
		return meanValue;
	}

	public void setMeanValue(double meanValue) {
		this.meanValue = meanValue;
	}

	public double getTotal() {
		return total;
	}

	public void setTotal(double total) {
		this.total = total;
	}

	public double getNet() {
		return net;
	}

	public void setNet(double net) {
		this.net = net;
	}

	public double getSigma() {
		return sigma;
	}

	public void setSigma(double sigma) {
		this.sigma = sigma;
	}

}
