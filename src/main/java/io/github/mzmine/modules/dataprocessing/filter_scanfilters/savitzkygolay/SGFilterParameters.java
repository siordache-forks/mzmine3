/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.savitzkygolay;

import java.awt.Window;

import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilterSetupDialog;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.util.ExitCode;

public class SGFilterParameters extends SimpleParameterSet {

  private static final Integer options[] = new Integer[] {5, 7, 9, 11, 13, 15};

  public static final ComboParameter<Integer> datapoints =
      new ComboParameter<Integer>("Number of datapoints", "Number of datapoints", options);

  public SGFilterParameters() {
    super(new Parameter[] {datapoints});
  }

  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ScanFilterSetupDialog dialog =
        new ScanFilterSetupDialog(parent, valueCheckRequired, this, SGFilter.class);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }

}
