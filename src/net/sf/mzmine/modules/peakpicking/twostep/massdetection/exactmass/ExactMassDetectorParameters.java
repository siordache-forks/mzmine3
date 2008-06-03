/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.twostep.massdetection.exactmass;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class ExactMassDetectorParameters extends SimpleParameterSet {

    public static final Parameter noiseLevel = new SimpleParameter(
            ParameterType.FLOAT, "Noise level",
            "Intensities less than this value are interpreted as noise",
            "absolute", new Float(10.0), new Float(0.0), null,
            MZmineCore.getIntensityFormat());

	public static final Parameter resolution = new SimpleParameter(
			ParameterType.FLOAT, "Resolution",
			"Mass Spectometry resolution", "absolute", new Float(60000.00),
			new Float(0.0), null, null);
	
	public ExactMassDetectorParameters() {
        super(new Parameter[] { noiseLevel, resolution  });

    }

}
