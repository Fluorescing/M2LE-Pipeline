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

// TODO: Auto-generated Javadoc
/**
 * The estimate class which holds information pertaining to the estimates.
 * @author Shane Stahlheber
 *
 */
public class Estimate implements Comparable<Estimate> {
    
    /** The x. */
    private int x;
    
    /** The y. */
    private int y;
    
    /** The slice. */
    private int slice;
    
    /** The signal. */
    private double signal;
    
    /** The eccentricity. */
    private double eccentricity;
    
    /** The major. */
    private double major;
    
    /** The minor. */
    private double minor;
    
    /** The thirdmomentsum. */
    private double thirdmomentsum;
    
    /** The thirdmomentdiff. */
    private double thirdmomentdiff;
    
    /** The hu. */
    private double[] hu = new double[8];
    
    /** The estrx. */
    private double estrx;
    
    /** The estry. */
    private double estry;
    
    /** The est ix. */
    private double estIx;
    
    /** The est iy. */
    private double estIy;
    
    /** The estbx. */
    private double estbx;
    
    /** The estby. */
    private double estby;
    
    /** The estwx. */
    private double estwx;
    
    /** The estwy. */
    private double estwy;

    /** The rejected. */
    private boolean rejected;
    
    /** The eos. */
    private boolean eos;
    
    /** The has distance. */
    private boolean hasDistance;
    
    /** The center distance. */
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
     * Gets the column.
     *
     * @return the column of the center pixel.
     */
    public int getColumn() {
        return x;
    }

    /**
     * Gets the row.
     *
     * @return the row of the center pixel.
     */
    public int getRow() {
        return y;
    }

    /**
     * Gets the slice index.
     *
     * @return the slice where the estimate is.
     */
    public int getSliceIndex() {
        return slice;
    }

    /**
     * Gets the signal.
     *
     * @return the signal at the center pixel.
     */
    public double getSignal() {
        return signal;
    }

    /**
     * Gets the eccentricity.
     *
     * @return the eccentricity of the region.
     */
    public double getEccentricity() {
        return eccentricity;
    }
    
    /**
     * Gets the major axis.
     *
     * @return the major-axis of the "elliptical" region.
     */
    public double getMajorAxis() {
        return major;
    }
    
    /**
     * Gets the minor axis.
     *
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
     * Gets the x.
     *
     * @return the x-coordinate of the estimated molecule position.
     */
    public double getX() {
        return estrx;
    }

    /**
     * Sets the x.
     *
     * @param estrx the x-coordinate of the estimated molecule position.
     */
    public void setX(double estrx) {
        this.estrx = estrx;
    }

    /**
     * Gets the y.
     *
     * @return the y-coordinate of the estimated molecule position.
     */
    public double getY() {
        return estry;
    }

    /**
     * Sets the y.
     *
     * @param estry the y-coordinate of the estimated molecule position.
     */
    public void setY(double estry) {
        this.estry = estry;
    }

    /**
     * Gets the intensity estimate x.
     *
     * @return the intensity estimate x
     */
    public double getIntensityEstimateX() {
        return estIx;
    }

    /**
     * Sets the intensity estimate x.
     *
     * @param estIx the new intensity estimate x
     */
    public void setIntensityEstimateX(double estIx) {
        this.estIx = estIx;
    }

    /**
     * Gets the intensity estimate y.
     *
     * @return the intensity estimate y
     */
    public double getIntensityEstimateY() {
        return estIy;
    }

    /**
     * Sets the intensity estimate y.
     *
     * @param estIy the new intensity estimate y
     */
    public void setIntensityEstimateY(double estIy) {
        this.estIy = estIy;
    }

    /**
     * Gets the background estimate x.
     *
     * @return the background estimate x
     */
    public double getBackgroundEstimateX() {
        return estbx;
    }

    /**
     * Sets the background estimate x.
     *
     * @param estbx the new background estimate x
     */
    public void setBackgroundEstimateX(double estbx) {
        this.estbx = estbx;
    }

    /**
     * Gets the background estimate y.
     *
     * @return the background estimate y
     */
    public double getBackgroundEstimateY() {
        return estby;
    }

    /**
     * Sets the background estimate y.
     *
     * @param estby the new background estimate y
     */
    public void setBackgroundEstimateY(double estby) {
        this.estby = estby;
    }

    /**
     * Gets the width estimate x.
     *
     * @return the width estimate x
     */
    public double getWidthEstimateX() {
        return estwx;
    }

    /**
     * Sets the width estimate x.
     *
     * @param estwx the new width estimate x
     */
    public void setWidthEstimateX(double estwx) {
        this.estwx = estwx;
    }

    /**
     * Gets the width estimate y.
     *
     * @return the width estimate y
     */
    public double getWidthEstimateY() {
        return estwy;
    }

    /**
     * Sets the width estimate y.
     *
     * @param estwy the new width estimate y
     */
    public void setWidthEstimateY(double estwy) {
        this.estwy = estwy;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Estimate o) {
        if (this.getSliceIndex() == o.getSliceIndex())
            return 0;
        else if (this.getSliceIndex() > o.getSliceIndex())
            return 1;
        else
            return -1;
    }

    /**
     * Gets the third moment sum.
     *
     * @return the third moment sum
     */
    public double getThirdMomentSum() {
        return thirdmomentsum;
    }

    /**
     * Sets the third moment sum.
     *
     * @param thirdmomentsum the new third moment sum
     */
    public void setThirdMomentSum(double thirdmomentsum) {
        this.thirdmomentsum = thirdmomentsum;
    }

    /**
     * Gets the third moment diff.
     *
     * @return the third moment diff
     */
    public double getThirdMomentDiff() {
        return thirdmomentdiff;
    }

    /**
     * Sets the third moment diff.
     *
     * @param thirdmomentdiff the new third moment diff
     */
    public void setThirdMomentDiff(double thirdmomentdiff) {
        this.thirdmomentdiff = thirdmomentdiff;
    }
    
    /**
     * Sets the hu.
     *
     * @param index the index
     * @param value the value
     */
    public void setHu(int index, double value) {
        hu[index] = value;
    }
    
    /**
     * Gets the hu.
     *
     * @param index the index
     * @return the hu
     */
    public double getHu(int index) {
        return hu[index];
    }
}
