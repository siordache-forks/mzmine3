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

package io.github.mzmine.modules.io.mztabimport;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.common.collect.Range;
import com.google.common.io.ByteStreams;
import com.google.common.math.DoubleMath;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.rawdataimport.RawDataImportModule;
import io.github.mzmine.modules.io.rawdataimport.RawDataImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import uk.ac.ebi.pride.jmztab.model.Assay;
import uk.ac.ebi.pride.jmztab.model.MZTabFile;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.SmallMolecule;
import uk.ac.ebi.pride.jmztab.model.SplitList;
import uk.ac.ebi.pride.jmztab.model.StudyVariable;
import uk.ac.ebi.pride.jmztab.utils.MZTabFileParser;

class MzTabImportTask extends AbstractTask {

  // parameter values
  private MZmineProject project;
  private File inputFile;
  private boolean importRawFiles;
  private double finishedPercentage = 0.0;

  // underlying tasks for importing raw data
  private final List<Task> underlyingTasks = new ArrayList<Task>();

  MzTabImportTask(MZmineProject project, ParameterSet parameters, File inputFile) {
    this.project = project;
    this.inputFile = inputFile;
    this.importRawFiles = parameters.getParameter(MzTabImportParameters.importrawfiles).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    if (importRawFiles && (getStatus() == TaskStatus.PROCESSING) && (!underlyingTasks.isEmpty())) {
      double newPercentage = 0.0;
      synchronized (underlyingTasks) {
        for (Task t : underlyingTasks) {
          newPercentage += t.getFinishedPercentage();
        }
        newPercentage /= underlyingTasks.size();
      }
      // Let's say that raw data import takes 80% of the time
      finishedPercentage = 0.1 + newPercentage * 0.8;
    }
    return finishedPercentage;
  }

  @Override
  public String getTaskDescription() {
    return "Loading feature list from mzTab file " + inputFile;
  }

