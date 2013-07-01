/*
 * Copyright (C) 2013 Shane Stahlheber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2le.core;

/**
 * The estimate class which holds information pertaining to the estimates.
 * @author Shane Stahlheber
 *
 */
public class Estimate implements Comparable<Estimate> {
    private int x;
    private int y;
    private int slice;
    private double signal;
    
    private double eccentricity;
    private double major;
    private double minor;
    
    private double thirdmomentsum;
    private double thirdmomentdiff;
    
    private double[] hu = new double[8];
    
    private double estrx;
    private double estry;
    private double estIx;
    private double estIy;
    private double estbx;
    private double estby;
    private double estwx;
    private double estwy;

    private boolean rejected;
    
    private boolean eos;
    
    private boolean hasDistance;
    private double centerDistance;
    
    /**
     * Constructor; End of Stream.  Tells an iterator to stop.
     */
    public Estimate() {
        this.hasDistance = false;
        this.eos = true;
        this.slice = Integer.MAX_VALUE;
    }
    
    /**
     * New estimate; constructor.
     * @param x the x-th column
     * @param y the y-th row
     * @param slice the image slice
     * @param signal the intensity of the pixel at (x,y)
     */
    public Estimate(int x, int y, int slice, double signal) {
        this.hasDistance = false;
        this.eos = false;
        this.x = x;
        this.y = y; 
        this.slice = slice;
        this.signal = signal;
        this.rejected = false;
    }
    
    /**
     * Checks if the estimate indicates the end of the queue.
     * @return tells the iterator to stop iterating.
     */
    public boolean isEndOfQueue() {
        return eos;
    }
    
    /**
     * Computes the distance from the estimated center to the actual region center.
     * @return the distance from the estimate to the region center.
     */
    public double getDistanceFromCenter() {
        // calculate the distance to center if not already done
        if (!hasDistance) {
            final double dx = estrx - (0.5+x);
            final double dy = estry - (0.5+y);
            centerDistance = Math.sqrt(dx*dx + dy*dy);
            hasDistance = true;
        }
        
        return centerDistance;
    }
    
    /**
     * @return the column of the center pixel.
     */
    public int getColumn() {
        return x;
    }

    /**
     * @return the row of the center pixel.
     */
    public int getRow() {
        return y;
    }

    /**
     * @return the slice where the estimate is.
     */
    public int getSliceIndex() {
        return slice;
    }

    /**
     * @return the signal at the center pixel.
     */
    public double getSignal() {
        return signal;
    }

    /**
     * @return the eccentricity of the region.
     */
    public double getEccentricity() {
        return eccentricity;
    }
    
    /**
     * @return the major-axis of the "elliptical" region.
     */
    public double getMajorAxis() {
        return major;
    }
    
    /**
     * @return the minor-axis of the "elliptical" region.
     */
    public double getMinorAxis() {
        return minor;
    }
    
    /**
     * Make rejected.
     */
    public void markRejected() {
        this.rejected = true;
    }
    
    /**
     * Forgive.
     */
    public void unmarkRejected() {
        this.rejected = false;
    }
    
    /**
     * Check if passed.
     * @return true if passed; false otherwise.
     */
    public boolean hasPassed() {
        return !rejected;
    }
    
    /**
     * Sets the eccentricity found of the estimate.
     * @param eccentricity the eccentricity of the region.
     */
    public void setEccentricity(double eccentricity) {
        this.eccentricity = eccentricity;
    }
    
    /**
     * Sets the major- and minor-axis of the "elliptical" region.
     * @param major the major-axis.
     * @param minor the minor-axis.
     */
    public void setAxis(double major, double minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * @return the x-coordinate of the estimated molecule position.
     */
    public double getX() {
        return estrx;
    }

    /**
     * @param estrx the x-coordinate of the estimated molecule position.
     */
    public void setX(double estrx) {
        this.estrx = estrx;
    }

    /**
     * @return the y-coordinate of the estimated molecule position.
     */
    public double getY() {
        return estry;
    }

    /**
     * @param estry the y-coordinate of the estimated molecule position.
     */
    public void setY(double estry) {
        this.estry = estry;
    }

    public double getIntensityEstimateX() {
        return estIx;
    }

    public void setIntensityEstimateX(double estIx) {
        this.estIx = estIx;
    }

    public double getIntensityEstimateY() {
        return estIy;
    }

    public void setIntensityEstimateY(double estIy) {
        this.estIy = estIy;
    }

    public double getBackgroundEstimateX() {
        return estbx;
    }

    public void setBackgroundEstimateX(double estbx) {
        this.estbx = estbx;
    }

    public double getBackgroundEstimateY() {
        return estby;
    }

    public void setBackgroundEstimateY(double estby) {
        this.estby = estby;
    }

    public double getWidthEstimateX() {
        return estwx;
    }

    public void setWidthEstimateX(double estwx) {
        this.estwx = estwx;
    }

    public double getWidthEstimateY() {
        return estwy;
    }

    public void setWidthEstimateY(double estwy) {
        this.estwy = estwy;
    }

    @Override
    public int compareTo(Estimate o) {
        if (this.getSliceIndex() == o.getSliceIndex())
            return 0;
        else if (this.getSliceIndex() > o.getSliceIndex())
            return 1;
        else
            return -1;
    }

    public double getThirdMomentSum() {
        return thirdmomentsum;
    }

    public void setThirdMomentSum(double thirdmomentsum) {
        this.thirdmomentsum = thirdmomentsum;
    }

    public double getThirdMomentDiff() {
        return thirdmomentdiff;
    }

    public void setThirdMomentDiff(double thirdmomentdiff) {
        this.thirdmomentdiff = thirdmomentdiff;
    }
    
    public void setHu(int index, double value) {
        hu[index] = value;
    }
    
    public double getHu(int index) {
        return hu[index];
    }
}
