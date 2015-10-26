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

import com.m2le.core.*;
import static com.m2le.core.UserSettings.DEBUG_MODE;
import static com.m2le.core.UserSettings.DB_TABLE;
import static com.m2le.core.UserSettings.PIXEL_SIZE;

import ij.IJ;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
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

        final JobContext job = new JobContext();
        final ResultsTable results = new ResultsTable();
        results.setPrecision(10);

        // load the image stack
        final StackContext stack = new StackContext(job);
        if (stack.stackFailed()) {
            IJ.showMessage("M2LE-Pipeline Warning", "No images to analyze!");
            return;
        }

        // Get the job from the user
        UserSettings.declareSettings(job);
        job.initialize();
        
        // check if user cancelled
        if (job.isCanceled()) {
            return;
        }
        
        final boolean debugMode = job.getCheckboxValue(DEBUG_MODE);
        ResultsTable debugTable = null;

        final String debugTableTitle = job.getComboboxChoice(DB_TABLE);
        if (!debugTableTitle.equals("")) {
            final TextPanel tp = ((TextWindow) WindowManager.getFrame(debugTableTitle)).getTextPanel();
            debugTable = (tp == null) ? null : tp.getResultsTable();
        }

        // iterate through all images in the stack
        BlockingQueue<Estimate> funnelled = runPipeline(stack);
        final int localizationCount = processResults(job, stack, results, funnelled, debugTable, debugMode);

        // display results
        if (localizationCount == 0) {
            IJ.log("M2LE-Pipeline: Could not locate any molecules; please review settings.");
            IJ.showStatus("Nothing found.");
        } else {
            IJ.log(String.format("M2LE-Pipeline: There were %d molecules located.", localizationCount));
            IJ.showStatus(String.format("%d Localizations.", localizationCount));
            results.show("Localization Results");
        }
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
    private static int processResults(final JobContext job,
                                      final StackContext stack,
                                      final ResultsTable results,
                                      BlockingQueue<Estimate> estimates,
                                      ResultsTable debugTable,
                                      final boolean debugMode) {

        final double pixelSize = job.getNumericValue(PIXEL_SIZE);
        final Reconstruction reconstruction = new Reconstruction(job, stack);

        int localizationCount = 0;

        // add to results table
        while (true) {
            try {
                // get pixel
                final Estimate estimate = estimates.take();
                
                // check for the end of the queue
                if (estimate.isEndOfQueue()) {
                    break;
                }
                
                // accumulate molecule to reconstruction
                reconstruction.accumulate(estimate.getX(), estimate.getY());

                // add the results to the table
                addResult(results, estimate, pixelSize, debugTable, debugMode);

                localizationCount++;
                
            } catch (InterruptedException e) {
                IJ.handleException(e);
                break;
            }
        }
        
        // display reconstruction
        reconstruction.display();

        return localizationCount;
    }

    /**
     * The actual pipeline which processes the images.
     * @param stack the stack of images
     * @return returns the list of estimates
     */
    private static BlockingQueue<Estimate> runPipeline(final StackContext stack) {
        IJ.showProgress(0, 100);
        IJ.showStatus("Locating Potential Molecules...");
        List<BlockingQueue<Estimate>> potential = LocatePotentialPixels.findPotentialPixels(stack);
        
        IJ.showProgress(25, 100);
        IJ.showStatus("Testing Eccentricity...");
        potential = EccentricityRejector.findSubset(stack, potential);
        
        IJ.showProgress(50, 100);
        IJ.showStatus("Localizing Molecules...");
        List<BlockingQueue<Estimate>> estimates = MoleculeLocator.findSubset(stack, potential);
        
        IJ.showProgress(63, 100);
        IJ.showStatus("Testing Third Moments...");
        estimates = ThirdMomentRejector.findSubset(stack, estimates);
        
        IJ.showProgress(75, 100);
        IJ.showStatus("Removing Duplicates...");
        estimates = RemoveDuplicates.findSubset(stack, estimates);
        
        IJ.showProgress(100, 100);
        IJ.showStatus("Printing Results...");
        return FunnelEstimates.findSubset(stack, estimates);
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
        results.addValue("Frame", estimate.getSliceIndex());
        results.addValue("x (px)", estimate.getX());
        results.addValue("y (px)", estimate.getY());
        results.addValue("x (nm)", estimate.getX()*pixelSize);
        results.addValue("y (nm)", estimate.getY()*pixelSize);
        results.addValue("Intensity x", estimate.getHorizontalIntensity());
        results.addValue("Intensity y", estimate.getVerticalIntensity());
        results.addValue("Background x", estimate.getHorizontalBackground());
        results.addValue("Background y", estimate.getVerticalBackground());
        results.addValue("Width x", estimate.getWidth());
        results.addValue("Width y", estimate.getHeight());
        
        if (debugMode) {
            results.addValue("Minor Axis", estimate.getMinorAxis());
            results.addValue("Major Axis", estimate.getMajorAxis());
            results.addValue("ROI x", estimate.getColumn()+0.5);
            results.addValue("ROI y", estimate.getRow()+0.5);
            
            results.addValue("thirdsum", estimate.getThirdMomentSum());
            results.addValue("thirddiff", estimate.getThirdMomentDiff());
            
            if (debugTable != null) {
                for (int column = 0; debugTable.columnExists(column); column++) {
                    final String name = "D_"+debugTable.getColumnHeading(column);
                    final double value = debugTable.getValueAsDouble(column, estimate.getSliceIndex()-1);
                    results.addValue(name, value);
                }
            }
        }
    }

}
