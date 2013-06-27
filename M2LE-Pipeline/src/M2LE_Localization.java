
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
        if (job.isCanceled()) {
            return;
        }
        
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
        
        final int pixelSize = (int) job.getNumericValue(UserParams.PIXEL_SIZE);
        
        // should we render an image?
        final boolean render = job.getCheckboxValue(UserParams.RENDER_ENABLED);
        final double rscale = job.getNumericValue(UserParams.RENDER_SCALE);
        
        final int rwidth = (int) (stack.getWidth()*rscale);
        final int rheight = (int) (stack.getHeight()*rscale);
        
        int[][] rendering = null;
        if (render)
            rendering = new int[rheight][rwidth];
        
        // add to results table
        int SIZE = funnelled.size()-1;
        while (true) {
            try {
                // get pixel
                final Estimate estimate = funnelled.take();
                
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
                
            } catch (InterruptedException e) {
                IJ.handleException(e);
            }
        }
        
        // display rendering
        if (render) {
            final ImagePlus rimp = IJ.createImage("Sample Rendering", "16-bit", rwidth, rheight, 1);
            final ImageProcessor rip = rimp.getProcessor();
            for (int y = 0; y < rheight; y++) {
                for (int x = 0; x < rwidth; x++) {
                    rip.set(x, y, rendering[y][x]);
                }
            }
            rimp.show();
        }
        
        // show the results
        results.show("Localization Results");
        
        IJ.showStatus(String.format("%d Localizations.", SIZE));
        IJ.log(String.format("[%d localizations]", SIZE));
    }

}
