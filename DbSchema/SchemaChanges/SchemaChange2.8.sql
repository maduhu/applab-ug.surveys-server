-- Change the submission table to allow for duplicate submissions to be caught by the DB
alter table ZebraSurveySubmissions change column result_hash result_hash_deprecated varchar(128) NOT NULL default '';
alter table ZebraSurveySubmissions add column result_hash varchar(128) NOT NULL;
update survey set `result_hash` =  `id`;
alter table survey add unique key ZebraSurveySubmissions_uidx_1 (result_hash);

-- Add a column to record the size in bytes of a submitted survey.
ALTER TABLE `ZebraSurveySubmissions` ADD `submission_size` BIGINT(15) NOT NULL default 0;