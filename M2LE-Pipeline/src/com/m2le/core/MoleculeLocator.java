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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

// TODO: Auto-generated Javadoc
/**
 * The Class MoleculeLocator.  This class contains static procedures for 
 * localizing single molecules.
 */
public final class MoleculeLocator {
    
    private MoleculeLocator() { }
    
    /**
     * The Class LocatorThread.
     */
    public static class LocatorThread implements Runnable {
        
        private StackContext stack;
        
        private BlockingQueue<Estimate> pixels;
        
        private BlockingQueue<Estimate> estimates;
        
        /**
         * Instantiates a new locator thread.
         *
         * @param stack the stack
         * @param pixels the pixels
         * @param estimates the estimates
         */
        public LocatorThread(final StackContext stack, final BlockingQueue<Estimate> pixels, final BlockingQueue<Estimate> estimates) {
            this.stack = stack;
            this.pixels = pixels;
            this.estimates = estimates;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            
            // check all potential pixels
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate estimate = pixels.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // process the pixel
                    findMaxLikelihood(stack, estimate);
                    
                    // put it back if it survived
                    if (estimate.hasPassed())
                        estimates.put(estimate);
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    /**
     * Find subset.
     *
     * @param stack the stack
     * @param pixels the pixels
     * @return the list
     */
    public static List<BlockingQueue<Estimate>> findSubset(
            final StackContext stack, 
            final List<BlockingQueue<Estimate>> pixels) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final List<BlockingQueue<Estimate>> estimates = new ArrayList<BlockingQueue<Estimate>>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            estimates.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new LocatorThread(stack, pixels.get(n), estimates.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(estimates);
        
        return estimates;
    }
    
    /**
     * Do iteration.
     *
     * @param stack the image stack
     * @param signal the signal array
     * @param parameters the parameters
     * @param delta the change in parameter values
     * @param likelihood the likelihood
     * @param length the length of the array
     * @param wavenumber the wavenumber of light used
     * @param pixelsize the size of the pixel
     * @param usablepixel the fraction of usable pixel
     * @param initialnoise the initial noise
     * @return true if successful; false otherwise
     */
    private static boolean doIteration(
            final StackContext stack,
            final SignalArray signal,
            final Parameters parameters,
            Parameters delta,
            final double[] likelihood,
            final double length, 
            final double wavenumber, 
            final double pixelsize,
            final double usablepixel,
            final double initialnoise) {
        
        // get the job settings
        final JobContext job = stack.getJobContext();
        
        final double posthreshold = job.getNumericValue(UserSettings.ML_POS_EPSILON);
        final double intthreshold = job.getNumericValue(UserSettings.ML_INT_EPSILON)/100.0;
        final double widthreshold = job.getNumericValue(UserSettings.ML_WID_EPSILON);
        
        final double minNoiseBound = job.getNumericValue(UserSettings.ML_MIN_NOISE);
        final double maxNoiseMulti = job.getNumericValue(UserSettings.ML_MAX_NOISE);
        
        // compute the Newton-Raphson parameter change
        delta = GaussianModel.computeNewtonRaphson(signal, parameters, delta,
                                                   length, wavenumber, 
                                                   pixelsize, usablepixel);
    
        // coefficient for decaying update
        double coefficient = 1.0;
        
        final Parameters newparameters = new Parameters();
        
        // loop an arbitrary 10 times (will hopefully only iterate once)
        // This loop will continue as long as we have a smaller likelihood.
        // This assumes (with certainty) that the Newton-Raphson method overshoots.
        for (int k = 0; k < 10; k++) {
            
            // update the new parameters
            newparameters.update(parameters, delta, coefficient);
            
            // ensure that the parameters are within bounds
            if (newparameters.background < minNoiseBound) {
                newparameters.background = minNoiseBound;
            } else if (newparameters.background > maxNoiseMulti*initialnoise) {
                newparameters.background = maxNoiseMulti*initialnoise;
            }
            
            // find the new log-likelihood
            final double newlikelihood = 
                    GaussianModel.computeLogLikelihood(signal, newparameters, 
                                                       length, wavenumber, 
                                                       pixelsize, usablepixel);
        
            // end the loop if the likelihood increases; otherwise, halve coefficient
            if (newlikelihood > likelihood[0]) {
                
                // Oh good! Lets stop.
                likelihood[0] = newlikelihood;
                break;
            } else {
                
                // The likelihood decreased! Weird...  Try again.
                coefficient /= 2.0;
            }
        }
        
        // check end-conditions
        boolean isDone = false;
        
        if (((delta.position < 0) ? -delta.position : delta.position) < posthreshold)
            isDone = true;
        else if (StaticMath.percentDifference(parameters.intensity, newparameters.intensity) < intthreshold)
            isDone = true;
        else if (((delta.width < 0)? -delta.width : delta.width) < widthreshold)
            isDone = true;
       
        // update the final parameters
        if (!isDone) parameters.set(newparameters);
        
        return isDone;
    }
    
    /**
     * Find max likelihood.
     *
     * @param stack the stack
     * @param estimate the estimate
     * @return the estimate
     */
    public static Estimate findMaxLikelihood(
            final StackContext stack, 
            final Estimate estimate) {
        
        final JobContext job     = stack.getJobContext();
        final ImageProcessor ip  = stack.getImageProcessor(estimate.getSliceIndex());
        
        // preferences and constants
        final double wavenumber = 2.0*Math.PI*job.getNumericValue(UserSettings.N_APERTURE)/job.getNumericValue(UserSettings.WAVELENGTH);
        final double pixelsize = job.getNumericValue(UserSettings.PIXEL_SIZE);
        final double usablepixel = job.getNumericValue(UserSettings.USABLE_PIXEL)/100.0;
        
        final int maxIter = (int) job.getNumericValue(UserSettings.ML_MAX_ITERATIONS);
        final double minWidth = job.getNumericValue(UserSettings.ML_MIN_WIDTH);
        final double maxWidth = job.getNumericValue(UserSettings.ML_MAX_WIDTH);
        final boolean fixWidth = job.getCheckboxValue(UserSettings.ML_FIX_WIDTH);
        
        // get the pixel scaling
        int saturation = 65535;
        if (ip instanceof ByteProcessor)
            saturation = 255;
        final double scale = saturation / job.getNumericValue(UserSettings.SATURATION);
        
        // center/focus point
        final int cx = estimate.getColumn();
        final int cy = estimate.getRow();
        
        // set window size
        final int left   = Math.max(0, cx - 3);
        final int right  = Math.min(ip.getWidth(), cx + 4);
        final int top    = Math.max(0, cy - 3);
        final int bottom = Math.min(ip.getHeight(), cy + 4);
        
        final int width = right - left;
        final int height = bottom - top;
        
        // check for sufficient size
        if (width < 4 || height < 4) {
            estimate.markRejected();
            return estimate;
        }
        
        // flatten region into two arrays
        final SignalArray xsignal = new SignalArray(width, pixelsize);
        final SignalArray ysignal = new SignalArray(height, pixelsize);
        
        // accumulate pixel signal
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                final double S = ip.get(x, y) / scale;
                xsignal.accumulate(x-left, S);
                ysignal.accumulate(y-top, S);
            }
        }
                