  @Override
  public void cancel() {
    super.cancel();
    // Cancel all the data import tasks
    for (Task t : underlyingTasks) {
      if ((t.getStatus() == TaskStatus.WAITING) || (t.getStatus() == TaskStatus.PROCESSING))
        t.cancel();
    }
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {
      // Prevent MZTabFileParser from writing to console
      OutputStream logStream = ByteStreams.nullOutputStream();

      // Load mzTab file
      MZTabFileParser mzTabFileParser = new MZTabFileParser(inputFile, logStream);

      if (!mzTabFileParser.getErrorList().getErrorList().isEmpty()) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Error processing " + inputFile + ":\n" + mzTabFileParser.getErrorList().toString());
        return;
      }

      MZTabFile mzTabFile = mzTabFileParser.getMZTabFile();

      // Let's say the initial parsing took 10% of the time
      finishedPercentage = 0.1;

      // Import raw data files
      SortedMap<Integer, RawDataFile> rawDataFiles = importRawDataFiles(mzTabFile);

      // Check if not canceled
      if (isCanceled())
        return;

      // Create a new feature list
      String peakListName = inputFile.getName().replace(".mzTab", "");
      RawDataFile rawDataArray[] = rawDataFiles.values().toArray(new RawDataFile[0]);
      PeakList newPeakList = new SimplePeakList(peakListName, rawDataArray);

      // Check if not canceled
      if (isCanceled())
        return;

      // Import variables
      importVariables(mzTabFile, rawDataFiles);

      // Check if not canceled
      if (isCanceled())
        return;

      // import small molecules (=feature list rows)
      importSmallMolecules(newPeakList, mzTabFile, rawDataFiles);

      // Check if not canceled
      if (isCanceled())
        return;

      // Add the new feature list to the project
      project.addPeakList(newPeakList);

      // Finish
      setStatus(TaskStatus.FINISHED);
      finishedPercentage = 1.0;

    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not import data from " + inputFile + ": " + e.getMessage());
      return;
    }

  }

  private SortedMap<Integer, RawDataFile> importRawDataFiles(MZTabFile mzTabFile) throws Exception {

    SortedMap<Integer, MsRun> msrun = mzTabFile.getMetadata().getMsRunMap();

    SortedMap<Integer, RawDataFile> rawDataFiles = new TreeMap<>();

    // If we are importing files, let's run RawDataImportModule
    if (importRawFiles) {
      List<File> filesToImport = new ArrayList<>();
      for (Entry<Integer, MsRun> entry : msrun.entrySet()) {
        File fileToImport = new File(entry.getValue().getLocation().getPath());

        if (fileToImport.exists() && fileToImport.canRead())
          filesToImport.add(fileToImport);
        else {
          // Check if the raw file exists in the same folder as the
          // mzTab file
          File checkFile = new File(inputFile.getParentFile(), fileToImport.getName());
          if (checkFile.exists() && checkFile.canRead())
            filesToImport.add(checkFile);
          else {
            // Append .gz & check again if file exists as a
            // workaround to .gz not getting preserved
            // when .mzML.gz importing
            checkFile = new File(inputFile.getParentFile(), fileToImport.getName() + ".gz");
            if (checkFile.exists() && checkFile.canRead())
              filesToImport.add(checkFile);
            else {
              // One more level of checking, appending .zip &
              // checking as a workaround
              checkFile = new File(inputFile.getParentFile(), fileToImport.getName() + ".zip");
              if (checkFile.exists() && checkFile.canRead())
                filesToImport.add(checkFile);
            }
          }

        }

      }

      RawDataImportModule RDI = MZmineCore.getModuleInstance(RawDataImportModule.class);
      ParameterSet rdiParameters = RDI.getParameterSetClass().newInstance();
      rdiParameters.getParameter(RawDataImportParameters.fileNames)
          .setValue(filesToImport.toArray(new File[0]));
      synchronized (underlyingTasks) {
        RDI.runModule(project, rdiParameters, underlyingTasks);
      }
      if (underlyingTasks.size() > 0) {
        MZmineCore.getTaskController().addTasks(underlyingTasks.toArray(new Task[0]));
      }

      // Wait until all raw data file imports have completed
      while (true) {
        if (isCanceled())
          return null;
        boolean tasksFinished = true;
        for (Task task : underlyingTasks) {
          if ((task.getStatus() == TaskStatus.WAITING)
              || (task.getStatus() == TaskStatus.PROCESSING))
            tasksFinished = false;
        }
        if (tasksFinished)
          break;
        Thread.sleep(1000);
      }

      /*
       * // Sort raw data files based on order in mzTab file MainWindow mainWindow = (MainWindow)
       * MZmineCore.getDesktop(); ProjectTree rawDataTree = mainWindow.getMainPanel()
       * .getRawDataTree(); final RawDataTreeModel treeModel = ((MZmineProjectImpl)
       * project).getRawDataTreeModel(); final DefaultMutableTreeNode rootNode =
       * treeModel.getRoot(); int[] selectedRows = new int[rootNode.getChildCount()]; for (int i =
       * 1; i < rootNode.getChildCount() + 1; i++) { selectedRows[i - 1] = i; } final
       * ArrayList<DefaultMutableTreeNode> selectedNodes = new ArrayList<DefaultMutableTreeNode>();
       * for (int row : selectedRows) { TreePath path = rawDataTree.getPathForRow(row);
       * DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path
       * .getLastPathComponent(); selectedNodes.add(selectedNode); }
       * 
       * // Reorder the nodes in the tree model based on order in mzTab // file int fileCounter = 0;
       * for (Entry<Integer, MsRun> entry : msrun.entrySet()) { fileCounter++; File f = new
       * File(entry.getValue().getLocation().getPath()); for (DefaultMutableTreeNode node :
       * selectedNodes) { if (node.toString().equals(f.getName())) {
       * treeModel.removeNodeFromParent(node); treeModel.insertNodeInto(node, rootNode, fileCounter
       * - 1); } } }
       */

    } else {
      finishedPercentage = 0.5;
    }

    // Find a matching RawDataFile for each MsRun entry
    for (Entry<Integer, MsRun> entry : msrun.entrySet()) {

      String rawFileName = new File(entry.getValue().getLocation().getPath()).getName();
      RawDataFile rawDataFile = null;

      // Check if we already have a RawDataFile of that name
      for (RawDataFile f : project.getDataFiles()) {
        if (f.getName().equals(rawFileName)) {
          rawDataFile = f;
          break;
        }
      }

      // If no data file of that name exists, create a dummy one
      if (rawDataFile == null) {
        RawDataFileWriter writer = MZmineCore.createNewFile(rawFileName);
        rawDataFile = writer.finishWriting();
        project.addFile(rawDataFile);
      }

      // Save a reference to the new raw data file
      rawDataFiles.put(entry.getKey(), rawDataFile);

    }
    return rawDataFiles;
  }

  private void importVariables(MZTabFile mzTabFile, Map<Integer, RawDataFile> rawDataFiles) {

    // Add sample parameters if available in mzTab file
    SortedMap<Integer, StudyVariable> variableMap = mzTabFile.getMetadata().getStudyVariableMap();

    if (variableMap.isEmpty())
      return;

    UserParameter<?, ?> newParameter =
        new StringParameter(inputFile.getName() + " study variable", "");
    project.addParameter(newParameter);

    for (Entry<Integer, StudyVariable> entry : variableMap.entrySet()) {

      // Stop the process if cancel() was called
      if (isCanceled())
        return;

      String variableValue = entry.getValue().getDescription();

      SortedMap<Integer, Assay> assayMap = entry.getValue().getAssayMap();

      for (Entry<Integer, RawDataFile> rawDataEntry : rawDataFiles.entrySet()) {

        RawDataFile rawData = rawDataEntry.getValue();
        Assay dataFileAssay = assayMap.get(rawDataEntry.getKey());
        if (dataFileAssay != null)
          project.setParameterValue(newParameter, rawData, variableValue);
      }

    }

  }

  private void importSmallMolecules(PeakList newPeakList, MZTabFile mzTabFile,
      Map<Integer, RawDataFile> rawDataFiles) {
    SortedMap<Integer, Assay> assayMap = mzTabFile.getMetadata().getAssayMap();
    Collection<SmallMolecule> smallMolecules = mzTabFile.getSmallMolecules();

    // Loop through SML data
    String formula, description, database, url = "";
    double mzExp = 0, abundance = 0, peak_mz = 0, peak_rt = 0, peak_height = 0, rtValue = 0;
    // int charge = 0;
    int rowCounter = 0;

    for (SmallMolecule smallMolecule : smallMolecules) {

      // Stop the process if cancel() was called
      if (isCanceled())
        return;

      rowCounter++;
      formula = smallMolecule.getChemicalFormula();
      // smile = smallMolecule.getSmiles();
      // inchiKey = smallMolecule.getInchiKey();
      description = smallMolecule.getDescription();
      // species = smallMolecule.getSpecies();
      database = smallMolecule.getDatabase();
      // dbVersion = smallMolecule.getDatabaseVersion();
      // reliability = smallMolecule.getReliability();

      if (smallMolecule.getURI() != null) {
        url = smallMolecule.getURI().toString();
      }

      String identifier = smallMolecule.getIdentifier().toString();
      SplitList<Double> rt = smallMolecule.getRetentionTime();
      // SplitList<Modification> modifications =
      // smallMolecule.getModifications();

      if (smallMolecule.getExpMassToCharge() != null) {
        mzExp = smallMolecule.getExpMassToCharge();
      }
      // if (smallMolecule.getCharge() != null) charge =
      // smallMolecule.getCharge();

      // Calculate average RT if multiple values are available
      if (rt != null && !rt.isEmpty()) {
        rtValue = DoubleMath.mean(rt);
      }

      if ((url != null) && (url.equals("null"))) {
        url = null;
      }
      if (identifier.equals("null")) {
        identifier = null;
      }
      if (description == null && identifier != null) {
        description = identifier;
      }

      // Add shared information to row
      SimplePeakListRow newRow = new SimplePeakListRow(rowCounter);
      newRow.setAverageMZ(mzExp);
      newRow.setAverageRT(rtValue);
      if (description != null) {
        SimplePeakIdentity newIdentity =
            new SimplePeakIdentity(description, formula, database, identifier, url);
        newRow.addPeakIdentity(newIdentity, false);
      }

      // Add raw data file entries to row
      for (Entry<Integer, RawDataFile> rawDataEntry : rawDataFiles.entrySet()) {

        RawDataFile rawData = rawDataEntry.getValue();
        Assay dataFileAssay = assayMap.get(rawDataEntry.getKey());

        abundance = 0;
        peak_mz = 0;
        peak_rt = 0;
        peak_height = 0;

        if (smallMolecule.getAbundanceColumnValue(dataFileAssay) != null) {
          abundance = smallMolecule.getAbundanceColumnValue(dataFileAssay);
        }

        if (smallMolecule.getOptionColumnValue(dataFileAssay, "peak_mz") != null) {
          peak_mz =
              Double.parseDouble(smallMolecule.getOptionColumnValue(dataFileAssay, "peak_mz"));
        } else {
          peak_mz = mzExp;
        }

        if (smallMolecule.getOptionColumnValue(dataFileAssay, "peak_rt") != null) {
          peak_rt =
              Double.parseDouble(smallMolecule.getOptionColumnValue(dataFileAssay, "peak_rt"));
        } else {
          peak_rt = rtValue;
        }

        if (smallMolecule.getOptionColumnValue(dataFileAssay, "peak_height") != null) {
          peak_height =
              Double.parseDouble(smallMolecule.getOptionColumnValue(dataFileAssay, "peak_height"));
        } else {
          peak_height = 0.0;
        }

        int scanNumbers[] = {};
        DataPoint finalDataPoint[] = new DataPoint[1];
        finalDataPoint[0] = new SimpleDataPoint(peak_mz, peak_height);
        int representativeScan = 0;
        int fragmentScan = 0;
        int[] allFragmentScans = new int[] {0};
        Range<Double> finalRTRange = Range.singleton(peak_rt);
        Range<Double> finalMZRange = Range.singleton(peak_mz);
        Range<Double> finalIntensityRange = Range.singleton(peak_height);
        FeatureStatus status = FeatureStatus.DETECTED;

        Feature peak = new SimpleFeature(rawData, peak_mz, peak_rt, peak_height, abundance,
            scanNumbers, finalDataPoint, status, representativeScan, fragmentScan, allFragmentScans,
            finalRTRange, finalMZRange, finalIntensityRange);

        if (abundance > 0) {
          newRow.addPeak(rawData, peak);
        }

      }

      // Add row to feature list
      newPeakList.addRow(newRow);

    }
  }

}
