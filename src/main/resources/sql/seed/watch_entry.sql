/* ============================================================
   SEED #7 — WATCH_ENTRY (histórico de visualização realista)
   ✅ Versão FINAL compatível (sem erro de LIMIT)
   ============================================================ */

START TRANSACTION;

-- ========== PARÂMETROS ==========
SET @movies_per_user   = 30;
SET @episodes_per_user = 50;

-- ========== LISTAS BASE ==========
DROP TEMPORARY TABLE IF EXISTS tmp_users;
CREATE TEMPORARY TABLE tmp_users AS
SELECT id, name FROM users;

DROP TEMPORARY TABLE IF EXISTS tmp_movies;
CREATE TEMPORARY TABLE tmp_movies AS
SELECT id, title FROM media WHERE type = 'MOVIE' ORDER BY id;

DROP TEMPORARY TABLE IF EXISTS tmp_episodes;
CREATE TEMPORARY TABLE tmp_episodes AS
SELECT e.id, m.title AS series_title, s.season_number, e.episode_number
FROM episodes e
JOIN seasons s ON s.id = e.season_id
JOIN media m ON m.id = s.media_id;

-- ========== COMENTÁRIOS ==========
DROP TEMPORARY TABLE IF EXISTS tmp_comments;
CREATE TEMPORARY TABLE tmp_comments (id INT PRIMARY KEY AUTO_INCREMENT, txt VARCHAR(255));
INSERT INTO tmp_comments (txt) VALUES
('Incrível! Que história.'),
('Muito bom, assistiria de novo.'),
('Excelente atuação!'),
('Trilha sonora marcante.'),
('Achei o roteiro um pouco confuso.'),
('Final surpreendente.'),
('Visual espetacular.'),
('Um clássico imperdível.'),
('Gostei, mas esperava mais.'),
('Fotografia perfeita.'),
('Um dos meus favoritos.'),
('Filme ok, mas o livro é melhor.'),
('Diálogo brilhante.'),
('Cena final inesquecível.'),
('Amei cada minuto.');

-- ========== CALCULAR LIMITES ==========
SELECT COUNT(*) INTO @total_users FROM tmp_users;
SET @limit_movies   = @total_users * @movies_per_user;
SET @limit_episodes = @total_users * @episodes_per_user;

-- armazenar limites em tabela para usar em statement dinâmico
DROP TEMPORARY TABLE IF EXISTS tmp_limits;
CREATE TEMPORARY TABLE tmp_limits (lm INT, le INT);
INSERT INTO tmp_limits VALUES (@limit_movies, @limit_episodes);

-- ========== (A) FILMES ==========
SET @sql_movies = CONCAT('
INSERT INTO watch_entry (user_id, media_id, rating, comment, watched_at)
SELECT 
  u.id,
  m.id,
  FLOOR(6 + RAND()*5),
  (SELECT txt FROM tmp_comments ORDER BY RAND() LIMIT 1),
  DATE_ADD(''2018-01-01'', INTERVAL FLOOR(RAND()*(7*365)) DAY)
FROM tmp_users u
JOIN tmp_movies m ON RAND() < 0.1
ORDER BY u.id, m.id
LIMIT ', (SELECT lm FROM tmp_limits), ';');

PREPARE stmt FROM @sql_movies;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========== (B) EPISÓDIOS ==========
SET @sql_eps = CONCAT('
INSERT INTO watch_entry (user_id, media_id, episode_id, rating, comment, watched_at)
SELECT 
  u.id,
  m.id, 
  e.id,
  FLOOR(5 + RAND()*6),
  (SELECT txt FROM tmp_comments ORDER BY RAND() LIMIT 1),
  DATE_ADD(''2019-01-01'', INTERVAL FLOOR(RAND()*(6*365)) DAY)
FROM tmp_users u
JOIN tmp_episodes e ON RAND() < 0.03
JOIN media m ON m.title = e.series_title
ORDER BY u.id, e.id
LIMIT ', (SELECT le FROM tmp_limits), ';');


PREPARE stmt2 FROM @sql_eps;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

COMMIT;

-- verificações rápidas
SELECT 'watch_entry_total' AS what, COUNT(*) FROM watch_entry;
SELECT u.name, COUNT(*) AS entries, ROUND(AVG(rating),1) AS avg_rating
FROM watch_entry w
JOIN users u ON u.id = w.user_id
GROUP BY u.name
ORDER BY entries DESC
LIMIT 10;

SELECT m.title, COUNT(*) AS views, ROUND(AVG(w.rating),1) AS avg_rating
FROM watch_entry w
JOIN media m ON m.id = w.media_id
WHERE m.type='MOVIE'
GROUP BY m.id
ORDER BY views DESC
LIMIT 10;
