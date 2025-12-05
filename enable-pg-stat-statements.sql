-- Habilitar pg_stat_statements en Cloud SQL
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Verificar que está habilitada
SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';

-- Verificar que funciona
SELECT count(*) FROM pg_stat_statements;
