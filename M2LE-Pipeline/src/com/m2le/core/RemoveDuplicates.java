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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// TODO: Auto-generated Javadoc
/**
 * The Class RemoveDuplicates.
 *
 * @author Shane Stahlheber
 */
public final class RemoveDuplicates {
    
    private RemoveDuplicates() { }
    
    /**
     * The Class FirstPassThread.
     */
    public static class FirstPassThread implements Runnable {
        
        private StackContext stack;
        
        private BlockingQueue<Estimate> estimates;
        
        private BlockingQueue<Estimate> reduced;
        
        /**
         * Instantiates a new first-pass thread.
         *
         * @param stack the image stack
         * @param estimates the estimates
         * @param reduced the reduced estimates
         */
        public FirstPassThread(final StackContext stack, final BlockingQueue<Estimate> estimates, final BlockingQueue<Estimate> reduced) {
            this.stack = stack;
            this.estimates = estimates;
            this.reduced = reduced;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            
            final int W = stack.getWidth();
            final int H = stack.getHeight();
            
            int slice = 0;
            Estimate[][] grid = new Estimate[W][H];
            
            // check all potential pixels
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate estimate = estimates.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // check the current slice (reset grid if new slice)
                    if (estimate.getSliceIndex() != slice) {
                        slice = estimate.getSliceIndex();
                        for (int x = 0; x < W; x++) {
                            for (int y = 0; y < H; y++) {
                                grid[x][y] = null;
                            }
                        }
                    }
                    
                    // check for a conflict
                    Estimate compare = grid[estimate.getColumn()][estimate.getRow()];
                    if (compare != null) {
                        // choose the lesser of the two center displacements
                        if (compare.getDistanceFromCenter() < estimate.getDistanceFromCenter()) {
                            
                            // replace estimate
                            estimate.markRejected();
                            
                        } else {
                            
                            compare.markRejected();
                            
                            // clear grid
                            final int CW = Math.min(W, compare.getColumn() + 4);
                            final int CH = Math.min(H, compare.getRow() + 4);
                            for (int x = Math.max(0, compare.getColumn()-3); x < CW; x++) {
                                for (int y = Math.max(0, compare.getRow()-3); y < CH; y++) {
                                    if (grid[x][y] == compare)
                                        grid[x][y] = null;
                                }
                            }
                        }
                    }
                    
                    // put marker down
                    final int EW = Math.min(W, estimate.getColumn() + 4);
                    final int EH = Math.min(H, estimate.getRow() + 4);
                    for (int x = Math.max(0, estimate.getColumn()-3); x < EW; x++) {
                        for (int y = Math.max(0, estimate.getRow()-3); y < EH; y++) {
                            if (grid[x][y] == null || (grid[x][y] != null && estimate.getDistanceFromCenter() < grid[x][y].getDistanceFromCenter()))
                                grid[x][y] = estimate;
                        }
                    }
                    
                    // put it back if it survived
                    if (estimate.hasPassed())
                        reduced.put(estimate);
                    
                } catch (InterruptedException e) {
                    IJ.handleException(e);
                }
            }
        }      
    }
    
    /**
     * The Class SecondPassThread.
     */
    public static class SecondPassThread implements Runnable {
        
        private BlockingQueue<Estimate> reduced;
        
        private BlockingQueue<Estimate> finalreduced;
        
        /**
         * Instantiates a new second pass thread.
         *
         * @param reduced the reduced
         * @param finalreduced the final reduced estimates
         */
        public SecondPassThread(final BlockingQueue<Estimate> reduced, final BlockingQueue<Estimate> finalreduced) {
            this.reduced = reduced;
            this.finalreduced = finalreduced;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            
            // further reduce the estimates
            while (true) {
                try {
                    
                    // get pixel
                    final Estimate estimate = reduced.take();
                    
                    // check for the end of the queue
                    if (estimate.isEndOfQueue())
                        break;
                    
                    // put it back if it survived
                    if (estimate.hasPassed())
                        finalreduced.put(estimate);
                    
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
     * @return the list
     */
    public static List<BlockingQueue<Estimate>> findSubset(final StackContext stack, final List<BlockingQueue<Estimate>> estimates) {
        
        final int numCPU = ThreadHelper.getProcessorCount();
        final Thread[] threads = new Thread[numCPU];
        
        final List<BlockingQueue<Estimate>> reduced = new ArrayList<>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            reduced.add(i, new LinkedBlockingQueue<>());
        }
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new FirstPassThread(stack, estimates.get(n), reduced.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(reduced);
        
        final List<BlockingQueue<Estimate>> finalreduced = new ArrayList<>(numCPU);
        
        for (int i = 0; i < numCPU; i++) {
            finalreduced.add(i, new LinkedBlockingQueue<>());
        }
        
        for (int n = 0; n < numCPU; n++) {
            Runnable r = new SecondPassThread(reduced.get(n), finalreduced.get(n));
            threads[n] = new Thread(r);
        }
        
        // start the threads
        ThreadHelper.startThreads(threads);
        
        // mark the end of the queue
        ThreadHelper.markEndOfQueue(finalreduced);
        
        return finalreduced;
    }
}
