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

import java.util.Random;

import ij.process.ImageProcessor;

/**
 * The Class StaticMath.  This class contains commonly used mathematical functions.
 */
public final class StaticMath {
    
    private StaticMath() { }
    
    private static Random rand = new Random(System.currentTimeMillis());
    
    /**
     * Calculate the eccentricity threshold.  
     *
     * @param photons the photon count per region
     * @param acc the single-molecule acceptance rate
     * @return the threshold
     */
    public static double calculateThreshold(double photons, double acc) {
        
        final double x0h = acc - 89.952;
        final double x0 = 61172.0/(x0h*x0h + 1307.9) - 97.515;
        final double y0 = 2.1759e-6*Math.pow(acc, 2.2837) + 0.082876;
        final double Ah = acc - 120.7;
        final double A = 992.92/(Ah*Ah - 35.069) + 2.9048;

        return A/Math.sqrt(photons - x0) + y0;
    }
    
    /**
     * Calculate third-moment threshold.
     *
     * @param photons the photon count per region
     * @param acc the single-molecule acceptance rate
     * @return the threshold
     */
    public static double calculateThirdThreshold(double photons, double acc) {
        
        final double A = 5.5801 + (-2.078e+5)/((acc - 147.95)*(acc - 147.95) - 1909.1);
        final double x0 = -5135.6 + acc*(423.24 + acc*(-13.961 + acc*(0.22529 + acc*(-0.0017943 + acc*(5.648e-6)))));
        final double y0 = -0.55942 + 30822.0/((acc - 183.86)*(acc - 183.86) - 6524.8);
    
        return A/Math.sqrt(photons - x0) + y0;
    }

