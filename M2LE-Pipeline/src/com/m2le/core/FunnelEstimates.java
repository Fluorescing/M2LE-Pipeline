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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// TODO: Auto-generated Javadoc
/**
 * The Class FunnelEstimates.
 *
 * @author Shane Stahlheber
 */
public final class FunnelEstimates {
    
    /**
     * Instantiates a new funnel estimates.
     */
    private FunnelEstimates() { }
    
    /**
     * The Class FunnelThread.
     *
     * @author Shane Stahlheber
     */
    public static class FunnelThread implements Runnable {
        
        /** The estimates. */
        private BlockingQueue<Estimate> estimates;
        
        /** The funnelled. */
        private BlockingQueue<Estimate> funnelled;
        
        /**
         * Instantiates a new funnel thread.
         *
         * @param estimates the estimates
         * @param funnelled the funnelled
         */
        public FunnelThread(final BlockingQueue<Estimate> estimates, final BlockingQueue<Estimate> funnelled) {
            this.estimates = estimates;
            this.funnelled = funnelled;
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
                    final Estimate estimate = estimates.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // put it back
                    funnelled.put(estimate);
                    
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
     * @param estimates the estimates
     * @return the blocking queue
     */
    public static BlockingQueue<Estimate> findSubset(final StackContext stack, final List<BlockingQueue<Estimate>> estimates) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final BlockingQueue<Estimate> funnelled = new LinkedBlockingQueue<>();
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new FunnelThread(estimates.get(n), funnelled);
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueueSingle(funnelled);
        
        return funnelled;
    }
}
