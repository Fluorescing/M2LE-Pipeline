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

import static com.m2le.core.UserSettings.SATURATION;
import static com.m2le.core.UserSettings.ECC_DISABLED;
import static com.m2le.core.UserSettings.ECC_THRESHOLD;

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Class to reject estimates based on eccentricity/ellipticity.
 * @author Shane Stahlheber
 *
 */
public final class EccentricityRejector {
    
    private EccentricityRejector() { }
    
    /**
     * A runnable class for multi-threaded rejection.
     * @author Shane Stahlheber
     *
     */
    public static class RejectionThread implements Runnable {
        
        private StackContext stack;
        private BlockingQueue<Estimate> initialEstimates;
        private BlockingQueue<Estimate> circularRegions;
        
        /**
         * Constructor
         * @param stack the stack of images
         * @param initialEstimates the initial estimates; interesting initialEstimates
         * @param circularRegions estimates which pass the eccentricity test
         */
        public RejectionThread(final StackContext stack,
                               final BlockingQueue<Estimate> initialEstimates,
                               final BlockingQueue<Estimate> circularRegions) {
            this.stack = stack;
            this.initialEstimates = initialEstimates;
            this.circularRegions = circularRegions;
        }

        @Override
        public void run() {
            
            // check if eccentricity-based rejection is disabled; still run
            final boolean disabled = stack.getJobContext().getCheckboxValue(ECC_DISABLED);
            
            // check all potential initialEstimates
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate pixel = initialEstimates.take();
                    
                    // check for the end of the queue
                    if (pixel.isEndOfQueue()) {
                        break;
                    }
                    
                    // process the pixel
                    updatePixel(stack, pixel);
                    
                    // put it back if it survived
                    if (pixel.hasPassed() || disabled) {
                        pixel.unmarkRejected();
                        circularRegions.put(pixel);
                    }
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    /**
     * 
     * @param stack the stack of images
     * @param initialEstimates a list of segmented estimates (interesting initialEstimates)
     * @return a list of segmented final estimates.
     */
    public static List<BlockingQueue<Estimate>> findSubset(final StackContext stack,
                                                           final List<BlockingQueue<Estimate>> initialEstimates) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final List<BlockingQueue<Estimate>> circularRegions = new ArrayList<BlockingQueue<Estimate>>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            circularRegions.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new RejectionThread(stack, initialEstimates.get(n), circularRegions.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(circularRegions);
        
        return circularRegions;
    }
    
    private static void updatePixel(final StackContext stack, final Estimate initialEstimates) {
    
        final ImageProcessor ip = stack.getImageProcessor(initialEstimates.getSliceIndex());
        final JobContext job = stack.getJobContext();
        
        // get the window dimensions
        final int x = initialEstimates.getColumn();
        final int y = initialEstimates.getRow();
        
        // get the pixel scaling
        int saturation = 65535;
        if (ip instanceof ByteProcessor) {
            saturation = 255;
        }
        final double scale = saturation / job.getNumericValue(SATURATION);
        
        // prevent out-of-bounds errors
        final int left     = Math.max(0, x - 3);
        final int right    = Math.min(ip.getWidth(), x + 4);
        final int top      = Math.max(0, y - 3);
        final int bottom   = Math.min(ip.getHeight(), y + 4);
        
        final double noise =
                StaticMath.estimateNoise(ip, stack, initialEstimates, scale);
        final double acceptance =
                job.getNumericValue(ECC_THRESHOLD);
        final double intensity =
                StaticMath.estimatePhotonCount(ip, left, right, top, bottom, noise, scale);
        final double threshold =
                StaticMath.calculateThreshold(intensity, acceptance * 100.0);
        
        // compute eigenvalues
        final double[] centroid =
                StaticMath.estimateCentroid(ip, left, right, top, bottom, noise, scale);
        final double[] moments =
                StaticMath.estimateSecondMoments(ip, centroid, left, right, top, bottom, noise, scale);
        final double[] eigenValues =
                StaticMath.findEigenValues(moments);
       
        final double eccentricity = Math.sqrt(1.0 - eigenValues[1] / eigenValues[0]);
        
        // save major/minor axis and eccentricity
        initialEstimates.setEccentricity(eccentricity);
        initialEstimates.setAxis(Math.sqrt(eigenValues[0]), Math.sqrt(eigenValues[1]));
        
        // check if the pixel should be rejected
        if (eccentricity >= threshold) {
            initialEstimates.markRejected();
        }
    }
}
