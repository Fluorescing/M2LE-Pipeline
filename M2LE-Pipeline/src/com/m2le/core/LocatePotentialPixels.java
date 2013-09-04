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

import ij.IJ;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// TODO: Auto-generated Javadoc
/**
 * The Class LocatePotentialPixels.
 *
 * @author Shane Stahlheber
 */
public final class LocatePotentialPixels {
    
    /**
     * Instantiates a new locate potential pixels.
     */
    private LocatePotentialPixels() { }
    
    /**
     * Find potential pixels.
     *
     * @param stack the stack
     * @return the list
     */
    public static List<BlockingQueue<Estimate>> findPotentialPixels(StackContext stack) {
        
        final int threads = ThreadHelper.getProcessorCount();
        
        final List<BlockingQueue<Estimate>> pixels = new ArrayList<BlockingQueue<Estimate>>(threads);
        
        for (int i = 0; i < threads; i++) {
            pixels.add(i, new LinkedBlockingQueue<Estimate>());
        }
        
        final JobContext job = stack.getJobContext();
        
        final double snCutoff = job.getNumericValue(UserSettings.SN_RATIO);
        
        // for all slices in the stack
        final int COUNT = stack.getSize();
        
        for (int slice = 1; slice <= COUNT; slice++) {
            
            // get the image processor for this slice
            final ImageProcessor ip = stack.getImageProcessor(slice);
            
            // get the pixel scaling
            int saturation = 65535;
            if (ip instanceof ByteProcessor)
                saturation = 255;
            final double scale = saturation / job.getNumericValue(UserSettings.SATURATION);
            
            // estimate noise
            final NoiseEstimator noise = new NoiseEstimator(stack, ip, scale);
            
            // for all pixels in the image
            final int W = ip.getWidth();
            final int H = ip.getHeight();
            
            for (int x = 3; x < W-3; x++) {
                for (int y = 3; y < H-3; y++) {
                    
                    // store potential pixels in the queue
                    final double S = ip.get(x, y) / scale;
                    
                    if (S > noise.getNoiseEstimate(x, y)*snCutoff) {
                        try {
                            final int i = Math.min(threads*slice/COUNT, threads-1);
                            pixels.get(i).put(new Estimate(x, y, slice, S));
                        } catch (InterruptedException e) {
                            IJ.handleException(e);
                        }
                    }
                }
            }
        }
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(pixels);
        
        return pixels;
    }
}
