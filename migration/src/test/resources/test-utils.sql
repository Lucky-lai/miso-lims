-- Useful MySql procedures for testing

DELIMITER //

DROP PROCEDURE IF EXISTS showCounts//
CREATE PROCEDURE showCounts ()
BEGIN
    
    SELECT 'Projects' AS 'Entity',COUNT(*) AS 'Rows' FROM Project 
    UNION SELECT 'Samples',COUNT(*) FROM Sample 
    UNION SELECT 'DetailedSample',COUNT(*) FROM DetailedSample 
    UNION SELECT 'Identities',COUNT(*) FROM Identity 
    UNION SELECT 'SampleTissues',COUNT(*) FROM SampleTissue 
    UNION SELECT 'SampleTissueProcessings',COUNT(*) FROM SampleTissueProcessing 
    UNION SELECT 'SampleCVSlides',COUNT(*) FROM SampleCVSlide 
    UNION SELECT 'SampleLCMTubes',COUNT(*) FROM SampleLCMTube 
    UNION SELECT 'SampleTissues',COUNT(*) FROM SampleTissue
    UNION SELECT 'SampleChangeLogs',COUNT(*) FROM SampleChangeLog
    UNION SELECT 'SampleQCs',COUNT(*) FROM SampleQC
    UNION SELECT 'Sample_Notes',COUNT(*) FROM Sample_Note
    UNION SELECT 'SampleChangeLogs',COUNT(*) FROM SampleChangeLog
    UNION SELECT 'Analyte Stocks',COUNT(*) FROM SampleStock
    UNION SELECT 'Analyte Aliquots',COUNT(*) FROM SampleAliquot
    UNION SELECT 'Libraries',COUNT(*) FROM Library
    UNION SELECT 'DetailedLibrary',COUNT(*) FROM DetailedLibrary
    UNION SELECT 'LibraryChangeLogs',COUNT(*) FROM LibraryChangeLog
    UNION SELECT 'LibraryDilutions',COUNT(*) FROM LibraryDilution
    UNION SELECT 'Pool_Elements',COUNT(*) FROM Pool_Elements
    UNION SELECT 'Pools',COUNT(*) FROM Pool
    UNION SELECT 'Partitions',COUNT(*) FROM _Partition
    UNION SELECT 'SequencerPartitionContainer_Partitions',COUNT(*) FROM SequencerPartitionContainer_Partition
    UNION SELECT 'SequencerPartitionContainers',COUNT(*) FROM SequencerPartitionContainer
    UNION SELECT 'Run_SequencerPartitionContainers',COUNT(*) FROM Run_SequencerPartitionContainer
    UNION SELECT 'Runs',COUNT(*) FROM Run
    UNION SELECT 'Status',COUNT(*) FROM Status;
    
END//

DROP PROCEDURE IF EXISTS clearData//
CREATE PROCEDURE clearData ()
BEGIN
    
    DELETE FROM SequencerPartitionContainerChangeLog;
    DELETE FROM Run_SequencerPartitionContainer;
    DELETE FROM SequencerPartitionContainer_Partition;
    DELETE FROM SequencerPartitionContainer;
    DELETE FROM _Partition;
    DELETE FROM RunChangeLog;
    DELETE FROM Run;
    DELETE FROM ExperimentChangeLog;
    DELETE FROM Experiment;
    DELETE FROM Pool_Note;
    DELETE FROM PoolOrder;
    DELETE FROM Pool_Dilution;
    DELETE FROM PoolChangeLog;
    DELETE FROM Pool;
    DELETE FROM LibraryDilution;
    DELETE FROM DetailedLibrary;
    DELETE FROM Library_Note;
    DELETE FROM Library_Index;
    DELETE FROM LibraryChangeLog;
    DELETE FROM Library;
    DELETE FROM SampleQC;
    DELETE FROM Sample_Note;
    DELETE FROM Identity;
    DELETE FROM SampleTissue;
    DELETE FROM SampleCVSlide;
    DELETE FROM SampleLCMTube;
    DELETE FROM SampleTissueProcessing;
    DELETE FROM SampleStock;
    DELETE FROM SampleAliquot;
    DELETE FROM DetailedSample;
    DELETE FROM SampleChangeLog;
    DELETE FROM Sample;
    DELETE FROM SampleNumberPerProject;
    DELETE FROM Subproject;
    DELETE FROM StudyChangeLog;
    DELETE FROM Study;
    DELETE FROM ProjectOverview_Note;
    DELETE FROM ProjectOverview;
    DELETE FROM Project;
    DELETE FROM Note;
    
END//

DELIMITER ;