    /**
     * Estimate the photon count of the region.
     *
     * @param ip the image processor
     * @param left the left coordinate
     * @param right the right coordinate
     * @param top the top coordinate
     * @param bottom the bottom coordinate
     * @param noise the background noise per pixel
     * @param scale the photon count scaling
     * @return the estimated photon count per region (excluding background)
     */
    public static double estimatePhotonCount(
            final ImageProcessor ip,
            final int left, final int right, 
            final int top, final int bottom,
            final double noise,
            final double scale) {
        
        double sum = 0.0;
        
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                sum += Math.max(ip.get(x, y)/scale - noise, 0.0);
            }
        }
        
        return sum;
    }
    
    /**
     * The minimum value in the SignalArray.
     *
     * @param array the signal array
     * @return the minimum value in the array
     */
    public static double min(final SignalArray array) {
        double min = array.get(0);
        
        for (int i = 1; i < array.getSize(); i++) {
            if (array.get(i) < min) {
                min = array.get(i);
            }
        }
        
        return min;
    }
    
    /**
     * The maximum value in the SignalArray.
     *
     * @param array the signal array
     * @return the maximum value in the array
     */
    public static double max(final SignalArray array) {
        double max = array.get(0);
        
        for (int i = 1; i < array.getSize(); i++) {
            if (array.get(i) > max) {
                max = array.get(i);
            }
        }
        
        return max;
    }
    
    /**
     * Error function.
     * <p>
     * Accuracy: fractional error less than x.xx * 10 ^ -4.
     * <p>
     * Source: Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical,
     * [http://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html].
     *
     * @param z z
     * @return erf(z)
     */
    public static double erf(final double z) {
        final double t = 1.0 / (1.0 + 0.47047 * ((z<0)?-z:z));
        final double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
        final double ans = 1.0 - poly * Math.exp(-z*z);
        
        if (z >= 0)
            return  ans;
        else
            return -ans;
    }

    /**
     * Fast error function approximation.
     *
     * @param x x
     * @return erf(x)
     */
    public static double erf2(final double x) {
        double v;
        
        if (x < 0)
            v = -x;
        else
            v = x;
        
        final double p = 0.3275911;
        final double t = 1.0/(1.0 + p*v);
        final double t2 = t*t;
        final double t3 = t2*t;
        final double t4 = t3*t;
        final double t5 = t4*t;
        final double a1 =  0.254829592 * t;
        final double a2 = -0.284496736 * t2;
        final double a3 =  1.421413741 * t3;
        final double a4 = -1.453152027 * t4;
        final double a5 =  1.061405429 * t5;
        final double result = 1.0 - (a1 + a2 + a3 + a4 + a5) * Math.exp(-v*v);
        
        if (x < 0)
            return -result;
        
        return result;
    }
    
    /**
     * Estimate the center.
     *
     * @param signal the signal array
     * @param noise the background photon count per pixel
     * @return the estimated center
     */
    public static double estimateCenter(final SignalArray signal, final double noise) {
        double center = 0;
        double sum = 0;
        
        for (int i = 0; i < signal.getSize(); i++) {
            center += Math.max(signal.get(i) - noise, 0)*signal.getPosition(i);
            sum += Math.max(signal.get(i) - noise, 0);
        }
        
        return center / sum;
    }

    /**
     * Estimate the centroid.
     *
     * @param ip the image processor
     * @param left the left coordinate
     * @param right the right coordinate
     * @param top the top coordinate
     * @param bottom the bottom coordinate
     * @param noise the background photon count per pixel
     * @param scale the photon count scaling
     * @return a two-value array representing the coordinate (x,y)
     */
    public static double[] estimateCentroid(
            final ImageProcessor ip,
            final int left, final int right, 
            final int top, final int bottom,
            final double noise,
            final double scale) {
        
        final double[] centroid = {0,0};
        double sum = 0;
        
        // find second moments
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                
                final double S = Math.max(ip.get(x, y)/scale - noise, 0.0);
                
                centroid[0] += S*(x+0.5);
                centroid[1] += S*(y+0.5);
                
                sum += S;
            }
        }
        
        centroid[0] /= sum;
        centroid[1] /= sum;
        
        return centroid;
    }

    /**
     * Estimate the second-moments.
     *
     * @param ip the image processor
     * @param centroid the centroid
     * @param left the left coordinate
     * @param right the right coordinate
     * @param top the top coordinate
     * @param bottom the bottom coordinate
     * @param noise the background photon count per pixel
     * @param scale the photon count scaling
     * @return a three-value array containing the second moments (XX, YY, XY)
     */
    public static double[] estimateSecondMoments(
            final ImageProcessor ip, 
            final double[] centroid, 
            final int left, final int right, 
            final int top, final int bottom,
            final double noise,
            final double scale) {
        
        final double[] moment = {0.0, 0.0, 0.0};
        double sum = 0;
        
        // find second moments
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                
                final double S = Math.max(ip.get(x, y)/scale - noise, 0.0);
                
                moment[0] += S*(x+0.5)*(x+0.5);
                moment[1] += S*(y+0.5)*(y+0.5);
                moment[2] += S*(x+0.5)*(y+0.5);
                
                sum += S;
            }
        }
        
        moment[0] /= sum;
        moment[1] /= sum;
        moment[2] /= sum;
        
        moment[0] -= centroid[0]*centroid[0];
        moment[1] -= centroid[1]*centroid[1];
        moment[2] -= centroid[0]*centroid[1];
        
        return moment;
    }
    
    /**
     * Gets the third-moment summation scaling.
     *
     * @param intensity the photon count per region
     * @return the third-moment summation scaling
     */
    public static double getThirdMomentSumScaling(double intensity) {
        return 10.246 + (0.1697 + (1.7027e-5 + 9.3532e-13*intensity)*intensity)*intensity;
    }
    
    /**
     * Gets the third-moment difference scaling.
     *
     * @param intensity the photon count per region
     * @return the third moment difference scaling
     */
    public static double getThirdMomentDiffScaling(double intensity) {
        return 41.227 + (0.64077 + (3.6687e-6 + 1.7874e-12*intensity)*intensity)*intensity;
    }
    
    /**
     * Calculate the third-moments using a Monte-Carlo method.
     *
    * @param ip the image processor
     * @param centroid the centroid
     * @param left the left coordinate
     * @param right the right coordinate
     * @param top the top coordinate
     * @param bottom the bottom coordinate
     * @param noise the background photon count per pixel
     * @param wavelength the wavelength of light used
     * @param width the width of the point spread function
     * @param scale the photon count scaling
     * @return a two-value array of the independent third-moments (sum, diff)
     */
    public static double[] calculateMonteCarloThirdMoments(
            final ImageProcessor ip, 
            final double[] centroid, 
            final int left, final int right, 
            final int top, final int bottom,
            final double noise,
            final double wavelength,
            final double width,
            final double scale) {
        
        final double alpha = Math.sqrt(8.)*Math.PI/(wavelength*width);
        final double beta = Math.sqrt(8.*Math.PI)/(wavelength*width);
        
        // $\sqrt{2^{m+n}m!n!}$ 
        final double f = 0.14433756729740644112728719512549;    // $m=3,n=0$
        final double g = 0.25;                                  // $m=2,n=1$
        
        // number of samples
        final int N = 5;
        
        // prepare modes for accumulation
        double mode30 = 0.0;
        double mode21 = 0.0;
        double mode12 = 0.0;
        double mode03 = 0.0;
        
        for (int i = 0; i < N; i++) {
            
            // vary the center from where we calculate our third moments
            final double dx = (rand.nextDouble() - 0.5)/2.;
            final double dy = (rand.nextDouble() - 0.5)/2.;
            
            // for all pixels in the region of interest, calculate modes
            for (int x = left; x < right; x++) {
                for (int y = top; y < bottom; y++) {
                    
                    // signal minus noise
                    final double S = ip.get(x, y)/scale - noise;
                    
                    final double x0 = (x + 0.5 - centroid[0] + dx)*alpha; 
                    final double y0 = (y + 0.5 - centroid[1] + dy)*alpha;
                    
                    final double mask = Math.exp(-(x0*x0 + y0*y0)/2.);
                    
                    mode30 += S*beta*f*(x0*(8.*x0*x0 - 12.))*mask;
                    mode21 += S*beta*g*(2.*y0)*(4.*x0*x0 - 2.)*mask;
                    mode12 += S*beta*g*(2.*x0)*(4.*y0*y0 - 2.)*mask;
                    mode03 += S*beta*f*(y0*(8.*y0*y0 - 12.))*mask;
                }
            }
        }
        
        mode30 /= N;
        mode21 /= N;
        mode12 /= N;
        mode03 /= N;
        
        double sum = 0.0;
        double diff = 0.0;
        
        sum += (mode30 + mode12)*(mode30 + mode12) 
             + (mode03 + mode21)*(mode03 + mode21);
        diff += (3.*mode12 - mode30)*(3.*mode12 - mode30) 
              + (mode03 - 3.*mode21)*(mode03 - 3.*mode21);
        
        return new double[] {sum, diff};
    }

    /**
     * Finds the eigen values of the moments given; (xx, yy, xy) -> [[xx yx],[xy yy]].
     *
     * @param moment the second-moments of the region
     * @return a two-value array containing the major- and minor-axis, respectively
     */
    public static double[] findEigenValues(final double[] moment) {
        
        final double[] eigenValues = {0.0, 0.0};
        final double first = moment[0] + moment[1];
        final double diff = moment[0] - moment[1];
        final double last = Math.sqrt(4.0 * moment[2] * moment[2] + diff * diff);
        
        eigenValues[0] = (first + last) / 2.0;
        eigenValues[1] = (first - last) / 2.0;
        
        return eigenValues;
    } 
    
    /**
     * Estimates the noise.
     *
     * @param stack the image stack
     * @param pixel the interesting pixel
     * @param scale the photon count scaling
     * @return the background noise estimate
     */
    public static double estimateNoise(final ImageProcessor ip, final StackContext stack, final Estimate pixel, final double scale) {
        
        //final ImageProcessor ip = stack.getImageProcessor(pixel.getSliceIndex());
        
        double noiseEstimate = -1.0;
        double tempnoise;
        
        final int left = Math.max(0, pixel.getColumn() - 3);
        final int right = Math.min(ip.getWidth(), pixel.getColumn() + 4);
        final int top = Math.max(0, pixel.getRow() - 3);
        final int bottom = Math.min(ip.getHeight(), pixel.getRow() + 4);
        
        for (int x = left; x < right; x++) {
            tempnoise = 0.0;
            for (int y = top; y < bottom; y++) {
                tempnoise += ip.get(x, y) / scale;
            }
            tempnoise /= (bottom-top);
            if (tempnoise < noiseEstimate || noiseEstimate < 0.0) {
                noiseEstimate = tempnoise;
            }
        }
        
        for (int y = top; y < bottom; y++) {
            tempnoise = 0.0;
            for (int x = left; x < right; x++) {
                tempnoise += ip.get(x, y) / scale;
            }
            tempnoise /= (right-left);
            if (tempnoise < noiseEstimate || noiseEstimate < 0.0) {
                noiseEstimate = tempnoise;
            }
        }
        
        return noiseEstimate;
    }
    
    /**
     * Calculates the percent difference between a and b.
     * @param a a
     * @param b b
     * @return the percent difference
     */
    public static double percentDifference(final double a, final double b) {
        final double c = 2. * (a - b) / (a + b);
        return (c < 0.) ? -c : c;
    }
}
