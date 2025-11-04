# 🔄 Migration V21: Estado System Refactoring

## Quick Start Guide

### 🚨 BEFORE YOU START

**1. BACKUP YOUR DATABASE**
```bash
pg_dump -U your_user -d faltauno_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

**2. RUN PRE-MIGRATION VALIDATION**
```bash
psql -U your_user -d faltauno_db -f pre_migration_validation.sql > pre_migration_report.txt
```

**3. REVIEW THE OUTPUT**
- Check `pre_migration_report.txt` for any issues
- Note the counts for comparison after migration

---

## 🎯 Step-by-Step Migration Process

### Step 1: Pre-Migration Validation (5 minutes)

```bash
cd Back/Backend_SD

# Run validation script
psql -U your_user -d faltauno_db -f pre_migration_validation.sql > pre_migration_report.txt

# Review the output
cat pre_migration_report.txt
```

**What to look for:**
- Total inscriptions count
- Breakdown by estado (ACEPTADO, PENDIENTE, RECHAZADO, CANCELADO)
- Any duplicates or orphaned records
- Foreign key integrity

**Example output:**
```
Total: 200 inscriptions
  - ACEPTADO: 150 (75%) → will stay in inscripcion
  - PENDIENTE: 30 (15%) → will move to solicitud_partido
  - RECHAZADO: 15 (7.5%) → will be deleted
  - CANCELADO: 5 (2.5%) → will be deleted
```

### Step 2: Create Backup (2 minutes)

```bash
# Full database backup
pg_dump -U your_user -d faltauno_db > backup_before_v21_$(date +%Y%m%d_%H%M%S).sql

# Or just inscripcion table
pg_dump -U your_user -d faltauno_db -t inscripcion > inscripcion_backup.sql
```

### Step 3: Stop Application (1 minute)

```bash
# Stop the backend if running
# Ctrl+C or:
pkill -f "spring-boot"
```

### Step 4: Run Migration (Automatic - 30 seconds)

```bash
# Start the application - Flyway will run V21 automatically
mvn clean spring-boot:run

# Or with Maven wrapper
./mvnw clean spring-boot:run
```

**Watch for these log messages:**
```
INFO  FlywayMigrationStrategy : Migrating schema to version 21
INFO  FlywayMigrationStrategy : Successfully applied 1 migration to schema "public"
NOTICE: Migración V21 completada:
NOTICE:   - 30 solicitudes movidas a solicitud_partido
NOTICE:   - 150 inscripciones activas (aceptadas)
NOTICE:   - Columna estado eliminada de inscripcion
```

### Step 5: Post-Migration Validation (5 minutes)

```bash
# Run validation script
psql -U your_user -d faltauno_db -f post_migration_validation.sql > post_migration_report.txt

# Review the output
cat post_migration_report.txt
```

**Verify these checks pass:**
- ✅ solicitud_partido table exists
- ✅ estado column removed from inscripcion
- ✅ fecha_inscripcion column exists
- ✅ Record counts match expectations
- ✅ No orphaned records
- ✅ No duplicates

### Step 6: Test Application (10 minutes)

#### A. Test Backend Endpoints

```bash
# Get your auth token first
TOKEN="your_jwt_token_here"

# 1. List user inscriptions (should work with estado filter)
curl -X GET "http://localhost:8080/api/inscripciones/usuario/{userId}?estado=ACEPTADO" \
  -H "Authorization: Bearer $TOKEN"

# 2. List pending requests (organizer only)
curl -X GET "http://localhost:8080/api/inscripciones/{partidoId}/pendientes" \
  -H "Authorization: Bearer $TOKEN"

# 3. Check inscription status
curl -X GET "http://localhost:8080/api/inscripciones/estado?partidoId={matchId}&usuarioId={userId}" \
  -H "Authorization: Bearer $TOKEN"

# 4. Access chat (should work for inscrito members)
curl -X GET "http://localhost:8080/api/mensajes/partido/{matchId}" \
  -H "Authorization: Bearer $TOKEN"
```

#### B. Test Frontend

1. **Login** and navigate to home page
   - ✅ Should see your accepted matches

2. **Open a match you're in**
   - ✅ Should see match details
   - ✅ Should be able to access chat

3. **As organizer, open match management**
   - ✅ Should see pending join requests
   - ✅ Should be able to accept/reject

4. **Request to join a new match**
   - ✅ Request should be created
   - ✅ Organizer should see it in pending requests

5. **Organizer accepts your request**
   - ✅ You should get notification
   - ✅ Match should appear in your home
   - ✅ You should be able to access chat

### Step 7: Monitor for Issues (Ongoing)

**Check application logs:**
```bash
tail -f logs/application.log | grep -i "error\|exception\|inscripcion"
```

**Common issues and solutions:**

| Issue | Solution |
|-------|----------|
| "Column estado does not exist" | Already fixed in code refactoring |
| Chat access denied | Check `MensajeService.validarAccesoChat` - should only check presence in inscripcion |
| Pending requests not showing | Ensure frontend calls correct endpoint |
| Count mismatch | Re-run post_migration_validation.sql |

---

## 📊 What Changed?

### Before (Single Table with Estados):
```
inscripcion
├── id
├── partido_id
├── usuario_id
├── estado (PENDIENTE/ACEPTADO/RECHAZADO/CANCELADO) ❌
├── comentario
├── fecha_aceptacion
├── fecha_rechazo ❌
├── fecha_cancelacion ❌
├── motivo_rechazo ❌
└── created_at
```

### After (Two Tables):
```
solicitud_partido (new)
├── id
├── partido_id
├── usuario_id
├── comentario
└── created_at

