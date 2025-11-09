-- Rename report table to reports to match Hibernate naming convention

ALTER TABLE report RENAME TO reports;

-- Update index names to match new table name
ALTER INDEX idx_report_reporter RENAME TO idx_reports_reporter;
ALTER INDEX idx_report_reported_user RENAME TO idx_reports_reported_user;
ALTER INDEX idx_report_status RENAME TO idx_reports_status;
ALTER INDEX idx_report_created_at RENAME TO idx_reports_created_at;
ALTER INDEX idx_report_resolved_by RENAME TO idx_reports_resolved_by;
ALTER INDEX idx_report_active RENAME TO idx_reports_active;
ALTER INDEX idx_report_reporter_created RENAME TO idx_reports_reporter_created;

-- Update table comment
COMMENT ON TABLE reports IS 'User reports for moderation - tracks inappropriate behavior, harassment, etc.';
