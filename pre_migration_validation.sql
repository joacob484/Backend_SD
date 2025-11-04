-- Pre-Migration Validation Script for V21
-- Run this BEFORE the migration to understand current data state
-- Save the output for comparison after migration

\echo '========================================='
\echo 'PRE-MIGRATION VALIDATION - V21'
\echo 'Date:' `date`
\echo '========================================='
\echo ''

-- 1. Count inscriptions by estado
\echo '1. CURRENT INSCRIPTIONS BY ESTADO:'
\echo '-----------------------------------'
SELECT 
    estado, 
    COUNT(*) as total,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM inscripcion
GROUP BY estado
ORDER BY total DESC;

\echo ''
\echo 'EXPECTED AFTER MIGRATION:'
\echo '  - PENDIENTE → moves to solicitud_partido table'
\echo '  - ACEPTADO → stays in inscripcion table'
\echo '  - RECHAZADO → deleted permanently'
\echo '  - CANCELADO → deleted permanently'
\echo ''

-- 2. Total counts
\echo '2. TOTAL COUNTS:'
\echo '----------------'
SELECT 
    (SELECT COUNT(*) FROM inscripcion) as total_inscripciones,
    (SELECT COUNT(*) FROM inscripcion WHERE estado = 'ACEPTADO') as will_remain_in_inscripcion,
    (SELECT COUNT(*) FROM inscripcion WHERE estado = 'PENDIENTE') as will_move_to_solicitud_partido,
    (SELECT COUNT(*) FROM inscripcion WHERE estado IN ('RECHAZADO', 'CANCELADO')) as will_be_deleted;

\echo ''

-- 3. Check for data that might be lost
\echo '3. DATA LOSS ANALYSIS:'
\echo '----------------------'
\echo 'Records that will be PERMANENTLY DELETED (RECHAZADO/CANCELADO):'
SELECT 
    estado,
    COUNT(*) as count,
    STRING_AGG(DISTINCT partido_id::TEXT, ', ') as affected_matches
FROM inscripcion
WHERE estado IN ('RECHAZADO', 'CANCELADO')
GROUP BY estado;

\echo ''
\echo 'NOTE: These rejected/canceled records will be deleted.'
\echo 'If you need to preserve them, backup the table before migration!'
\echo ''

-- 4. Check for duplicates that might cause issues
\echo '4. DUPLICATE CHECK:'
\echo '-------------------'
SELECT 
    partido_id,
    usuario_id,
    COUNT(*) as duplicate_count,
    STRING_AGG(estado::TEXT, ', ') as estados
FROM inscripcion
GROUP BY partido_id, usuario_id
HAVING COUNT(*) > 1;

\echo ''
\echo 'If duplicates exist, review them before migration!'
\echo ''

-- 5. Sample of pending requests that will be migrated
\echo '5. SAMPLE OF PENDING REQUESTS (will move to solicitud_partido):'
\echo '----------------------------------------------------------------'
SELECT 
    i.id,
    i.partido_id,
    i.usuario_id,
    u.nombre || ' ' || u.apellido as usuario_nombre,
    p.tipo_partido,
    i.created_at,
    i.comentario
FROM inscripcion i
LEFT JOIN usuario u ON i.usuario_id = u.id
LEFT JOIN partido p ON i.partido_id = p.id
WHERE i.estado = 'PENDIENTE'
ORDER BY i.created_at DESC
LIMIT 10;

\echo ''

-- 6. Check for null values that might cause issues
\echo '6. NULL VALUE CHECK:'
\echo '--------------------'
SELECT 
    'partido_id nulls' as check_name,
    COUNT(*) as count
FROM inscripcion
WHERE partido_id IS NULL
UNION ALL
SELECT 
    'usuario_id nulls',
    COUNT(*)
FROM inscripcion
WHERE usuario_id IS NULL;

\echo ''
\echo 'All counts should be 0. If not, fix before migration!'
\echo ''

-- 7. Check foreign key relationships
\echo '7. FOREIGN KEY INTEGRITY CHECK:'
\echo '--------------------------------'
\echo 'Inscriptions with missing partido:'
SELECT COUNT(*) as inscriptions_with_missing_partido
FROM inscripcion i
LEFT JOIN partido p ON i.partido_id = p.id
WHERE p.id IS NULL;

\echo ''
\echo 'Inscriptions with missing usuario:'
SELECT COUNT(*) as inscriptions_with_missing_usuario
FROM inscripcion i
LEFT JOIN usuario u ON i.usuario_id = u.id
WHERE u.id IS NULL;

\echo ''
\echo 'Both counts should be 0. If not, clean orphaned records first!'
\echo ''

-- 8. Check current table structure
\echo '8. CURRENT INSCRIPCION TABLE STRUCTURE:'
\echo '---------------------------------------'
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'inscripcion'
ORDER BY ordinal_position;

\echo ''

-- 9. Check current indexes
\echo '9. CURRENT INDEXES ON INSCRIPCION:'
\echo '----------------------------------'
SELECT 
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'inscripcion'
ORDER BY indexname;

\echo ''

-- 10. Estimated migration impact
\echo '10. ESTIMATED MIGRATION IMPACT:'
\echo '--------------------------------'
WITH counts AS (
    SELECT 
        COUNT(*) as total_before,
        COUNT(*) FILTER (WHERE estado = 'ACEPTADO') as will_remain,
        COUNT(*) FILTER (WHERE estado = 'PENDIENTE') as will_move,
        COUNT(*) FILTER (WHERE estado IN ('RECHAZADO', 'CANCELADO')) as will_delete
    FROM inscripcion
)
SELECT 
    total_before,
    will_remain as inscripcion_after,
    will_move as solicitud_partido_after,
    will_delete as deleted_records,
    ROUND(will_remain * 100.0 / total_before, 2) as pct_remaining,
    ROUND(will_move * 100.0 / total_before, 2) as pct_moving,
    ROUND(will_delete * 100.0 / total_before, 2) as pct_deleted
FROM counts;

\echo ''
\echo '========================================='
\echo 'VALIDATION COMPLETE'
\echo '========================================='
\echo ''
\echo 'NEXT STEPS:'
\echo '1. Review the output above'
\echo '2. Create a backup: pg_dump -t inscripcion > inscripcion_backup.sql'
\echo '3. If everything looks good, run the application to trigger V21 migration'
\echo '4. After migration, run post_migration_validation.sql to verify'
\echo ''
