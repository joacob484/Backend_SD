-- Post-Migration Validation Script for V21
-- Run this AFTER the migration to verify everything worked correctly
-- Compare with pre_migration_validation.sql output

\echo '========================================='
\echo 'POST-MIGRATION VALIDATION - V21'
\echo 'Date:' `date`
\echo '========================================='
\echo ''

-- 1. Verify new table exists
\echo '1. CHECK NEW TABLES:'
\echo '--------------------'
SELECT 
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_name IN ('inscripcion', 'solicitud_partido')
ORDER BY table_name;

\echo ''
\echo 'Expected: Both tables should exist'
\echo ''

-- 2. Count records in both tables
\echo '2. RECORD COUNTS AFTER MIGRATION:'
\echo '----------------------------------'
SELECT 
    (SELECT COUNT(*) FROM inscripcion) as inscripcion_count,
    (SELECT COUNT(*) FROM solicitud_partido) as solicitud_partido_count,
    (SELECT COUNT(*) FROM inscripcion) + (SELECT COUNT(*) FROM solicitud_partido) as total_active_records;

\echo ''
\echo 'COMPARE WITH PRE-MIGRATION:'
\echo '  inscripcion_count should = pre-migration ACEPTADO count'
\echo '  solicitud_partido_count should = pre-migration PENDIENTE count'
\echo '  total_active_records should = ACEPTADO + PENDIENTE (RECHAZADO/CANCELADO deleted)'
\echo ''

-- 3. Verify estado column is gone
\echo '3. VERIFY ESTADO COLUMN REMOVED:'
\echo '--------------------------------'
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'inscripcion' AND column_name = 'estado'
        ) THEN '❌ FAILED: estado column still exists'
        ELSE '✅ SUCCESS: estado column removed'
    END as status;

\echo ''

-- 4. Verify fecha_inscripcion exists
\echo '4. VERIFY FECHA_INSCRIPCION COLUMN:'
\echo '------------------------------------'
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'inscripcion' AND column_name = 'fecha_inscripcion'
        ) THEN '✅ SUCCESS: fecha_inscripcion exists'
        ELSE '❌ FAILED: fecha_inscripcion missing'
    END as status;

\echo ''

-- 5. Verify old columns are gone
\echo '5. VERIFY OLD COLUMNS REMOVED:'
\echo '-------------------------------'
SELECT 
    column_name,
    CASE 
        WHEN column_name IN ('estado', 'motivo_rechazo', 'fecha_rechazo', 'fecha_cancelacion', 'fecha_aceptacion')
        THEN '❌ SHOULD BE DELETED'
        ELSE 'OK'
    END as status
FROM information_schema.columns
WHERE table_name = 'inscripcion'
  AND column_name IN ('estado', 'motivo_rechazo', 'fecha_rechazo', 'fecha_cancelacion', 'fecha_aceptacion');

\echo ''
\echo 'Expected: No rows returned (all old columns deleted)'
\echo ''

-- 6. Check inscripcion table structure
\echo '6. NEW INSCRIPCION TABLE STRUCTURE:'
\echo '------------------------------------'
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'inscripcion'
ORDER BY ordinal_position;

\echo ''

-- 7. Check solicitud_partido table structure
\echo '7. SOLICITUD_PARTIDO TABLE STRUCTURE:'
\echo '--------------------------------------'
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'solicitud_partido'
ORDER BY ordinal_position;

\echo ''

-- 8. Verify indexes
\echo '8. NEW INDEXES:'
\echo '---------------'
\echo 'Inscripcion indexes:'
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'inscripcion'
ORDER BY indexname;

\echo ''
\echo 'Solicitud_partido indexes:'
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'solicitud_partido'
ORDER BY indexname;

\echo ''

-- 9. Sample data from solicitud_partido
\echo '9. SAMPLE SOLICITUD_PARTIDO RECORDS:'
\echo '-------------------------------------'
SELECT 
    sp.id,
    sp.partido_id,
    sp.usuario_id,
    u.nombre || ' ' || u.apellido as usuario_nombre,
    p.tipo_partido,
    sp.created_at
FROM solicitud_partido sp
LEFT JOIN usuario u ON sp.usuario_id = u.id
LEFT JOIN partido p ON sp.partido_id = p.id
ORDER BY sp.created_at DESC
LIMIT 5;

\echo ''

-- 10. Sample data from inscripcion
\echo '10. SAMPLE INSCRIPCION RECORDS:'
\echo '--------------------------------'
SELECT 
    i.id,
    i.partido_id,
    i.usuario_id,
    u.nombre || ' ' || u.apellido as usuario_nombre,
    p.tipo_partido,
    i.fecha_inscripcion,
    i.created_at
