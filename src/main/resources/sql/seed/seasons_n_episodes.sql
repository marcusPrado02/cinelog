/* ============================================================
SEED #6 — SEASONS e EPISODES (dados realistas)
- Requer: SEED #2 (media com séries)
============================================================ */
START TRANSACTION;

-- 1) preparar séries
DROP TEMPORARY TABLE IF EXISTS tmp_series;

CREATE TEMPORARY TABLE tmp_series AS
SELECT
  id,
  title,
  release_year
FROM
  media
WHERE
  type = 'SERIES';

-- 2) definir número de temporadas por série (baseado em séries reais)
DROP TEMPORARY TABLE IF EXISTS tmp_series_season_count;

CREATE TEMPORARY TABLE tmp_series_season_count (
  title VARCHAR(300) PRIMARY KEY,
  total_seasons INT NOT NULL
);

INSERT INTO
  tmp_series_season_count (title, total_seasons)
VALUES
  ('Breaking Bad', 5),
  ('Game of Thrones', 8),
  ('The Sopranos', 6),
  ('The Wire', 5),
  ('Friends', 10),
  ('The Office (US)', 9),
  ('The Office (UK)', 2),
  ('Stranger Things', 5),
  ('The Crown', 6),
  ('The Mandalorian', 3),
  ('The Last of Us', 2),
  ('House of the Dragon', 2),
  ('Chernobyl', 1),
  ('Band of Brothers', 1),
  ('True Detective', 4),
  ('Fargo', 5),
  ('Dark', 3),
  ('Narcos', 3),
  ('Narcos: Mexico', 3),
  ('Black Mirror', 6),
  ('Westworld', 4),
  ('Succession', 4),
  ('Better Call Saul', 6),
  ('Mr. Robot', 4),
  ('The Boys', 4),
  ('The Bear', 3),
  ('Severance', 2),
  ('The Queen''s Gambit', 1),
  ('The Witcher', 3),
  ('Vikings', 6),
  ('The Handmaid''s Tale', 6),
  ('Money Heist (La Casa de Papel)', 5),
  ('Attack on Titan', 4),
  ('Death Note', 1),
  ('Fullmetal Alchemist: Brotherhood', 1),
  ('One Piece', 20),
  ('Avatar: The Last Airbender', 3),
  ('Cowboy Bebop', 1),
  ('Sherlock', 4),
  ('Peaky Blinders', 6);

-- 3) gerar seasons
INSERT INTO
  seasons (media_id, season_number, name, air_date)
SELECT
  m.id,
  s.season_number,
  CONCAT ('Season ', s.season_number),
  MAKEDATE (m.release_year, 1) + INTERVAL (s.season_number - 1) YEAR
FROM
  tmp_series m
  JOIN tmp_series_season_count c ON c.title = m.title
  JOIN (
    SELECT
      1 AS season_number
    UNION ALL
    SELECT
      2
    UNION ALL
    SELECT
      3
    UNION ALL
    SELECT
      4
    UNION ALL
    SELECT
      5
    UNION ALL
    SELECT
      6
    UNION ALL
    SELECT
      7
    UNION ALL
    SELECT
      8
    UNION ALL
    SELECT
      9
    UNION ALL
    SELECT
      10
    UNION ALL
    SELECT
      11
    UNION ALL
    SELECT
      12
    UNION ALL
    SELECT
      13
    UNION ALL
    SELECT
      14
    UNION ALL
    SELECT
      15
    UNION ALL
    SELECT
      16
    UNION ALL
    SELECT
      17
    UNION ALL
    SELECT
      18
    UNION ALL
    SELECT
      19
    UNION ALL
    SELECT
      20
  ) s ON s.season_number <= c.total_seasons;

-- 4) gerar episódios por temporada (mínimo 6, máximo 13)
DROP TEMPORARY TABLE IF EXISTS tmp_seasons;

CREATE TEMPORARY TABLE tmp_seasons AS
SELECT
  id AS season_id,
  media_id,
  season_number,
  air_date
FROM
  seasons;

INSERT INTO
  episodes (season_id, episode_number, name, air_date)
SELECT
  t.season_id,
  e.episode_number,
  CONCAT (
    'Episode ',
    e.episode_number,
    ' (S',
    t.season_number,
    ')'
  ),
  DATE_ADD (
    t.air_date,
    INTERVAL (7 * (e.episode_number - 1)) DAY
  )
FROM
  tmp_seasons t
  JOIN (
    SELECT
      1 AS episode_number
    UNION ALL
    SELECT
      2
    UNION ALL
    SELECT
      3
    UNION ALL
    SELECT
      4
    UNION ALL
    SELECT
      5
    UNION ALL
    SELECT
      6
    UNION ALL
    SELECT
      7
    UNION ALL
    SELECT
      8
    UNION ALL
    SELECT
      9
    UNION ALL
    SELECT
      10
    UNION ALL
    SELECT
      11
    UNION ALL
    SELECT
      12
    UNION ALL
    SELECT
      13
  ) e
WHERE
  e.episode_number <= CASE
    WHEN t.season_number = 1
    AND t.media_id IN (
      SELECT
        id
      FROM
        media
      WHERE
        title IN (
          'Chernobyl',
          'Band of Brothers',
          'The Queen''s Gambit',
          'Death Note',
          'Cowboy Bebop',
          'Fullmetal Alchemist: Brotherhood'
        )
    ) THEN 6
    WHEN t.media_id IN (
      SELECT
        id
      FROM
        media
      WHERE
        title = 'Friends'
    ) THEN 24
    ELSE 10
  END;

COMMIT;

-- verificações rápidas
SELECT
  'seasons_total' AS what,
  COUNT(*)
FROM
  seasons
UNION ALL
SELECT
  'episodes_total',
  COUNT(*)
FROM
  episodes
ORDER BY
  what;

SELECT
  m.title,
  s.season_number,
  COUNT(e.id) AS episodes
FROM
  media m
  JOIN seasons s ON s.media_id = m.id
  LEFT JOIN episodes e ON e.season_id = s.id
GROUP BY
  m.title,
  s.season_number
ORDER BY
  m.title,
  s.season_number
LIMIT
  20;