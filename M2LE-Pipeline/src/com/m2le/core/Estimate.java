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
    
    /** The x. */
    private int column;
    
    /** The y. */
    private int row;
    
    /** The slice. */
    private int slice;

    /** The eccentricity. */
    private double eccentricity;
    
    /** The major. */
    private double major;
    
    /** The minor. */
    private double minor;
    
    /** The thirdMomentSum. */
    private double thirdMomentSum;
    
    /** The thirdMomentDiff. */
    private double thirdMomentDiff;
    
    /** The x estimate. */
    private double x;
    
    /** The y estimate. */
    private double y;
    
    /** The horizontal intensity estimate. */
    private double horizontalIntensity;
    
    /** The vertical intensity estimate. */
    private double verticalIntensity;
    
    /** The horizontal background estimate. */
    private double horizontalBackground;
    
    /** The vertical background estimate. */
    private double verticalBackground;
    
    /** The width estimate. */
    private double width;
    
    /** The height estimate. */
    private double height;

    /** The rejected. */
    private boolean rejected;
    
    /** The isQueueTerminator. */
    private boolean isQueueTerminator;
    
    /** The has distance. */
    private boolean hasDistance;
    
    /** The center distance. */
    private double centerDistance;
    
    /**
     * Constructor; End of Stream.  Tells an iterator to stop.
     */
    public Estimate() {
        this.hasDistance = false;
        this.isQueueTerminator = true;
        this.slice = Integer.MAX_VALUE;
    }
    
    /**
     * New estimate; constructor.
     * @param column the column
     * @param row the row
     * @param slice the image slice
     */
    public Estimate(int column, int row, int slice) {
        this.hasDistance = false;
        this.isQueueTerminator = false;
        this.column = column;
        this.row = row;
        this.slice = slice;
        this.rejected = false;
    }
    
    /**
     * Checks if the estimate indicates the end of the queue.
     * @return tells the iterator to stop iterating.
     */
    public boolean isEndOfQueue() {
        return isQueueTerminator;
    }
    
    /**
     * Computes the distance from the estimated center to the actual region center.
     * @return the distance from the estimate to the region center.
     */
    public double getDistanceFromCenter() {
        // calculate the distance to center if not already done
        if (!hasDistance) {
            final double dx = x - (0.5+column);
            final double dy = y - (0.5+row);
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
        return column;
    }

    /**
     * Gets the row.
     *
     * @return the row of the center pixel.
     */
    public int getRow() {
        return row;
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
        return x;
    }

    /**
     * Sets the x.
     *
     * @param x the x-coordinate of the estimated molecule position.
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Gets the y.
     *
     * @return the y-coordinate of the estimated molecule position.
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the y.
     *
     * @param y the y-coordinate of the estimated molecule position.
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Gets the  horizontal intensity estimate.
     *
     * @return the  horizontal intensity estimate
     */
    public double getHorizontalIntensity() {
        return horizontalIntensity;
    }

    /**
     * Sets the  horizontal intensity estimate.
     *
     * @param intensity the new horizontal intensity estimate
     */
    public void setHorizontalIntensity(double intensity) {
        this.horizontalIntensity = intensity;
    }

    /**
     * Gets the vertical intensity estimate.
     *
     * @return the vertical intensity estimate
     */
    public double getVerticalIntensity() {
        return verticalIntensity;
    }

    /**
     * Sets the vertical intensity estimate.
     *
     * @param intensity the new vertical intensity estimate
     */
    public void setVerticalIntensity(double intensity) {
        this.verticalIntensity = intensity;
    }

    /**
     * Gets the horizontal background estimate.
     *
     * @return the horizontal background estimate
     */
    public double getHorizontalBackground() {
        return horizontalBackground;
    }

    /**
     * Sets the horizontal background estimate.
     *
     * @param background the new horizontal background estimate
     */
    public void setHorizontalBackground(double background) {
        this.horizontalBackground = background;
    }

    /**
     * Gets the vertical background estimate.
     *
     * @return the vertical background estimate
     */
    public double getVerticalBackground() {
        return verticalBackground;
    }

    /**
     * Sets the vertical background estimate.
     *
     * @param background the new vertical background estimate
     */
    public void setVerticalBackground(double background) {
        this.verticalBackground = background;
    }

    /**
     * Gets the width estimate.
     *
     * @return the width estimate
     */
    public double getWidth() {
        return width;
    }

    /**
     * Sets the width estimate.
     *
     * @param width the new width estimate
     */
    public void setWidth(double width) {
        this.width = width;
    }

    /**
     * Gets the height estimate.
     *
     * @return the height estimate
     */
    public double getHeight() {
        return height;
    }

    /**
     * Sets the height estimate.
     *
     * @param height the new height estimate
     */
    public void setHeight(double height) {
        this.height = height;
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
        return thirdMomentSum;
    }

    /**
     * Sets the third moment sum.
     *
     * @param thirdMomentSum the new third moment sum
     */
    public void setThirdMomentSum(double thirdMomentSum) {
        this.thirdMomentSum = thirdMomentSum;
    }

    /**
     * Gets the third moment diff.
     *
     * @return the third moment diff
     */
    public double getThirdMomentDiff() {
        return thirdMomentDiff;
    }

    /**
     * Sets the third moment diff.
     *
     * @param thirdMomentDiff the new third moment diff
     */
    public void setThirdMomentDiff(double thirdMomentDiff) {
        this.thirdMomentDiff = thirdMomentDiff;
    }
}
