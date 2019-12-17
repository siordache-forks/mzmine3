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

package io.github.mzmine.util.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_manual.ManualPeakPickerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;

public class PeakSummaryComponent extends JPanel implements ActionListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final DecimalFormat formatter = new DecimalFormat("###.#");

  private static final Font defaultFont = new Font("SansSerif", Font.PLAIN, 11);
  private static final Font titleFont = new Font("SansSerif", Font.BOLD, 14);
  private static final Font ratioFont = new Font("SansSerif", Font.PLAIN, 18);

  private static final Dimension xicPreferredSize = new Dimension(350, 70);

  private JButton btnChange, btnShow;
  private JComboBox<String> comboShow;
  private JLabel ratio;

  private PeakSummaryTableModel listElementModel;
  private JTable peaksInfoList;

  private PeakListRow row;

  private static String[] visualizers =
      {"Chromatogram", "Mass spectrum", "Peak in 2D", "Peak in 3D", "MS/MS", "Isotope pattern"};

  private Color bg = new Color(255, 250, 205); // default color

  public PeakSummaryComponent(PeakListRow row, boolean headerVisible, boolean ratioVisible,
      boolean graphVisible, boolean tableVisible, boolean buttonsVisible, Color backgroundColor) {
    this(row, row.getRawDataFiles(), headerVisible, ratioVisible, graphVisible, tableVisible,
        buttonsVisible, backgroundColor);
  }

  /**
   * @param index
   * @param dataSet
   * @param fold
   * @param frame
   */
  public PeakSummaryComponent(PeakListRow row, RawDataFile[] rawDataFiles, boolean headerVisible,
      boolean ratioVisible, boolean graphVisible, boolean tableVisible, boolean buttonsVisible,
      Color backgroundColor) {

    if (backgroundColor != null) {
      bg = backgroundColor;
    }

    setBackground(bg);

    this.row = row;

    // Get info
    Feature[] peaks = new Feature[rawDataFiles.length];
    for (int i = 0; i < peaks.length; i++) {
      peaks[i] = row.getPeak(rawDataFiles[i]);
    }

    PeakIdentity identity = row.getPreferredPeakIdentity();

    // General container
    JPanel pnlAll = new JPanel(new BorderLayout());
    pnlAll.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    pnlAll.setBackground(bg);

    // Header peak identification & ratio
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
    JLabel name, info;
    if (identity != null) {
      name = new JLabel(identity.getName(), SwingUtilities.LEFT);
      StringBuffer buf = new StringBuffer();
      Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
      Format timeFormat = MZmineCore.getConfiguration().getRTFormat();
      buf.append("#" + row.getID() + " ");
      buf.append(mzFormat.format(row.getAverageMZ()));
      buf.append(" m/z @");
      buf.append(timeFormat.format(row.getAverageRT()));
      info = new JLabel(buf.toString(), SwingUtilities.LEFT);
      info.setBackground(bg);
      info.setFont(defaultFont);
      headerPanel.add(name, BorderLayout.NORTH);
      headerPanel.add(info, BorderLayout.CENTER);
    } else {
      name = new JLabel(row.toString(), SwingUtilities.LEFT);
      headerPanel.add(name, BorderLayout.CENTER);
    }

    name.setFont(titleFont);
    name.setBackground(bg);
    headerPanel.setBackground(bg);
    headerPanel.setPreferredSize(new Dimension(290, 50));
    headerPanel.setVisible(headerVisible);

    // Ratio between peaks
    JPanel ratioPanel = new JPanel(new BorderLayout());
    ratio = new JLabel("", SwingUtilities.LEFT);
    ratio.setFont(ratioFont);

    ratio.setBackground(bg);
    ratioPanel.add(ratio, BorderLayout.CENTER);

    ratioPanel.setBackground(bg);
    ratioPanel.setVisible(ratioVisible);

    JPanel headerAndRatioPanel = new JPanel(new BorderLayout());
    headerAndRatioPanel.add(headerPanel, BorderLayout.WEST);
    headerAndRatioPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
    headerAndRatioPanel.add(ratioPanel, BorderLayout.EAST);
    headerAndRatioPanel.setBackground(bg);
    pnlAll.add(headerAndRatioPanel, BorderLayout.NORTH);
    // <-

    // Plot section
    JPanel plotPanel = new JPanel();
    plotPanel.setLayout(new BoxLayout(plotPanel, BoxLayout.Y_AXIS));
    Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    plotPanel.setBorder(BorderFactory.createCompoundBorder(one, two));
    plotPanel.setBackground(Color.white);
    // No tooltip
    CombinedXICComponent xic = new CombinedXICComponent(peaks, -1);
    xic.setPreferredSize(xicPreferredSize);
    plotPanel.add(xic);
    plotPanel.setVisible(graphVisible);
    pnlAll.add(plotPanel, BorderLayout.CENTER);
    // <-

    // Table with peak's information
    JPanel tablePanel = new JPanel();
    tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
    tablePanel.setBackground(bg);

    listElementModel = new PeakSummaryTableModel();
    peaksInfoList = new JTable();
    peaksInfoList.setModel(listElementModel);
    // commenting this out did not break anything for my. Why does every
    // actionCommand work with
    // multiple row selection, but the table itself disallows it?
    // ~ SteffenHeu
    // peaksInfoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    peaksInfoList.setDefaultRenderer(Object.class, new PeakSummaryTableCellRenderer());

    int colorIndex = 0;
    Color peakColor;

    for (Feature peak : peaks) {
      // set color for current XIC
      if (peak != null) {
        peakColor = CombinedXICComponent.plotColors[colorIndex];
        listElementModel.addElement(peak, peakColor);
      }
      colorIndex = (colorIndex + 1) % CombinedXICComponent.plotColors.length;
    }

    JPanel listPanel = new JPanel(new BorderLayout());
    listPanel.add(new JScrollPane(peaksInfoList), BorderLayout.CENTER);
    listPanel.add(peaksInfoList.getTableHeader(), BorderLayout.NORTH);
    listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

    Dimension d = calculatedTableDimension(peaksInfoList);
    listPanel.setPreferredSize(d);

    tablePanel.add(Box.createVerticalStrut(5));
    tablePanel.add(listPanel, BorderLayout.CENTER);
    tablePanel.setBackground(bg);
    tablePanel.setVisible(tableVisible);

    // Buttons
    comboShow = new JComboBox<String>(visualizers);

    btnShow = new JButton("Show");
    btnShow.setActionCommand("SHOW");
    btnShow.addActionListener(this);

    btnChange = new JButton("Change");
    btnChange.setActionCommand("CHANGE");
    btnChange.addActionListener(this);

    JPanel pnlShow = new JPanel(new BorderLayout());
    pnlShow.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    pnlShow.add(comboShow, BorderLayout.NORTH);
    pnlShow.add(btnShow, BorderLayout.CENTER);
    pnlShow.setBackground(bg);

    JPanel buttonsPanel = new JPanel(new BorderLayout());
    buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    buttonsPanel.add(pnlShow, BorderLayout.NORTH);
    buttonsPanel.add(Box.createVerticalGlue(), BorderLayout.CENTER);
    buttonsPanel.add(btnChange, BorderLayout.SOUTH);
    buttonsPanel.setBackground(bg);
    buttonsPanel.setVisible(buttonsVisible);

    JPanel buttonsAndTablePanel = new JPanel(new BorderLayout());
    buttonsAndTablePanel.add(tablePanel, BorderLayout.CENTER);
    buttonsAndTablePanel.add(buttonsPanel, BorderLayout.EAST);
    buttonsAndTablePanel.setBackground(bg);

    pnlAll.add(buttonsAndTablePanel, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    add(pnlAll, BorderLayout.CENTER);

  }

  public void setRatio(double area1, double area2) {
    String text;
    Color ratioColor;
    if (area1 > area2) {
      text = formatter.format(area1 / area2) + "x";
      ratioColor = CombinedXICComponent.plotColors[0];
    } else {
      text = formatter.format(area2 / area1) + "x";
      ratioColor = CombinedXICComponent.plotColors[1];
    }
    ratio.setForeground(ratioColor);
    ratio.setText(text);
  }

  /**
   * @param peaksInfoList
   * @return
   */
  private Dimension calculatedTableDimension(JTable peaksInfoList) {

    int numRows = peaksInfoList.getRowCount();
    int numCols = peaksInfoList.getColumnCount();
    int maxWidth = 0, compWidth, totalWidth = 0, totalHeight = 0;
    TableCellRenderer renderer = peaksInfoList.getDefaultRenderer(Object.class);
    TableCellRenderer headerRenderer = peaksInfoList.getTableHeader().getDefaultRenderer();
    TableModel model = peaksInfoList.getModel();
    Component comp;
    TableColumn column;

    for (int c = 0; c < numCols; c++) {
      for (int r = 0; r < numRows; r++) {

        if (r == 0) {
          comp = headerRenderer.getTableCellRendererComponent(peaksInfoList, model.getColumnName(c),
              false, false, r, c);
          compWidth = comp.getPreferredSize().width + 10;
          maxWidth = Math.max(maxWidth, compWidth);

        }

        comp = renderer.getTableCellRendererComponent(peaksInfoList, model.getValueAt(r, c), false,
            false, r, c);

        compWidth = comp.getPreferredSize().width + 10;
        maxWidth = Math.max(maxWidth, compWidth);

        if (c == 0) {
          totalHeight += comp.getPreferredSize().height;
        }

        // Consider max 10 rows
        if (r == 8) {
          break;
        }

      }
      totalWidth += maxWidth;
      column = peaksInfoList.getColumnModel().getColumn(c);
      column.setPreferredWidth(maxWidth);
      maxWidth = 0;
    }

    // add 30x10 px for a scrollbar
    totalWidth += 30;
    totalHeight += 10;

    comp = headerRenderer.getTableCellRendererComponent(peaksInfoList, model.getColumnName(0),
        false, false, 0, 0);
    totalHeight += comp.getPreferredSize().height;

    return new Dimension(totalWidth, totalHeight);

  }

  public void actionPerformed(ActionEvent e) {

    String command = e.getActionCommand();

    if (command.equals("SHOW")) {

      String visualizerType = (String) comboShow.getSelectedItem();
      int[] indexesRow = peaksInfoList.getSelectedRows();
      Feature[] selectedPeaks = new Feature[indexesRow.length];
      RawDataFile[] dataFiles = new RawDataFile[indexesRow.length];
      Range<Double> rtRange = null, mzRange = null;
      for (int i = 0; i < indexesRow.length; i++) {
        selectedPeaks[i] = listElementModel.getElementAt(indexesRow[i]);
        dataFiles[i] = selectedPeaks[i].getDataFile();

        if ((rtRange == null) || (mzRange == null)) {
          rtRange = dataFiles[i].getDataRTRange(1);
          mzRange = selectedPeaks[i].getRawDataPointsMZRange();
        } else {
          rtRange = rtRange.span(dataFiles[i].getDataRTRange(1));
          mzRange = mzRange.span(selectedPeaks[i].getRawDataPointsMZRange());
        }
      }

      if (dataFiles.length == 0) {
        return;
      }

      if (visualizerType.equals("Chromatogram")) {

        // Label best peak with preferred identity.
        final Feature bestPeak = row.getBestPeak();
        final PeakIdentity peakIdentity = row.getPreferredPeakIdentity();
        final Map<Feature, String> labelMap = new HashMap<Feature, String>(1);
        if (bestPeak != null && peakIdentity != null) {

          labelMap.put(bestPeak, peakIdentity.getName());
        }

        ScanSelection scanSelection = new ScanSelection(rtRange, 1);

        ChromatogramVisualizerModule.showNewTICVisualizerWindow(dataFiles, selectedPeaks, labelMap,
            scanSelection, TICPlotType.BASEPEAK, mzRange);
        return;

      } else if (visualizerType.equals("Mass spectrum")) {

        for (int i = 0; i < selectedPeaks.length; ++i) {
          final Feature peak = selectedPeaks[i];
          final IsotopePattern ip = peak.getIsotopePattern();

          if (ip != null) {
            // ------------------------------
            // Multiply isotope pattern by -1
            // ------------------------------

            DataPoint[] newDataPoints = Arrays.stream(ip.getDataPoints())
                .map(p -> new SimpleDataPoint(p.getMZ(), -p.getIntensity()))
                .toArray(DataPoint[]::new);

            // ---------------------------
            // Construct identity spectrum
            // ---------------------------

            List<DataPoint> identityDataPoints = new ArrayList<>();
            PeakIdentity identity = row.getPreferredPeakIdentity();

            if (identity != null) {
              String spectrum = identity.getPropertyValue(PeakIdentity.PROPERTY_SPECTRUM);

              if (spectrum != null && spectrum.length() > 2) {
                spectrum = spectrum.substring(1, spectrum.length() - 1);

                for (String strPair : spectrum.split(",")) {
                  String[] pair = strPair.split("=", 2);
                  if (pair.length == 2)
                    identityDataPoints.add(new SimpleDataPoint(Double.parseDouble(pair[0]),
                        Double.parseDouble(pair[1])));
                }
              }
            }

            // -------------
            // Plot spectrum
            // -------------

            if (identityDataPoints.isEmpty()) // Plot raw spectrum
                                              // and isotope pattern
              SpectraVisualizerModule.showNewSpectrumWindow(dataFiles[i],
                  peak.getRepresentativeScanNumber(), null, null, null,
                  new SimpleIsotopePattern(newDataPoints, ip.getStatus(), ip.getDescription()));
            else // Plot raw spectrum, isotope pattern, and identity
                 // spectrum
              SpectraVisualizerModule.showNewSpectrumWindow(dataFiles[i],
                  peak.getRepresentativeScanNumber(), null,
                  new SimpleIsotopePattern(
                      identityDataPoints.toArray(new DataPoint[identityDataPoints.size()]),
                      IsotopePatternStatus.DETECTED,
                      identity.getPropertyValue(PeakIdentity.PROPERTY_FORMULA)),
                  null,
                  new SimpleIsotopePattern(newDataPoints, ip.getStatus(), ip.getDescription()));
          } else // Plot raw spectrum without isotope pattern
            SpectraVisualizerModule.showNewSpectrumWindow(dataFiles[i],
                peak.getRepresentativeScanNumber());
        }

      } else if (visualizerType.equals("Peak in 2D")) {
        for (int i = 0; i < selectedPeaks.length; i++) {
          Range<Double> peakRTRange = selectedPeaks[i].getRawDataPointsRTRange();
          Range<Double> peakMZRange = selectedPeaks[i].getRawDataPointsMZRange();
          final double rtLen = peakRTRange.upperEndpoint() - peakRTRange.lowerEndpoint();
          Range<Double> localRTRange =
              Range.closed(Math.max(0, peakRTRange.lowerEndpoint() - rtLen),
                  peakRTRange.upperEndpoint() + rtLen);

          final double mzLen = peakMZRange.upperEndpoint() - peakMZRange.lowerEndpoint();
          Range<Double> localMZRange =
              Range.closed(Math.max(0, peakMZRange.lowerEndpoint() - mzLen),
                  peakMZRange.upperEndpoint() + mzLen);
          TwoDVisualizerModule.show2DVisualizerSetupDialog(dataFiles[i], localMZRange,
              localRTRange);
        }
      } else if (visualizerType.equals("Peak in 3D")) {
        for (int i = 0; i < selectedPeaks.length; i++) {
          Range<Double> peakRTRange = selectedPeaks[i].getRawDataPointsRTRange();
          Range<Double> peakMZRange = selectedPeaks[i].getRawDataPointsMZRange();
          final double rtLen = peakRTRange.upperEndpoint() - peakRTRange.lowerEndpoint();
          Range<Double> localRTRange =
              Range.closed(Math.max(0, peakRTRange.lowerEndpoint() - rtLen),
                  peakRTRange.upperEndpoint() + rtLen);
          final double mzLen = peakMZRange.upperEndpoint() - peakMZRange.lowerEndpoint();
          Range<Double> localMZRange =
              Range.closed(Math.max(0, peakMZRange.lowerEndpoint() - mzLen),
                  peakMZRange.upperEndpoint() + mzLen);
          Fx3DVisualizerModule.setupNew3DVisualizer(dataFiles[i], localMZRange, localRTRange);
        }
      } else if (visualizerType.equals("MS/MS")) {
        for (int i = 0; i < selectedPeaks.length; i++) {
          int scanNumber = selectedPeaks[i].getMostIntenseFragmentScanNumber();
          if (scanNumber > 0) {
            SpectraVisualizerModule.showNewSpectrumWindow(dataFiles[i], scanNumber);
          } else {
            JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
            MZmineCore.getDesktop().displayMessage(null,
                "There is no fragment for the mass "
                    + MZmineCore.getConfiguration().getMZFormat().format(selectedPeaks[i].getMZ())
                    + "m/z in the current raw data.");
            return;
          }
        }

      } else if (visualizerType.equals("Isotope pattern")) {
        for (int i = 0; i < selectedPeaks.length; i++) {
          IsotopePattern ip = selectedPeaks[i].getIsotopePattern();
          if (ip == null) {
            return;
          }
          SpectraVisualizerModule.showNewSpectrumWindow(dataFiles[i],
              selectedPeaks[i].getMostIntenseFragmentScanNumber(), ip);

        }
      }
      return;
    }

    if (command.equals("CHANGE")) {
      int indexRow = peaksInfoList.getSelectedRow();
      if (indexRow == -1) {
        return;
      }
      Feature selectedPeak = listElementModel.getElementAt(indexRow);
      ManualPeakPickerModule.runManualDetection(selectedPeak.getDataFile(), row, null, null);

      return;
    }

  }

}
