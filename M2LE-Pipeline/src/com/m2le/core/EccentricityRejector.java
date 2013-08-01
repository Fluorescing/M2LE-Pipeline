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

/**
 * Class to reject estimates based on eccentricity/ellipticity.
 * @author Shane Stahlheber
 *
 */
public final class EccentricityRejector {
    
    private EccentricityRejector() { };
    
    /**
     * A runnable class for multi-threaded rejection.
     * @author Shane Stahlheber
     *
     */
    public static class RejectionThread implements Runnable {
        
        private StackContext stack;
        private BlockingQueue<Estimate> pixels;
        private BlockingQueue<Estimate> eccpixels;
        
        /**
         * Constructor
         * @param stack the stack of images
         * @param pixels the initial estimates; interesting pixels
         * @param eccpixels estimates which pass the eccentricity test
         */
        public RejectionThread(final StackContext stack, final BlockingQueue<Estimate> pixels, final BlockingQueue<Estimate> eccpixels) {
            this.stack = stack;
            this.pixels = pixels;
            this.eccpixels = eccpixels;
        }

        @Override
        public void run() {
            
            // check if eccentricity-based rejection is disabled; still run
            final boolean disabled = stack.getJobContext().getCheckboxValue(UserParams.ECC_DISABLED);
            
            // check all potential pixels
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate pixel = pixels.take();
                    
                    // check for the end of the queue
                    if (pixel.isEndOfQueue())
                        break;
                    
                    // process the pixel
                    updatePixel(stack, pixel);
                    
                    // put it back if it survived
                    if (pixel.hasPassed() || disabled) {
                        pixel.unmarkRejected();
                        eccpixels.put(pixel);
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
     * @param pixels a list of segmented estimates (interesting pixels)
     * @return a list of segmented final estimates.
     */
    public static List<BlockingQueue<Estimate>> findSubset(final StackContext stack, final List<BlockingQueue<Estimate>> pixels) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final List<BlockingQueue<Estimate>> eccpixels = new ArrayList<BlockingQueue<Estimate>>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            eccpixels.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new RejectionThread(stack, pixels.get(n), eccpixels.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(eccpixels);
        
        return eccpixels;
    }
    
    private static void updatePixel(final StackContext stack, final Estimate pixel) {
    
        final ImageProcessor ip = stack.getImageProcessor(pixel.getSliceIndex());
        final JobContext job = stack.getJobContext();
        
        // get the window dimensions
        final int x = pixel.getColumn();
        final int y = pixel.getRow();
        
        // get the pixel scaling
        int saturation = 65535;
        if (ip instanceof ByteProcessor)
            saturation = 255;
        final double scale = saturation / job.getNumericValue(UserParams.SATURATION);
        
        // prevent out-of-bounds errors
        final int left     = Math.max(0, x - 3);
        final int right    = Math.min(ip.getWidth(), x + 4);
        final int top      = Math.max(0, y - 3);
        final int bottom   = Math.min(ip.getHeight(), y + 4);
        
        final double noise = StaticMath.estimateNoise(ip, stack, pixel, scale);
        final double acceptance = job.getNumericValue(UserParams.ECC_THRESHOLD);
        final double intensity = StaticMath.estimatePhotonCount(ip, left, right, top, bottom, noise, scale);
        final double threshold = StaticMath.calculateThreshold(intensity, acceptance*100.0);
        
        // compute eigenvalues
        final double[] centroid = StaticMath.estimateCentroid(ip, left, right, top, bottom, noise, scale);
        final double[] moments  = StaticMath.estimateSecondMoments(ip, centroid, left, right, top, bottom, noise, scale);
        final double[] eigen    = StaticMath.findEigenValues(moments);
       
        final double eccentricity = Math.sqrt(1.0 - eigen[1]/eigen[0]);
        
        // save major/minor axis and eccentricity
        pixel.setEccentricity(eccentricity);
        pixel.setAxis(Math.sqrt(eigen[0]), Math.sqrt(eigen[1]));
        
        // check if the pixel should be rejected
        if (eccentricity >= threshold) 
            pixel.markRejected();
    }
}
