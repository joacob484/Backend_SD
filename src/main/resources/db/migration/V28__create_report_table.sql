-- Create report table for user reporting system

CREATE TABLE IF NOT EXISTS report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    reported_user_id UUID NOT NULL,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by UUID,
    action_taken VARCHAR(30),
    resolution_notes TEXT,
    
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_reported_user FOREIGN KEY (reported_user_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_resolved_by FOREIGN KEY (resolved_by) REFERENCES usuario(id) ON DELETE SET NULL,
    
    CONSTRAINT chk_report_reason CHECK (reason IN (
        'COMPORTAMIENTO_INAPROPIADO',
        'LENGUAJE_OFENSIVO',
        'SPAM',
        'SUPLANTACION_IDENTIDAD',
        'ACOSO',
        'CONTENIDO_INAPROPIADO',
        'NO_APARECE_PARTIDOS',
        'JUGADOR_VIOLENTO',
        'OTRO'
    )),
    
    CONSTRAINT chk_report_status CHECK (status IN (
        'PENDING',
        'UNDER_REVIEW',
        'RESOLVED',
        'DISMISSED'
    )),
    
    CONSTRAINT chk_report_action CHECK (action_taken IS NULL OR action_taken IN (
        'NO_ACTION',
        'WARNING_SENT',
        'USER_BANNED',
        'CONTENT_REMOVED',
        'FALSE_REPORT'
    ))
);

-- Indexes for query performance
CREATE INDEX idx_report_reporter ON report(reporter_id);
CREATE INDEX idx_report_reported_user ON report(reported_user_id);
CREATE INDEX idx_report_status ON report(status);
CREATE INDEX idx_report_created_at ON report(created_at DESC);
CREATE INDEX idx_report_resolved_by ON report(resolved_by) WHERE resolved_by IS NOT NULL;

-- Index for finding active reports (pending or under review)
CREATE INDEX idx_report_active ON report(status) WHERE status IN ('PENDING', 'UNDER_REVIEW');

-- Index for monthly report count queries (for rate limiting)
CREATE INDEX idx_report_reporter_created ON report(reporter_id, created_at DESC);

-- Comments
COMMENT ON TABLE report IS 'User reports for moderation - tracks inappropriate behavior, harassment, etc.';
COMMENT ON COLUMN report.reason IS 'Type of violation being reported';
COMMENT ON COLUMN report.status IS 'Current status of the report';
COMMENT ON COLUMN report.action_taken IS 'Action taken by admin after reviewing the report';
