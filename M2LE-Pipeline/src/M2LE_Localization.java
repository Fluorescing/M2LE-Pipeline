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

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.m2le.core.EccentricityRejector;
import com.m2le.core.Estimate;
import com.m2le.core.FunnelEstimates;
import com.m2le.core.JobContext;
import com.m2le.core.LocatePotentialPixels;
import com.m2le.core.MoleculeLocator;
import com.m2le.core.RemoveDuplicates;
import com.m2le.core.StackContext;
import com.m2le.core.ThirdMomentRejector;
import com.m2le.core.UserParams;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow;

/**
 * The ImageJ plugin class from which the localization job begins.
 * 
 * @author Shane Stahlheber
 *
 */
public class M2LE_Localization implements PlugIn {

    /**
     * The starting point for the M2LE plugin for ImageJ.
     * @param arg the arguments given by ImageJ
     */
    @Override
    public void run(String arg) {
        // Get the job from the user
        final JobContext job = new JobContext();
        UserParams.getUserParameters(job);
        job.initialize();
        
        // check if user cancelled
        if (job.isCanceled()) return;
        
        final boolean debugMode = job.getCheckboxValue(UserParams.DEBUG_MODE);
        ResultsTable debugTable = null;
        
        final String debugTableTitle = job.getChoice(UserParams.DB_TABLE);
        if (!debugTableTitle.equals("")) {
            final TextPanel tp = ((TextWindow) WindowManager.getFrame(debugTableTitle)).getTextPanel();
            debugTable = (tp == null) ? null : tp.getResultsTable();
        }
    
        // load the image stack
        final StackContext stack = new StackContext(job);
        
        if (stack.loadFailed()) {
            IJ.showMessage("M2LE Warning", "No images to analyze!");
            return;
        }
        
        // iterate through all images in the stack
        final ResultsTable results = new ResultsTable();
        results.setPrecision(10);
        
        BlockingQueue<Estimate> funnelled = runPipeline(stack);
        
        int SIZE = processResults(job, stack, results, funnelled, debugTable, debugMode);
        
        // show the results
        results.show("Localization Results");
        
        IJ.showStatus(String.format("%d Localizations.", SIZE));
        IJ.log(String.format("[%d localizations]", SIZE));
    }

    /**
     * Process the estimates.
     * @param job job settings
     * @param stack stack of images
     * @param results the results table
     * @param estimates list of estimates
     * @param debugTable the debug table
     * @param debugMode is debug enabled or disabled
     * @return returns the number of estimates added to the table
     */
    private static int processResults(final JobContext job, final StackContext stack,
            final ResultsTable results, BlockingQueue<Estimate> estimates,
            ResultsTable debugTable, final boolean debugMode) {
        final double pixelSize = job.getNumericValue(UserParams.PIXEL_SIZE);
        
        // should we render an image?
        final boolean render = job.getCheckboxValue(UserParams.RENDER_ENABLED);
        final double rscale = job.getNumericValue(UserParams.RENDER_SCALE);
        
        final int rwidth = (int) (stack.getWidth()*rscale);
        final int rheight = (int) (stack.getHeight()*rscale);
        
        int[][] rendering = null;
        if (render)
            rendering = new int[rheight][rwidth];
        
        // add to results table
        int SIZE = estimates.size()-1;
        while (true) {
            try {
                // get pixel
                final Estimate estimate = estimates.take();
                
                // check for the end of the queue
                if (estimate.isEndOfQueue())
                    break;
                
                // accumulate image
                if (render && rendering != null) {
                    final int x = (int) (estimate.getXEstimate()*rscale);
                    final int y = (int) (estimate.getYEstimate()*rscale);
                    if (x >= 0 && x < rwidth && y >= 0 && y < rheight)
                        rendering[y][x] += 1;
                }
                
                addResult(results, estimate, pixelSize, debugTable, debugMode);
                
            } catch (InterruptedException e) {
                IJ.handleException(e);
            }
        }
        
        // display reconstruction
        if (render) displayReconstruction(rwidth, rheight, rendering);
        return SIZE;
    }

