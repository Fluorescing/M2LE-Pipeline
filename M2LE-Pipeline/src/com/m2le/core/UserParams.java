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

import ij.WindowManager;
import ij.text.TextWindow;

import java.awt.Frame;
import java.util.LinkedList;
import java.util.List;

/**
 * This non-instantiable class contains information on the user-set 
 * @author Shane Stahlheber
 *
 */
public final class UserParams {
    
    private UserParams() { }
    
    /** The current version of the plugin. */
    public static final String VERSION = "1.1.15";
    
    /** The signal-to-noise ratio multiple threshold. */
    public static final String SN_RATIO = "M2LEPL.IA.SNC";
    
    /** The minimum noise estimate allowed. */
    public static final String LOWEST_NOISE_EST = "M2LEPL.IA.LNE";
    
    /** The size of the pixel in nanometers. */
    public static final String PIXEL_SIZE = "M2LEPL.IA.PS";
    
    /** The camera saturation value (largest signal possible). */
    public static final String SATURATION = "M2LEPL.IA.SP";
    
    /** Debug-mode option. */
    public static final String DEBUG_MODE = "M2LEPL.IA.DM";
    
    /** The eccentricity threshold (single-molecule acceptance rate). */
    public static final String ECC_THRESHOLD = "M2LEPL.MR.ET";
    
    /** The third-moment threshold (single-molecule acceptance rate). */
    public static final String THRD_THRESHOLD = "M2LEPL.MR.TT";
    
    /** Option to disable the eccentricity shape test. */
    public static final String ECC_DISABLED = "M2LEPL.MR.DER";
    
    /** Option to disable the third-moment shape test. */
    public static final String THRD_DISABLED = "M2LEPL.MR.DTR";
    
    /** The wavelength of light used; in nanometers. */
    public static final String WAVELENGTH = "M2LEPL.ML.LW";
    
    /** The numerical aperture value used. */
    public static final String N_APERTURE = "M2LEPL.ML.NA";
    
    /** The fraction of camera pixel that is usable. */
    public static final String USABLE_PIXEL = "M2LEPL.ML.UP";
    
    /** Option to fix the width parameter. */
    public static final String ML_FIX_WIDTH = "M2LEPL.ML.FW";
    
    /** The ending condition for the MLE (smallest position update). */
    public static final String ML_POS_EPSILON = "M2LEPL.ML.PT";
    
    /** The ending condition for the MLE (smallest intensity update). */
    public static final String ML_INT_EPSILON = "M2LEPL.ML.IT";
    
    /** The ending condition for the MLE (smallest width update). */
    public static final String ML_WID_EPSILON = "M2LEPL.ML.WT";
    
    /** The ending condition for the MLE (maximum number of iterations). */
    public static final String ML_MAX_ITERATIONS = "M2LEPL.ML.MI";
    
    /** The maximum noise allowed (MLE). */
    public static final String ML_MAX_NOISE = "M2LEPL.ML.MNM";
    
    /** The minimum noise allowed (MLE). */
    public static final String ML_MIN_NOISE = "M2LEPL.ML.MNB";
    
    /** The maximum width allowed (MLE). */
    public static final String ML_MAX_WIDTH = "M2LEPL.ML.MAW";
    
    /** The minimum width allowed (MLE). */
    public static final String ML_MIN_WIDTH = "M2LEPL.ML.MIW";
    
    /** The name of the debug table. */
    public static final String DB_TABLE = "M2LEPL.DB.DT";
    
    /** Option to enable image reconstruction. */
    public static final String RENDER_ENABLED = "M2LEPL.RENDER";
    
    /** The scale of the reconstruction (multiple of the original image size). */
    public static final String RENDER_SCALE = "M2LEPL.RENDERSCALE";
    
    /**
     * Returns a version string containing the major, minor, and build version.
     * @return The version string.
     */
    public static String getVersionString() {
        return VERSION;
    }
    
    // get a list of all text windows (except log)
    private static String[] getResultsTables() {
        final Frame[] frames = WindowManager.getNonImageWindows();
        final List<String> tables = new LinkedList<String>();
        tables.add("");
        for (Frame f : frames) {
            if (!f.getTitle().equalsIgnoreCase("log") 
                    && f.getClass().equals(TextWindow.class)) {
                tables.add(f.getTitle());
            }
        }
        return tables.toArray(new String[0]);
    }
    
    /**
     * Initialize the user parameters and parameter info for the new job.
     * @param job the job context
     */
    public static void getUserParameters(final JobContext job) {
        
        final String[] tables = getResultsTables();
        
        job.addLabel("Image Analysis");
        job.addNumericField(SN_RATIO,           "SignalNoise Cutoff",       4.0,  2, "");
        job.addNumericField(LOWEST_NOISE_EST,   "Lowest Noise Estimate",    2.0,  0, "photons");
        job.addNumericField(PIXEL_SIZE,         "Pixel Size",             110.0,  2, "nanometers");
        job.addNumericField(SATURATION,         "Saturation Point",     65535.0,  2, "DN");
        
        job.addLabel("Debug Options");
        job.addCheckboxField(DEBUG_MODE,        "Debug Mode",   false);
        job.addComboboxField(DB_TABLE,          "Debug Table",  tables);
        
        job.addLabel("Molecule Rejection");
        job.addNumericField(ECC_THRESHOLD,      "Eccentricity Threshold",    .9,  2, "");
        job.addNumericField(THRD_THRESHOLD,     "Third Moment Threshold",    .9,  2, "");
        job.addCheckboxField(ECC_DISABLED,      "Disable Ellipticity Rejector", false);
        job.addCheckboxField(THRD_DISABLED,     "Disable_Third Moment Rejector", false);
        
        job.addLabel("Maximum Likelihood Estimator");
        job.addCheckboxField(ML_FIX_WIDTH,      "Fixed Width",  true);
        job.addNumericField(WAVELENGTH,         "Light Wavelength",       550.0,  1, "nanometers");
        job.addNumericField(N_APERTURE,         "Numerical Aperture",       1.0,  2, "");
        job.addNumericField(USABLE_PIXEL,       "Usable Pixel",            90.0,  1, "%");
        job.addNumericField(ML_POS_EPSILON,     "Position Threshold",       0.0001, 4, "nanometers");
        job.addNumericField(ML_INT_EPSILON,     "Intensity Threshold",      0.01,  4, "%");
        job.addNumericField(ML_WID_EPSILON,     "Width Threshold",          0.0001, 4, "px");
        job.addNumericField(ML_MAX_ITERATIONS,  "Maximum Iterations",      50.0,  0, "");
        
        job.addLabel("Parameter Bounds");
        job.addNumericField(ML_MAX_NOISE,       "Max Noise Multiplier",     2.0,  0, "");
        job.addNumericField(ML_MIN_NOISE,       "Min Noise Bound",          1.0,  2, "photons");
        job.addNumericField(ML_MAX_WIDTH,       "MaxWidth",                 3.0,  2, "px");
        job.addNumericField(ML_MIN_WIDTH,       "MinWidth",                 1.5,  2, "px");
        
        job.addLabel("Sample Rendering");
        job.addCheckboxField(RENDER_ENABLED,    "Enable rendering", false);
        job.addNumericField(RENDER_SCALE,       "Render scale",             4.0,  4, "");
        
        job.addLabel(String.format("Version: %s", VERSION));
    }
}
