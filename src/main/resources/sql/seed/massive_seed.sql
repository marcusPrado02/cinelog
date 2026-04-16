/* ============================================================
   SEED MASSIVO — Mais usuarios, watch_entries variados,
   e recalculo do read-model media_popularity.

   Objetivo: gerar ~2500+ watch_entries com ratings de 1 a 10
   distribuidos de forma realista, e views variados por media.

   Banco: MySQL 8+ (apos rodar seeds 1-7 existentes)
   ============================================================ */

START TRANSACTION;

-- ========== 1. MAIS USUARIOS (25 novos) ==========
-- Senha padrao: 'Senha@123' em BCrypt (mesma dos usuarios seed originais)
SET @default_hash = '$2a$10$KmXjCJefz9A2.YLgzloHiuLxgc5jIM1FONpniE7jOq/GLNgQXTYl6';

INSERT INTO users (name, email, password_hash) VALUES
  ('Fernanda Costa',    'fernanda.costa@email.com',   @default_hash),
  ('Rafael Oliveira',   'rafael.oliveira@email.com',  @default_hash),
  ('Camila Souza',      'camila.souza@email.com',     @default_hash),
  ('Bruno Almeida',     'bruno.almeida@email.com',    @default_hash),
  ('Juliana Ferreira',  'juliana.ferreira@email.com', @default_hash),
  ('Gustavo Lima',      'gustavo.lima@email.com',     @default_hash),
  ('Larissa Mendes',    'larissa.mendes@email.com',   @default_hash),
  ('Diego Rocha',       'diego.rocha@email.com',      @default_hash),
  ('Isabela Martins',   'isabela.martins@email.com',  @default_hash),
  ('Thiago Barbosa',    'thiago.barbosa@email.com',   @default_hash),
  ('Patricia Ribeiro',  'patricia.ribeiro@email.com', @default_hash),
  ('Lucas Carvalho',    'lucas.carvalho@email.com',   @default_hash),
  ('Amanda Gomes',      'amanda.gomes@email.com',     @default_hash),
  ('Rodrigo Pereira',   'rodrigo.pereira@email.com',  @default_hash),
  ('Beatriz Araujo',    'beatriz.araujo@email.com',   @default_hash),
  ('Felipe Nunes',      'felipe.nunes@email.com',     @default_hash),
  ('Carolina Dias',     'carolina.dias@email.com',    @default_hash),
  ('Pedro Monteiro',    'pedro.monteiro@email.com',   @default_hash),
  ('Mariana Teixeira',  'mariana.teixeira@email.com', @default_hash),
  ('Henrique Castro',   'henrique.castro@email.com',  @default_hash),
  ('Vanessa Cardoso',   'vanessa.cardoso@email.com',  @default_hash),
  ('Andre Correia',     'andre.correia@email.com',    @default_hash),
  ('Natalia Vieira',    'natalia.vieira@email.com',   @default_hash),
  ('Gabriel Santos',    'gabriel.santos@email.com',   @default_hash),
  ('Leticia Moreira',   'leticia.moreira@email.com',  @default_hash)
ON DUPLICATE KEY UPDATE name = VALUES(name);


-- ========== 2. POOL DE COMENTARIOS VARIADOS ==========
DROP TEMPORARY TABLE IF EXISTS tmp_comments_massive;
CREATE TEMPORARY TABLE tmp_comments_massive (id INT PRIMARY KEY AUTO_INCREMENT, txt VARCHAR(255));
INSERT INTO tmp_comments_massive (txt) VALUES
  ('Obra-prima absoluta!'),
  ('Muito bom, assistiria de novo.'),
  ('Excelente atuacao e direcao.'),
  ('Trilha sonora incrivel.'),
  ('Roteiro confuso e arrastado.'),
  ('Final surpreendente!'),
  ('Visual espetacular, mas raso.'),
  ('Classico indiscutivel.'),
  ('Esperava mais, achei mediano.'),
  ('Fotografia perfeita.'),
  ('Um dos meus favoritos de todos os tempos.'),
  ('Nao entendi o hype, achei fraco.'),
  ('Dialogo brilhante do inicio ao fim.'),
  ('Cena final inesquecivel.'),
  ('Amei cada minuto.'),
  ('Muito longo, perdi o interesse na metade.'),
  ('Atuacoes fracas, mas a premissa e boa.'),
  ('Horrivel, nao recomendo.'),
  ('Razoavel, da pra assistir uma vez.'),
  ('Genial! Precisa de mais de uma sessao pra absorver.'),
  ('Bom entretenimento pipoca, nada mais.'),
  ('Me fez chorar, que historia linda.'),
  ('Achei pretensioso demais.'),
  ('Bom, mas o original e melhor.'),
  ('Perdi tempo assistindo isso.'),
  ('Surpreendentemente bom!'),
  ('Regular, nem bom nem ruim.'),
  ('Acao demais, substancia de menos.'),
  ('Recomendo pra quem gosta do genero.'),
  ('Nota 10 sem pensar.');


