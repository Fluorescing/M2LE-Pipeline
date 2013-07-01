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

public final class UserParams {
    
    private UserParams() { }
    
    public static final String VERSION = "1.1.15";
    
    public static final String SN_RATIO = "M2LEPL.IA.SNC";
    public static final String LOWEST_NOISE_EST = "M2LEPL.IA.LNE";
    public static final String PIXEL_SIZE = "M2LEPL.IA.PS";
    public static final String SATURATION = "M2LEPL.IA.SP";
    public static final String DEBUG_MODE = "M2LEPL.IA.DM";
    public static final String ECC_THRESHOLD = "M2LEPL.MR.ET";
    public static final String THRD_THRESHOLD = "M2LEPL.MR.TT";
    public static final String ECC_RADIUS = "M2LEPL.MR.TR";
    public static final String ECC_DISABLED = "M2LEPL.MR.DER";
    public static final String THRD_DISABLED = "M2LEPL.MR.DTR";
    public static final String WAVELENGTH = "M2LEPL.ML.LW";
    public static final String N_APERTURE = "M2LEPL.ML.NA";
    public static final String USABLE_PIXEL = "M2LEPL.ML.UP";
    public static final String ML_FIX_WIDTH = "M2LEPL.ML.FW";
    public static final String ML_POS_EPSILON = "M2LEPL.ML.PT";
    public static final String ML_INT_EPSILON = "M2LEPL.ML.IT";
    public static final String ML_WID_EPSILON = "M2LEPL.ML.WT";
    public static final String ML_MAX_ITERATIONS = "M2LEPL.ML.MI";
    public static final String ML_RADIUS = "M2LEPL.ML.IR";
    public static final String ML_MAX_NOISE = "M2LEPL.ML.MNM";
    public static final String ML_MIN_NOISE = "M2LEPL.ML.MNB";
    public static final String ML_MAX_WIDTH = "M2LEPL.ML.MAW";
    public static final String ML_MIN_WIDTH = "M2LEPL.ML.MIW";
    public static final String DB_TABLE = "M2LEPL.DB.DT";
    public static final String DB_ROI = "M2LEPL.DB.ROI";
    public static final String RENDER_ENABLED = "M2LEPL.RENDER";
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
        job.addComboboxField(DB_TABLE,            "Debug Table",  tables);
        
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