    /**
     * The actual pipeline which processes the images.
     * @param stack the stack of images
     * @return returns the list of estimates
     */
    private static BlockingQueue<Estimate> runPipeline(final StackContext stack) {
        IJ.showProgress(0, 100);
        IJ.showStatus("Locating Potential Molecules...");
        
        // find all potential pixels
        List<BlockingQueue<Estimate>> potential = LocatePotentialPixels.findPotentialPixels(stack);
        
        IJ.showProgress(25, 100);
        IJ.showStatus("Testing Eccentricity...");
        
        // find subset of potential pixels that pass an eccentricity test
        potential = EccentricityRejector.findSubset(stack, potential);
        
        IJ.showProgress(50, 100);
        IJ.showStatus("Localizing Molecules...");
        
        // transform the PE pixels into localization estimates
        List<BlockingQueue<Estimate>> estimates = MoleculeLocator.findSubset(stack, potential);
        
        IJ.showProgress(63, 100);
        IJ.showStatus("Testing Third Moments...");
        
        // find subset of potential pixels that pass the third moments test
        estimates = ThirdMomentRejector.findSubset(stack, estimates);
        
        IJ.showProgress(75, 100);
        IJ.showStatus("Removing Duplicates...");
        
        // weed out duplicates (choose the estimate carefully)
        estimates = RemoveDuplicates.findSubset(stack, estimates);
        
        IJ.showProgress(100, 100);
        IJ.showStatus("Printing Results...");
        BlockingQueue<Estimate> funnelled = FunnelEstimates.findSubset(stack, estimates);
        return funnelled;
    }

    /**
     * Displays the reconstructed image.
     * @param width
     * @param height
     * @param image the actual reconstructed image
     */
    private static void displayReconstruction(final int width, final int height, int[][] image) {
        final ImagePlus rimp = IJ.createImage("Sample Rendering", "16-bit", width, height, 1);
        final ImageProcessor rip = rimp.getProcessor();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rip.set(x, y, image[y][x]);
            }
        }
        rimp.show();
    }

    /**
     * Adds a result to the results table.
     * @param results the results table
     * @param estimate the final estimate
     * @param pixelSize the size of the pixel in 
     * @param debugTable the debug table (table created when the simulated images were created)
     * @param debugMode whether debug mode is on or off
     */
    private static void addResult(final ResultsTable results, final Estimate estimate,
            final double pixelSize, ResultsTable debugTable, final boolean debugMode) {
        
        // add result
        results.incrementCounter();
        results.addValue("Frame", estimate.getSlice());
        results.addValue("x (px)", estimate.getXEstimate());
        results.addValue("y (px)", estimate.getYEstimate());
        results.addValue("x (nm)", estimate.getXEstimate()*pixelSize);
        results.addValue("y (nm)", estimate.getYEstimate()*pixelSize);
        results.addValue("Intensity x", estimate.getIntensityEstimateX());
        results.addValue("Intensity y", estimate.getIntensityEstimateY());
        results.addValue("Background x", estimate.getBackgroundEstimateX());
        results.addValue("Background y", estimate.getBackgroundEstimateY());
        results.addValue("Width x", estimate.getWidthEstimateX());
        results.addValue("Width y", estimate.getWidthEstimateY());
        
        if (debugMode) {
            results.addValue("Minor Axis", estimate.getMinorAxis());
            results.addValue("Major Axis", estimate.getMajorAxis());
            results.addValue("ROI x", estimate.getX()+0.5);
            results.addValue("ROI y", estimate.getY()+0.5);
            
            results.addValue("thirdsum", estimate.getThirdMomentSum());
            results.addValue("thirddiff", estimate.getThirdMomentDiff());
            
            if (debugTable != null) {
                for (int column = 0; debugTable.columnExists(column); column++) {
                    final String name = "D_"+debugTable.getColumnHeading(column);
                    final double value = debugTable.getValueAsDouble(column, estimate.getSlice()-1);
                    results.addValue(name, value);
                }
            }
        }
    }

}