-- ========== 3. WATCH ENTRIES EM MASSA ==========
-- Estrategia: cross join users x movies com probabilidades variadas
-- e ratings distribuidos realisticamente (bell curve centrada em 5-6)

-- 3a. Tabelas temporarias
DROP TEMPORARY TABLE IF EXISTS tmp_all_users;
CREATE TEMPORARY TABLE tmp_all_users AS SELECT id FROM users;

DROP TEMPORARY TABLE IF EXISTS tmp_all_movies;
CREATE TEMPORARY TABLE tmp_all_movies AS SELECT id FROM media WHERE type = 'MOVIE';

DROP TEMPORARY TABLE IF EXISTS tmp_all_series;
CREATE TEMPORARY TABLE tmp_all_series AS SELECT id FROM media WHERE type = 'SERIES';

-- ========== 3b. FILMES — alta probabilidade (65%) ==========
-- Rating: soma de 3 RAND() / 3 gera bell curve centrada em ~5
INSERT IGNORE INTO watch_entry (user_id, media_id, rating, comment, watched_at, status_type)
SELECT
  u.id,
  m.id,
  LEAST(10, GREATEST(1, FLOOR(1 + (RAND()+RAND()+RAND())/3.0 * 9.99))),
  (SELECT txt FROM tmp_comments_massive ORDER BY RAND() LIMIT 1),
  DATE_ADD('2023-01-01', INTERVAL FLOOR(RAND() * 1170) DAY),
  'COMPLETED'
FROM tmp_all_users u
CROSS JOIN tmp_all_movies m
WHERE RAND() < 0.65
ORDER BY RAND();


-- ========== 3c. SERIES — media probabilidade (40%) ==========
INSERT IGNORE INTO watch_entry (user_id, media_id, rating, comment, watched_at, status_type)
SELECT
  u.id,
  m.id,
  LEAST(10, GREATEST(1, FLOOR(1 + (RAND()+RAND()+RAND())/3.0 * 9.99))),
  (SELECT txt FROM tmp_comments_massive ORDER BY RAND() LIMIT 1),
  DATE_ADD('2023-06-01', INTERVAL FLOOR(RAND() * 1020) DAY),
  CASE
    WHEN RAND() < 0.15 THEN 'WATCHING'
    WHEN RAND() < 0.05 THEN 'DROPPED'
    ELSE 'COMPLETED'
  END
FROM tmp_all_users u
CROSS JOIN tmp_all_series m
WHERE RAND() < 0.40
ORDER BY RAND();


-- ========== 3d. ENTRADAS RECENTES (ultimos 14 dias) ==========
-- Garante que trending report tenha dados frescos
INSERT IGNORE INTO watch_entry (user_id, media_id, rating, comment, watched_at, status_type)
SELECT
  u.id,
  m.id,
  LEAST(10, GREATEST(1, FLOOR(1 + (RAND()+RAND())/2.0 * 9.99))),
  (SELECT txt FROM tmp_comments_massive ORDER BY RAND() LIMIT 1),
  DATE_ADD(CURDATE(), INTERVAL -FLOOR(RAND() * 14) DAY),
  'COMPLETED'
FROM tmp_all_users u
CROSS JOIN (SELECT id FROM media ORDER BY RAND() LIMIT 30) m
WHERE RAND() < 0.50
ORDER BY RAND();


-- ========== 3e. NOTAS POLARIZADAS (baixas: 1-3) ==========
-- Quebra a monotonia de notas altas
INSERT IGNORE INTO watch_entry (user_id, media_id, rating, comment, watched_at, status_type)
SELECT
  u.id,
  m.id,
  FLOOR(1 + RAND() * 3),
  CASE FLOOR(RAND() * 4)
    WHEN 0 THEN 'Perdi tempo assistindo isso.'
    WHEN 1 THEN 'Horrivel, nao recomendo.'
    WHEN 2 THEN 'Nao entendi o hype, achei fraco.'
    ELSE 'Roteiro confuso e arrastado.'
  END,
  DATE_ADD('2024-01-01', INTERVAL FLOOR(RAND() * 810) DAY),
  CASE WHEN RAND() < 0.3 THEN 'DROPPED' ELSE 'COMPLETED' END
FROM (SELECT id FROM users ORDER BY RAND() LIMIT 12) u
CROSS JOIN (SELECT id FROM media ORDER BY RAND() LIMIT 20) m
WHERE RAND() < 0.60
ORDER BY RAND();


-- ========== 3f. CLASSICOS COM NOTAS ALTAS (8-10) ==========
-- Primeiros 15 filmes (Godfather, Dark Knight etc.) ganham muitas notas altas
INSERT IGNORE INTO watch_entry (user_id, media_id, rating, comment, watched_at, status_type)
SELECT
  u.id,
  m.id,
  FLOOR(8 + RAND() * 3),
  CASE FLOOR(RAND() * 4)
    WHEN 0 THEN 'Obra-prima absoluta!'
    WHEN 1 THEN 'Classico indiscutivel.'
    WHEN 2 THEN 'Um dos meus favoritos de todos os tempos.'
    ELSE 'Genial! Precisa de mais de uma sessao pra absorver.'
  END,
  DATE_ADD('2023-01-01', INTERVAL FLOOR(RAND() * 1170) DAY),
  'COMPLETED'
