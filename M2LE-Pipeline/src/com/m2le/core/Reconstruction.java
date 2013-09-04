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
import ij.ImagePlus;
import ij.process.ImageProcessor;

import static com.m2le.core.UserSettings.RENDER_ENABLED;
import static com.m2le.core.UserSettings.RENDER_SCALE;

/**
 * User: Shane Stahlheber
 * Date: 9/3/13
 * Time: 4:23 PM
 */
public class Reconstruction {

    private final boolean renderingEnabled;
    private final double renderScale;
    private final int renderWidth;
    private final int renderHeight;
    private int[][] renderedImage;

    /**
     * Initializes the reconstruction object from the settings located in the job context and image information
     * located in the stack context.
     * @param job the job context; which contains user settings
     * @param stack the stack context; which contains image information
     */
    public Reconstruction(final JobContext job, final StackContext stack) {

        renderingEnabled = job.getCheckboxValue(RENDER_ENABLED);
        renderScale = job.getNumericValue(RENDER_SCALE);

        renderWidth = (int) (stack.getWidth() * renderScale);
        renderHeight = (int) (stack.getHeight() * renderScale);

        if (renderingEnabled) {
            renderedImage = new int[renderHeight][renderWidth];
        }
    }

    /**
     * Displays the reconstruction in ImageJ.
     */
    public void display() {
        if (renderingEnabled) {
            final ImagePlus impRender = IJ.createImage("Sample Rendering", "16-bit", renderWidth, renderHeight, 1);
            final ImageProcessor ipRender = impRender.getProcessor();
            for (int y = 0; y < renderHeight; y++) {
                for (int x = 0; x < renderWidth; x++) {
                    ipRender.putPixel(x, y, renderedImage[y][x]);
                }
            }
            impRender.show();
        }
    }

    /**
     * Accumulates a single digital number representing a molecule at the specified position
     * in the reconstruction image.
     * @param x the molecule position; x
     * @param y the molecule position; y
     */
    public void accumulate(final double x, final double y) {
        if (renderingEnabled) {
            final int column = (int) (x * renderScale);
            final int row = (int) (y * renderScale);
            if (column >= 0 && column < renderWidth &&
                    row >= 0 && row < renderHeight)
                renderedImage[row][column] += 1;
        }
    }
}
