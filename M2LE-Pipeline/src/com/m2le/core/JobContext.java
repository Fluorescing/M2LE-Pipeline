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

import java.awt.Font;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;

/**
 * Keeps track of job-specific information, including preference setting/saving
 * and debugging information.
 * @author Shane Stahlheber
 */
public class JobContext {
    
    public enum Type {
        /** Numeric Field */
        NUMERIC, 
        /** Label */
        LABEL, 
        /** Check-box Field */
        CHECKBOX, 
        /** Combo-box Field */
        COMBOBOX
    }
    
    private class Parameter {
        
        /** The type. */
        public Type type;
        public String id;
        public String name;
        public double numeric;
        public boolean checkbox;
        public String choice[];
        public int precision;
        public String units;
    }
    
    private final List<Parameter> mList = new LinkedList<>();
    private final Map<String,Double>  mNumeric  = new HashMap<>();
    private final Map<String,Boolean> mCheckbox = new HashMap<>();
    private final Map<String,String>  mChoice   = new HashMap<>();
    private boolean mCanceled = false;
    
    /**
     * Checks if the job has been canceled .
     * @return true if canceled; false otherwise.
     */
    public boolean isCanceled() {
        return mCanceled;
    }
    
    /**
     * Add a label to the job setup dialog box.
     * @param label the text of the label.
     */
    public void addLabel(final String label) {
        final Parameter param = new Parameter();
        param.type = Type.LABEL;
        param.name = label;
        mList.add(param);
    }
    
    /**
     * Add a numeric parameter to the job.
     * @param id the id of the job parameter.
     * @param name the name of the job parameter.
     * @param defaultValue the default value.
     * @param precision the decimal precision.
     * @param units the units of the job parameter.
     */
    public void addNumericField(
            final String id, 
            final String name, 
            final double defaultValue, 
            final int precision, 
            final String units) {
        final Parameter param = new Parameter();
        param.type = Type.NUMERIC;
        param.id = id;
        param.name = name;
        param.numeric = defaultValue;
        param.precision = precision;
        param.units = units;
        mList.add(param);
    }
    
    /**
     * Add a check-box parameter to the job.
     * @param id the id of the job parameter.
     * @param name the name of the job parameter.
     * @param defaultValue the default value.
     */
    public void addCheckboxField(
            final String id, 
            final String name, 
            final boolean defaultValue) {
        final Parameter param = new Parameter();
        param.type = Type.CHECKBOX;
        param.id = id;
        param.name = name;
        param.checkbox = defaultValue;
        mList.add(param);
    }
    
    /**
     * Add a combo-box parameter to the job.
     * @param id the id of the job parameter.
     * @param name the name of the job parameter.
     * @param choices the possible choices.
     */
    public void addComboboxField(
            final String id, 
            final String name, 
            final String[] choices) {
        final Parameter param = new Parameter();
        param.type = Type.COMBOBOX;
        param.id = id;
        param.name = name;
        param.choice = choices;
        mList.add(param);
    }
    
    /**
     * @param id the identification of the field.
     * @return the numerical value of the field.
     */
    public double getNumericValue(final String id) {
        return mNumeric.get(id);
    }
    
    /**
     * @param id the identification of the field.
     * @return the check-box value of the field.
     */
    public boolean getCheckboxValue(final String id) {
        return mCheckbox.get(id);
    }
    
    /**
     * @param id the identification of the field.
     * @return the combo-box choice of the field.
     */
    public String getComboboxChoice(final String id) {
        return mChoice.get(id);
    }
    
    // create and show the preference dialog to the user
    private GenericDialog createDialog() {
        final Font header = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        
        final GenericDialog dialog = new GenericDialog("M2LE Options");
        
        dialog.centerDialog(true);
        
        for (Parameter param : mList) {
            switch (param.type) {
            case LABEL:
                dialog.addMessage(param.name, header);
                break;
            case NUMERIC:
                dialog.addNumericField(param.name, 
                        Prefs.get(param.id, param.numeric), param.precision,
                        6, param.units);
                break;
            case CHECKBOX:
                dialog.addCheckbox(param.name,
                        Prefs.get(param.id, param.checkbox));
                break;
            case COMBOBOX:
                if (param.choice.length > 0)
                    dialog.addChoice(param.name, param.choice, param.choice[0]);
                else
                    dialog.addChoice(param.name, new String[] {""}, "");
                break;
            default:
                break;
            }
        }
        
        dialog.showDialog();
        
        return dialog;
    }
    
    // grab the preferences from the dialog (must be in the same order)
    private void getPreferences(final GenericDialog dialog) {
        
        for (Parameter param : mList) {
            switch (param.type) {
            case NUMERIC:
                final Double numeric = dialog.getNextNumber();
                mNumeric.put(param.id, numeric);
                break;
            case CHECKBOX:
                final Boolean checkbox = dialog.getNextBoolean();
                mCheckbox.put(param.id, checkbox);
                break;
            case COMBOBOX:
                final String choice = dialog.getNextChoice();
                mChoice.put(param.id, choice);
                break;
            default:
                break;
            }
        }
    }
    
    // save the preferences in ImageJ for later use of the plugin
    private void savePreferences() {
        for (Parameter param : mList) {
            switch (param.type) {
            case NUMERIC:
                Prefs.set(param.id, mNumeric.get(param.id));
                break;
            case CHECKBOX:
                Prefs.set(param.id, mCheckbox.get(param.id));
                break;
            default:
                break;
            }
        }
    }
    
    /**
     * log preferences and setup debug statistics.
     */
    public void logParameters() {
        
        for (Parameter param : mList) {
            switch (param.type) {
            case LABEL:
                IJ.log(String.format("==%s==", param.name));
                break;
            case NUMERIC:
                IJ.log(String.format("%s: %g", param.name, mNumeric.get(param.id)));
                break;
            case CHECKBOX:
                IJ.log(String.format("%s: %s", param.name, mCheckbox.get(param.id)?"True":"False"));
                break;
            case COMBOBOX:
                IJ.log(String.format("%s: %s", param.name, mChoice.get(param.id)));
                break;
            default:
                break;
            }
        }
    }
    
    /**
     * Initialize the job.
     */
    public void initialize() {
        
        // show the dialog to the user
        final GenericDialog dialog = createDialog();

        mCanceled = dialog.wasCanceled();

        // create a jobcontext from the user-set preferences
        getPreferences(dialog);
        
        // save the preferences in ImageJ
        savePreferences();
    }

}