inscripcion (simplified)
├── id
├── partido_id
├── usuario_id
├── comentario
├── fecha_inscripcion (renamed from fecha_aceptacion)
└── created_at
```

---

## 🔄 Rollback Instructions (Emergency Only)

**Only use if migration failed or critical issues found!**

### Option 1: Restore from Backup (Safest)
```bash
# Stop application
pkill -f "spring-boot"

# Restore database
psql -U your_user -d faltauno_db < backup_before_v21_TIMESTAMP.sql

# Mark migration as failed in Flyway
psql -U your_user -d faltauno_db -c "DELETE FROM flyway_schema_history WHERE version = '21';"

# Restart application
mvn spring-boot:run
```

### Option 2: Manual Rollback (Advanced)
```bash
# Stop application
# Run rollback commands from MIGRATION_V21_TESTING_PLAN.md
# Restart application
```

---

## ✅ Success Criteria

Migration is successful when:

- [ ] Pre-migration validation completed without errors
- [ ] Database backup created
- [ ] Migration ran without errors (check logs)
- [ ] Post-migration validation passed all checks
- [ ] `solicitud_partido` table exists and populated
- [ ] `inscripcion` table has no `estado` column
- [ ] Record counts match expectations (ACEPTADO stay, PENDIENTE moved)
- [ ] All API endpoints return correct data
- [ ] Frontend displays matches correctly
- [ ] Chat access works for accepted members
- [ ] Can accept/reject pending requests
- [ ] Can request to join new matches
- [ ] No errors in application logs

---

## 📞 Troubleshooting

### Issue: Migration didn't run
**Symptoms:** No V21 in `flyway_schema_history`
**Solution:**
```bash
# Check Flyway status
psql -U your_user -d faltauno_db -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Manually run migration if needed
psql -U your_user -d faltauno_db -f src/main/resources/db/migration/V21__create_solicitud_partido_table.sql
```

### Issue: Compilation errors
**Symptoms:** Backend won't start, Java errors
**Solution:**
```bash
# Clean and rebuild
mvn clean install -DskipTests

# If still issues, check that all services were updated:
# - InscripcionService ✅
# - PartidoService ✅
# - MensajeService ✅
# - InscripcionRepository ✅
# - InscripcionMapper ✅
```

### Issue: Frontend shows wrong data
**Symptoms:** Missing matches, wrong counts
**Solution:**
```bash
# Clear browser cache
# Hard refresh (Ctrl+Shift+R)
# Check browser console for API errors
# Verify backend logs
```

### Issue: Data loss detected
**Symptoms:** Missing inscriptions or requests
**Solution:**
```bash
# IMMEDIATELY restore from backup
psql -U your_user -d faltauno_db < backup_before_v21_TIMESTAMP.sql

# Report issue with:
# - Pre-migration report
# - Post-migration report
# - Application logs
```

---

## 📈 Expected Results

### Database Changes:
- **solicitud_partido**: New table with pending requests (~15% of original inscripcion)
- **inscripcion**: Simplified table with only accepted members (~75% of original)
- **Total size**: Reduced by ~10-25% (no more rejected/canceled records)

### Performance:
- ✅ Faster queries (no estado filtering)
- ✅ Simpler indexes
- ✅ Cleaner data model

### Functionality:
- ✅ Same features as before
- ✅ Better separation of concerns
- ✅ Easier to understand and maintain

---

## 📚 Additional Resources

- **Full Testing Plan**: `MIGRATION_V21_TESTING_PLAN.md`
- **Pre-Migration Check**: `pre_migration_validation.sql`
- **Post-Migration Check**: `post_migration_validation.sql`
- **Migration SQL**: `src/main/resources/db/migration/V21__create_solicitud_partido_table.sql`

---

## ⏱️ Estimated Timeline

| Step | Duration | Can Skip? |
|------|----------|-----------|
| Pre-validation | 5 min | No |
| Backup | 2 min | No |
| Migration | 30 sec | No |
| Post-validation | 5 min | No |
| API testing | 10 min | Optional |
| Frontend testing | 10 min | Optional |
| **Total** | **~35 min** | - |

---

**Last Updated**: November 4, 2025  
**Status**: Ready for Production ✅  
**Backward Compatible**: Yes  
**Requires Frontend Changes**: No

---

## 🎉 After Successful Migration

Once everything is verified:

1. Keep backup for 7 days (in case of delayed issues)
2. Update documentation if needed
3. Monitor application for 24-48 hours
4. Celebrate! You've successfully refactored the inscription system! 🚀

---

**Need help?** Check logs first, then review the troubleshooting section above.
