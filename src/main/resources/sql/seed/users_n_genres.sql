/* ==========================================
SEED #1 — USERS (poucos) + GENRES (reais)
Banco: MySQL 8+ (após rodar as migrations)
========================================== */
START TRANSACTION;

-- ---------- USERS (apenas alguns registros) ----------
-- evita erro se já existir (email é único)
INSERT INTO
  users (name, email)
VALUES
  ('Marcus Prado', 'marcus.prado@gmail.com'),
  ('Ana Silva', 'ana.silva@gmail.com'),
  ('João Santos', 'joao.santos@gmail.com') ON DUPLICATE KEY
UPDATE name =
VALUES
  (name);

-- ---------- GENRES (lista real consolidada) ----------
-- fontes comuns: TMDb/IMDb categorias amplamente usadas
-- usamos nomes padronizados em inglês para consistência
INSERT INTO
  genres (name)
VALUES
  ('Action'),
  ('Adventure'),
  ('Animation'),
  ('Comedy'),
  ('Crime'),
  ('Documentary'),
  ('Drama'),
  ('Family'),
  ('Fantasy'),
  ('History'),
  ('Horror'),
  ('Music'),
  ('Mystery'),
  ('Romance'),
  ('Science Fiction'),
  ('TV Movie'),
  ('Thriller'),
  ('War'),
  ('Western'),
  ('Biography'),
  ('Sport'),
  ('Superhero'),
  ('Film-Noir') ON DUPLICATE KEY
UPDATE name =
VALUES
  (name);

COMMIT;

-- sanity checks opcionais
SELECT
  'users' AS t,
  COUNT(*) AS total
FROM
  users
UNION ALL
SELECT
  'genres',
  COUNT(*)
FROM
  genres;