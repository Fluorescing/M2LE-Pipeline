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
 * The column-/row-summed array of the region of interest.
 * @author Shane Stahlheber
 *
 */
public class SignalArray {
    
    public double[] mSignal;
    private final double[] mPosition;
    
    /**
     * Constructor.
     * @param size the number of elements in the array.
     * @param interval the length (interval) of the array.
     */
    public SignalArray(final int size, final double interval) {
        mSignal = new double[size];
        mPosition = new double[size];
        
        double position = interval/2.;
        for (int i = 0; i < size; i++) {
            mPosition[i] = position;
            position += interval;
        }
    }
    
    /**
     * Accumulate the {@value value} given to the array at {@value index}.
     * @param index the position in the array.
     * @param value the value to add.
     */
    public void accumulate(final int index, final double value) {
        mSignal[index] += value;
    }
    
    /**
     * Set the {@value value} given to the array at {@value index}.
     * @param index the position in the array.
     * @param value the value to set.
     */
    public void set(final int index, final double value) {
        mSignal[index] = value;
    }
    
    /**
     * Get the value given to the array at {@value index}.
     * @param index the position in the array.
     * @return the value of the array at the specified {@value index}.
     */
    public double get(final int index) {
        return mSignal[index];
    }
    
    /**
     * Get the physical position of the element in the array.
     * @param index the position in the array element.
     * @return the position of the element at the specified {@value index}.
     */
    public double getPosition(final int index) {
        return mPosition[index];
    }
    
    /**
     * @return the size of the array.
     */
    public int getSize() {
        return mSignal.length;
    }
}
