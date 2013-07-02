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

import java.util.List;
import java.util.concurrent.BlockingQueue;

import ij.IJ;

/**
 * The Class ThreadHelper.
 */
public final class ThreadHelper {
    
    private ThreadHelper() { }
    
    /**
     * Gets the number of processor cores available.
     *
     * @return the processor count
     */
    public static int getProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Mark the end of queue.
     *
     * @param estimates the estimates
     */
    public static void markEndOfQueue(List<BlockingQueue<Estimate>> estimates) {
        // mark the end of the queue
        int numCPU = ThreadHelper.getProcessorCount();
        
        for (int n = 0; n < numCPU; n++)
            estimates.get(n).add(new Estimate());
    }
    
    /**
     * Mark the end of queue (single-thread).
     *
     * @param estimates the estimates
     */
    public static void markEndOfQueueSingle(BlockingQueue<Estimate> estimates) {
        estimates.add(new Estimate());
    }
    
    /**
     * Start threads.
     *
     * @param threads the threads
     */
    public static void startThreads(final Thread[] threads) {
        
        // start the threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // join the threads (waits for them to finish)
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            IJ.handleException(e);
        }
    }
}
