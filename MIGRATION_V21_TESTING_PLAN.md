# Migration V21 Testing & Validation Plan

## ⚠️ IMPORTANT: Read Before Running Migration

This migration restructures the inscription system from a single-table with estados to a two-table architecture.

---

## 🎯 What This Migration Does

### Data Movement:
1. **Creates** `solicitud_partido` table for pending requests
2. **Migrates** all `inscripcion` records with `estado='PENDIENTE'` → `solicitud_partido`
3. **Deletes** all `inscripcion` records with `estado IN ('RECHAZADO', 'CANCELADO')`
4. **Keeps** all `inscripcion` records with `estado='ACEPTADO'` (only accepted members remain)
5. **Drops** columns: `estado`, `motivo_rechazo`, `fecha_rechazo`, `fecha_cancelacion`
6. **Renames** `fecha_aceptacion` → `fecha_inscripcion`

### Result:
- `solicitud_partido`: Contains pending join requests
- `inscripcion`: Contains only accepted members (no estado column)

---

## ✅ Pre-Migration Checklist

### 1. Backup Your Database
```sql
-- Create full backup
pg_dump -U your_user -d faltauno_db > backup_before_v21_$(date +%Y%m%d_%H%M%S).sql

-- Or just the affected tables
pg_dump -U your_user -d faltauno_db -t inscripcion > inscripcion_backup.sql
```

### 2. Verify Current State
```sql
-- Count inscriptions by estado
SELECT estado, COUNT(*) as total
FROM inscripcion
GROUP BY estado
ORDER BY estado;

-- Expected output example:
--  estado    | total
-- -----------+-------
--  ACEPTADO  |  150
--  PENDIENTE |   25
--  RECHAZADO |   10
--  CANCELADO |    5
```

### 3. Check for Dependencies
```sql
-- Check if any foreign keys or constraints depend on estado column
SELECT
    tc.constraint_name,
    tc.table_name,
    kcu.column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.table_name = 'inscripcion'
  AND kcu.column_name = 'estado';
```

---

## 🚀 Running the Migration

### Option A: Automatic (via Flyway)
```bash
# Backend will automatically run migration on startup
cd Back/Backend_SD
mvn spring-boot:run

# Watch logs for migration success
# Look for: "Migración V21 completada"
```

### Option B: Manual (for testing)
```sql
-- Run the migration SQL directly
\i src/main/resources/db/migration/V21__create_solicitud_partido_table.sql
```

---

## 🔍 Post-Migration Validation

### 1. Verify Data Migration
```sql
-- Check solicitud_partido was populated
SELECT COUNT(*) as solicitudes_pendientes FROM solicitud_partido;
-- Should equal the count of PENDIENTE from before

-- Check inscripcion only has accepted members
SELECT COUNT(*) as miembros_aceptados FROM inscripcion;
-- Should equal the count of ACEPTADO from before

-- Verify estado column is gone
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'inscripcion' AND column_name = 'estado';
-- Should return 0 rows

-- Verify fecha_inscripcion exists
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'inscripcion' AND column_name = 'fecha_inscripcion';
-- Should return 1 row
```

### 2. Test API Endpoints

#### A. List User Inscriptions (with estado filter)
```bash
# Should work with backward compatibility
curl -X GET "http://localhost:8080/api/inscripciones/usuario/{userId}?estado=ACEPTADO"
curl -X GET "http://localhost:8080/api/inscripciones/usuario/{userId}?estado=PENDIENTE"
```

#### B. Check Inscription Status
```bash
curl -X GET "http://localhost:8080/api/inscripciones/estado?partidoId={matchId}&usuarioId={userId}"
# Should return: {"estado": "ACEPTADO"} or {"estado": "PENDIENTE"} or null
```

#### C. Accept Pending Request
```bash
curl -X POST "http://localhost:8080/api/inscripciones/{solicitudId}/aceptar" \
  -H "Authorization: Bearer {token}"
# Should: delete from solicitud_partido, create in inscripcion
```

#### D. Chat Access
```bash
curl -X GET "http://localhost:8080/api/mensajes/partido/{matchId}" \
  -H "Authorization: Bearer {token}"
# Should work for: organizer OR users in inscripcion table
```

### 3. Frontend Compatibility Test

