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

package io.github.mzmine.parameters.parametertypes.ranges;

import java.awt.GridBagConstraints;
import java.math.RoundingMode;
import java.text.NumberFormat;
import javax.swing.JLabel;
import javax.swing.JTextField;
import com.google.common.collect.Range;

import io.github.mzmine.gui.framework.listener.DelayedDocumentListener;
import io.github.mzmine.util.components.GridBagPanel;

public class DoubleRangeComponent extends GridBagPanel {

  private static final long serialVersionUID = 1L;

  private JTextField minTxtField, maxTxtField;
  private NumberFormat format;

  public DoubleRangeComponent(NumberFormat format) {

    this.format = format;

    minTxtField = new JTextField();
    minTxtField.setColumns(8);

    maxTxtField = new JTextField();
    maxTxtField.setColumns(8);

    add(minTxtField, 0, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL);
    add(new JLabel(" - "), 1, 0, 1, 1, 0, 0);
    add(maxTxtField, 2, 0, 1, 1, 1, 0, GridBagConstraints.HORIZONTAL);
  }

  public Range<Double> getValue() {
    String minString = minTxtField.getText();
    String maxString = maxTxtField.getText();

    try {
      Number minValue = format.parse(minString.trim());
      Number maxValue = format.parse(maxString.trim());

      if ((minValue == null) || (maxValue == null))
        return null;
      return Range.closed(minValue.doubleValue(), maxValue.doubleValue());

    } catch (Exception e) {
      return null;
    }
  }

  public void setNumberFormat(NumberFormat format) {
    this.format = format;
  }

  public void setValue(Range<Double> value) {
    if (value == null)
      return;
    NumberFormat floorFormat = (NumberFormat) format.clone();
    floorFormat.setRoundingMode(RoundingMode.FLOOR);
    NumberFormat ceilFormat = (NumberFormat) format.clone();
    ceilFormat.setRoundingMode(RoundingMode.CEILING);
    minTxtField.setText(floorFormat.format(value.lowerEndpoint()));
    maxTxtField.setText(ceilFormat.format(value.upperEndpoint()));
  }

  @Override
  public void setToolTipText(String toolTip) {
    minTxtField.setToolTipText(toolTip);
    maxTxtField.setToolTipText(toolTip);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    minTxtField.setEnabled(enabled);
    maxTxtField.setEnabled(enabled);
  }

  /**
   * Listens to changes
   * 
   * @param list
   */
  public void addDocumentListener(DelayedDocumentListener list) {
    minTxtField.getDocument().addDocumentListener(list);
    maxTxtField.getDocument().addDocumentListener(list);
  }
}