FROM tmp_all_users u
CROSS JOIN (SELECT id FROM media ORDER BY id LIMIT 15) m
WHERE RAND() < 0.80
ORDER BY RAND();


COMMIT;


-- ========== 4. RECALCULAR MEDIA_POPULARITY (read model) ==========
-- Dados inseridos direto no banco (sem Kafka), entao precisamos
-- reconstruir o read model manualmente.

START TRANSACTION;

DELETE FROM media_popularity;

INSERT INTO media_popularity (media_id, ratings_count, avg_rating, watch_count, last_watched_at, updated_at)
SELECT
  w.media_id,
  COUNT(w.rating)                                  AS ratings_count,
  ROUND(AVG(w.rating), 2)                         AS avg_rating,
  COUNT(*)                                          AS watch_count,
  MAX(CONCAT(w.watched_at, ' 00:00:00'))           AS last_watched_at,
  NOW()                                             AS updated_at
FROM watch_entry w
WHERE w.media_id IS NOT NULL
GROUP BY w.media_id;

COMMIT;


-- ========== 5. RECALCULAR USER_STATS (read model) ==========

START TRANSACTION;

DELETE FROM user_stats;

INSERT INTO user_stats (user_id, total_watched, total_movies, total_series, avg_rating, last_watched_at, updated_at)
SELECT
  w.user_id,
  COUNT(*)                                                                      AS total_watched,
  SUM(CASE WHEN m.type = 'MOVIE' THEN 1 ELSE 0 END)                           AS total_movies,
  SUM(CASE WHEN m.type = 'SERIES' THEN 1 ELSE 0 END)                          AS total_series,
  ROUND(AVG(w.rating), 2)                                                       AS avg_rating,
  MAX(w.watched_at)                                                             AS last_watched_at,
  NOW()                                                                         AS updated_at
FROM watch_entry w
JOIN media m ON m.id = w.media_id
WHERE w.media_id IS NOT NULL
GROUP BY w.user_id;

COMMIT;


-- ========== 6. VERIFICACOES ==========
SELECT '--- RESUMO GERAL ---' AS info;
SELECT 'users' AS tabela, COUNT(*) AS total FROM users
UNION ALL SELECT 'media', COUNT(*) FROM media
UNION ALL SELECT 'watch_entry', COUNT(*) FROM watch_entry
UNION ALL SELECT 'media_popularity', COUNT(*) FROM media_popularity
UNION ALL SELECT 'user_stats', COUNT(*) FROM user_stats;

SELECT '--- DISTRIBUICAO DE RATINGS ---' AS info;
SELECT rating, COUNT(*) AS qtd,
       REPEAT('*', GREATEST(1, COUNT(*) DIV 20)) AS histogram
FROM watch_entry
WHERE rating IS NOT NULL
GROUP BY rating ORDER BY rating;

SELECT '--- TOP 10 MAIS ASSISTIDOS ---' AS info;
SELECT m.title, mp.watch_count, mp.avg_rating, mp.ratings_count
FROM media_popularity mp
JOIN media m ON m.id = mp.media_id
ORDER BY mp.watch_count DESC
LIMIT 10;

SELECT '--- TOP 10 MELHOR AVALIADOS (min 5 ratings) ---' AS info;
SELECT m.title, mp.avg_rating, mp.watch_count, mp.ratings_count
FROM media_popularity mp
JOIN media m ON m.id = mp.media_id
WHERE mp.ratings_count >= 5
ORDER BY mp.avg_rating DESC
LIMIT 10;

SELECT '--- 10 PIOR AVALIADOS (min 5 ratings) ---' AS info;
SELECT m.title, mp.avg_rating, mp.watch_count, mp.ratings_count
FROM media_popularity mp
JOIN media m ON m.id = mp.media_id
WHERE mp.ratings_count >= 5
ORDER BY mp.avg_rating ASC
LIMIT 10;

SELECT '--- TRENDING (ultimos 14 dias) ---' AS info;
SELECT m.title, mp.watch_count, mp.avg_rating
FROM media_popularity mp
JOIN media m ON m.id = mp.media_id
WHERE mp.last_watched_at >= DATE_SUB(NOW(), INTERVAL 14 DAY)
ORDER BY mp.watch_count DESC
LIMIT 10;

SELECT '--- STATS POR USUARIO (top 10) ---' AS info;
SELECT u.name, us.total_watched, us.total_movies, us.total_series, us.avg_rating
FROM user_stats us
JOIN users u ON u.id = us.user_id
ORDER BY us.total_watched DESC
LIMIT 10;