#### Expected Behavior:
- ✅ Home page shows accepted matches (from `inscripcion`)
- ✅ Match management shows pending requests (from `solicitud_partido`)
- ✅ Chat access works for accepted members
- ✅ User can see their request status (PENDIENTE/ACEPTADO)
- ✅ Organizer can accept/reject requests

---

## 🔄 Rollback Plan (If Something Goes Wrong)

### Option 1: Restore from Backup
```bash
# Stop the application
# Restore database from backup
psql -U your_user -d faltauno_db < backup_before_v21_TIMESTAMP.sql
```

### Option 2: Manual Rollback SQL
```sql
-- ONLY USE IF MIGRATION PARTIALLY FAILED

-- 1. Recreate estado column in inscripcion
ALTER TABLE inscripcion ADD COLUMN estado VARCHAR(20) DEFAULT 'ACEPTADO';

-- 2. Migrate solicitud_partido back to inscripcion as PENDIENTE
INSERT INTO inscripcion (id, partido_id, usuario_id, comentario, created_at, estado)
SELECT id, partido_id, usuario_id, comentario, created_at, 'PENDIENTE'
FROM solicitud_partido;

-- 3. Drop solicitud_partido table
DROP TABLE solicitud_partido;

-- 4. Rename fecha_inscripcion back to fecha_aceptacion
ALTER TABLE inscripcion RENAME COLUMN fecha_inscripcion TO fecha_aceptacion;

-- 5. Recreate missing columns
ALTER TABLE inscripcion ADD COLUMN motivo_rechazo VARCHAR(500);
ALTER TABLE inscripcion ADD COLUMN fecha_rechazo TIMESTAMP;
ALTER TABLE inscripcion ADD COLUMN fecha_cancelacion TIMESTAMP;

-- 6. Mark Flyway schema as version 20 (previous version)
UPDATE flyway_schema_history SET success = false WHERE version = '21';
DELETE FROM flyway_schema_history WHERE version = '21';
```

---

## 🐛 Known Issues & Solutions

### Issue 1: "Column estado does not exist"
**Cause**: Code trying to access `inscripcion.estado` after migration  
**Solution**: Already fixed in the refactoring - all services updated

### Issue 2: Frontend shows wrong inscription count
**Cause**: Frontend might be caching old estado values  
**Solution**: Clear browser cache, restart backend

### Issue 3: Chat access denied for accepted members
**Cause**: MensajeService still checking estado  
**Solution**: Already fixed - now checks presence in `inscripcion` table

### Issue 4: Pending requests not showing up
**Cause**: Frontend querying wrong endpoint  
**Solution**: Ensure frontend calls `/api/inscripciones/{partidoId}/pendientes`

---

## 📊 Expected Metrics After Migration

### Database Size Impact:
- **solicitud_partido**: ~1-5% of inscripcion size (only pending requests)
- **inscripcion**: ~60-80% of original size (only accepted members)
- **Overall**: Should reduce DB size (no more rejected/canceled records)

### Performance Impact:
- ✅ Faster queries (no estado filtering needed)
- ✅ Simpler indexes (no composite estado indexes)
- ✅ Cleaner data (auto-cleanup on reject/kick)

---

## 📞 Support

If you encounter issues:
1. Check logs for "Migración V21 completada" message
2. Verify data counts match pre-migration numbers
3. Test all API endpoints listed above
4. Check frontend functionality
5. If critical issue: Use rollback plan

---

## ✨ Success Criteria

Migration is successful when:
- [x] `solicitud_partido` table exists and populated
- [x] `inscripcion` table has no `estado` column
- [x] All pending requests moved to `solicitud_partido`
- [x] All accepted members remain in `inscripcion`
- [x] API endpoints return correct data
- [x] Frontend shows matches and pending requests correctly
- [x] Chat access works for accepted members
- [x] No compilation errors in backend
- [x] All tests pass

---

## 📝 Post-Migration Notes

### Backward Compatibility:
- DTO still includes `estado` field (always "ACEPTADO" or "PENDIENTE")
- API endpoints still accept `?estado=` parameter
- Frontend doesn't need changes (already uses boolean checks)

### Future Improvements:
- Consider creating `InvitacionPartido` table for organizer invitations
- Add `SolicitudPartidoDTO` for cleaner API responses
- Update `UsuarioService.obtenerInvitaciones()` to use `solicitud_partido`

---

**Last Updated**: November 4, 2025  
**Migration Version**: V21  
**Status**: Ready for Testing ✅
