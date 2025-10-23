UPDATE flyway_schema_history SET checksum = -178316961 WHERE version = '8';
SELECT version, description, checksum, installed_on FROM flyway_schema_history WHERE version = '8';
