alter table zebrasurveysubmissions ADD COLUMN customer_care_status enum('Not Reviewed', 'First Level Approved', 'Flagged') default 'Not Reviewed';
alter table zebrasurveysubmissions MODIFY survey_status enum('Not Reviewed', 'Approved', 'Rejected', 'Pending', 'Flagged') NOT NULL default 'Not Reviewed';
alter table zebrasurveysubmissions ADD COLUMN is_draft enum('Y', 'N') default 'N';