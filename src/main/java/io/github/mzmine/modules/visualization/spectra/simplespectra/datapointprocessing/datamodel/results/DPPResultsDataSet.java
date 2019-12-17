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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.DataPointsDataSet;

public class DPPResultsDataSet extends DataPointsDataSet {

  private static final long serialVersionUID = 1L;

  public DPPResultsDataSet(String label, ProcessedDataPoint[] mzPeaks) {
    super(label, mzPeaks);
  }

  /**
   * This type has to be DataPoint, else you will get class cast exceptions. Casting it later on is
   * still possible.
   */
  public DataPoint[] getDataPoints() {
    return mzPeaks;
  }

}
