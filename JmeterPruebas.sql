USE gramola;

-- 1. Guardo los tokens antiguos de usuarios jmeter
DROP TEMPORARY TABLE IF EXISTS tmp_old_jmeter_tokens;

CREATE TEMPORARY TABLE tmp_old_jmeter_tokens AS
SELECT creation_token_id AS token_id
FROM `user`
WHERE email LIKE 'jmeter%';

-- 2. Borro usuarios jmeter antiguos
DELETE FROM `user`
WHERE email LIKE 'jmeter%';

-- 3. Borro tokens antiguos asociados
DELETE FROM token
WHERE id IN (
  SELECT token_id
  FROM tmp_old_jmeter_tokens
);

DROP TEMPORARY TABLE IF EXISTS tmp_old_jmeter_tokens;


-- 4. Creo tabla temporal
DROP TEMPORARY TABLE IF EXISTS tmp_jmeter_users;

CREATE TEMPORARY TABLE tmp_jmeter_users (
  num INT NOT NULL,
  email VARCHAR(255) NOT NULL,
  bar_name VARCHAR(120) NOT NULL,
  token_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (num)
);


-- 5. Genero 1000 números exactos: 0 hasta 999
INSERT INTO tmp_jmeter_users (num, email, bar_name, token_id)
SELECT
  n,
  CONCAT('jmeter', LPAD(n, 4, '0'), '@gramola.test') AS email,
  CONCAT('Bar JMeter ', LPAD(n, 4, '0')) AS bar_name,
  UUID() AS token_id
FROM (
  SELECT
    unidades.n
    + decenas.n * 10
    + centenas.n * 100 AS n
  FROM
    (
      SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) unidades
  CROSS JOIN
    (
      SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) decenas
  CROSS JOIN
    (
      SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) centenas
) numeros
WHERE n BETWEEN 0 AND 999
ORDER BY n;


-- 6. Compruebo que la tabla temporal tiene 1000 antes de insertar
SELECT COUNT(*) AS total_temporal
FROM tmp_jmeter_users;


-- 7. Inserto tokens ya usados
INSERT INTO token (id, creation_time, use_time, expires_time)
SELECT
  token_id,
  NOW(6),
  NOW(6),
  DATE_ADD(NOW(6), INTERVAL 30 MINUTE)
FROM tmp_jmeter_users;


-- 8. Inserto los 1000 usuarios
INSERT INTO `user`
(
  email,
  bar_name,
  pwd,
  client_id,
  client_secret,
  creation_token_id,
  reset_token_id,
  subscription_paid,
  song_price_cents
)
SELECT
  email,
  bar_name,
  'e1073afc',
  'f98afbff41e477db914ed341ab84a89',
  '490290dc4f9d09bd9eae77285ea46',
  token_id,
  NULL,
  b'1',
  88
FROM tmp_jmeter_users;


DROP TEMPORARY TABLE IF EXISTS tmp_jmeter_users;


-- 9. Comprobación final: debe salir 1000
SELECT COUNT(*) AS total_usuarios_jmeter
FROM `user`
WHERE email LIKE 'jmeter%';


-- 10. Comprobación del primero y último
SELECT email, bar_name, pwd, subscription_paid
FROM `user`
WHERE email IN (
  'jmeter0000@gramola.test',
  'jmeter0999@gramola.test'
)
ORDER BY email;