FROM inscripcion i
LEFT JOIN usuario u ON i.usuario_id = u.id
LEFT JOIN partido p ON i.partido_id = p.id
ORDER BY i.created_at DESC
LIMIT 5;

\echo ''

-- 11. Check for duplicates
\echo '11. DUPLICATE CHECK:'
\echo '--------------------'
\echo 'Duplicates in inscripcion:'
SELECT 
    partido_id,
    usuario_id,
    COUNT(*) as count
FROM inscripcion
GROUP BY partido_id, usuario_id
HAVING COUNT(*) > 1;

\echo ''
\echo 'Duplicates in solicitud_partido:'
SELECT 
    partido_id,
    usuario_id,
    COUNT(*) as count
FROM solicitud_partido
GROUP BY partido_id, usuario_id
HAVING COUNT(*) > 1;

\echo ''
\echo 'Expected: No rows (no duplicates)'
\echo ''

-- 12. Foreign key integrity
\echo '12. FOREIGN KEY INTEGRITY:'
\echo '--------------------------'
\echo 'Inscripcion orphans (missing partido):'
SELECT COUNT(*) as orphan_count
FROM inscripcion i
LEFT JOIN partido p ON i.partido_id = p.id
WHERE p.id IS NULL;

\echo ''
\echo 'Inscripcion orphans (missing usuario):'
SELECT COUNT(*) as orphan_count
FROM inscripcion i
LEFT JOIN usuario u ON i.usuario_id = u.id
WHERE u.id IS NULL;

\echo ''
\echo 'Solicitud_partido orphans (missing partido):'
SELECT COUNT(*) as orphan_count
FROM solicitud_partido sp
LEFT JOIN partido p ON sp.partido_id = p.id
WHERE p.id IS NULL;

\echo ''
\echo 'Solicitud_partido orphans (missing usuario):'
SELECT COUNT(*) as orphan_count
FROM solicitud_partido sp
LEFT JOIN usuario u ON sp.usuario_id = u.id
WHERE u.id IS NULL;

\echo ''
\echo 'All counts should be 0'
\echo ''

-- 13. Check Flyway migration status
\echo '13. FLYWAY MIGRATION STATUS:'
\echo '----------------------------'
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    installed_on,
    execution_time,
    success
FROM flyway_schema_history
WHERE version >= '20'
ORDER BY installed_rank;

\echo ''

-- 14. Final verification checklist
\echo '14. FINAL VERIFICATION CHECKLIST:'
\echo '----------------------------------'
SELECT 
    CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'solicitud_partido')
        THEN '✅' ELSE '❌' END || ' solicitud_partido table exists' as check_1
UNION ALL
SELECT 
    CASE WHEN NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'inscripcion' AND column_name = 'estado')
        THEN '✅' ELSE '❌' END || ' estado column removed from inscripcion'
UNION ALL
SELECT 
    CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'inscripcion' AND column_name = 'fecha_inscripcion')
        THEN '✅' ELSE '❌' END || ' fecha_inscripcion column exists'
UNION ALL
SELECT 
    CASE WHEN (SELECT COUNT(*) FROM solicitud_partido) > 0 OR (SELECT COUNT(*) FROM inscripcion WHERE created_at > NOW() - INTERVAL '1 hour') = 0
        THEN '✅' ELSE '⚠️' END || ' Data migrated to solicitud_partido (or no pending requests existed)'
UNION ALL
SELECT 
    CASE WHEN (SELECT COUNT(*) FROM inscripcion) > 0
        THEN '✅' ELSE '⚠️' END || ' Inscripcion table has records (accepted members)';

\echo ''
\echo '========================================='
\echo 'VALIDATION COMPLETE'
\echo '========================================='
\echo ''
\echo 'IF ALL CHECKS PASSED (✅):'
\echo '  Migration V21 successful!'
\echo '  Test the application endpoints now.'
\echo ''
\echo 'IF ANY CHECKS FAILED (❌):'
\echo '  Review the output above'
\echo '  Check application logs'
\echo '  Consider rolling back if critical'
\echo ''
\echo 'NEXT STEPS:'
\echo '1. Test API endpoints (see MIGRATION_V21_TESTING_PLAN.md)'
\echo '2. Test frontend functionality'
\echo '3. Monitor application logs for errors'
\echo '4. If everything works, delete backup files'
\echo ''
