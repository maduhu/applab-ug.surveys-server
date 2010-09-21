-- Add a column to record the size in bytes of a submitted survey.
ALTER TABLE `ZebraSurveySubmissions` ADD `submission_size` BIGINT(15) NOT NULL default 0;