        // get initial estimates
        final Parameters xparam = new Parameters(stack, estimate, xsignal, 
                                           height, wavenumber, 
                                           pixelsize, usablepixel, fixWidth);
        
        final Parameters yparam = new Parameters(stack, estimate, ysignal, 
                                           width, wavenumber, 
                                           pixelsize, usablepixel, fixWidth);
        
        // estimate the initial noise level
        final double initialnoise = (xparam.background + yparam.background)/2.;
        
        // initial likelihood calculations
        final double[] xlikelihood = {
                GaussianModel.computeLogLikelihood(xsignal, xparam, 
                                                   height, wavenumber, 
                                                   pixelsize, usablepixel)};
        
        final double[] ylikelihood = {
                GaussianModel.computeLogLikelihood(ysignal, yparam, 
                                                   width, wavenumber, 
                                                   pixelsize, usablepixel)};
        
        // used for the change in parameters
        final Parameters delta = new Parameters();
        
        // condition flags
        boolean xdone = false;
        boolean ydone = false;
        
        // update parameters
        for (int iter = 0; iter < maxIter; iter++) {
            
            // do iteration until done
            if (!xdone) {
                xdone = doIteration(stack, xsignal,
                                    xparam, delta, xlikelihood,
                                    height, wavenumber, 
                                    pixelsize, usablepixel, 
                                    initialnoise);
            }
            
            // do iteration until done
            if (!ydone) {
                ydone = doIteration(stack, ysignal,
                                    yparam, delta, ylikelihood,
                                    width, wavenumber, 
                                    pixelsize, usablepixel, 
                                    initialnoise);
            }
            
            // end the loop when both are done
            if (xdone && ydone) {
                break;
            }
        }
        
        // check for invalid parameters (to reject)
        if (!xparam.isValid() || !yparam.isValid()) {
            estimate.markRejected();
            return estimate;
        }
        
        // check that the position estimate is within the region
        if (xparam.position < 0. || xparam.position > pixelsize*width ||
                yparam.position < 0. || yparam.position > pixelsize*height) {
            estimate.markRejected();
            return estimate;
        }
        
        // check that the width of the PSF is within acceptable bounds
        if (xparam.width < minWidth || xparam.width > maxWidth
                || yparam.width < minWidth || yparam.width > maxWidth) {
            estimate.markRejected();
            return estimate;
        }
        
        // record information
        estimate.setX(xparam.position/pixelsize + left); 
        estimate.setY(yparam.position/pixelsize + top);
        estimate.setIntensityEstimateX(xparam.intensity);
        estimate.setIntensityEstimateY(yparam.intensity);
        estimate.setBackgroundEstimateX(xparam.background);
        estimate.setBackgroundEstimateY(yparam.background);
        estimate.setWidthEstimateX(xparam.width);
        estimate.setWidthEstimateY(yparam.width);
        
        return estimate;
    }
}
