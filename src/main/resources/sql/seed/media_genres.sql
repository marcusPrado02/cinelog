/* ============================================================
SEED #3 — MEDIA_GENRES (1–3 gêneros reais por título)
✅ Versão FINAL (sem "Can't reopen table")
- Requer: SEED #1 (genres) e SEED #2 (media) aplicados
============================================================ */
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_media_genres_map;

CREATE TEMPORARY TABLE tmp_media_genres_map (
  title VARCHAR(300) PRIMARY KEY,
  g1 VARCHAR(100) NOT NULL,
  g2 VARCHAR(100),
  g3 VARCHAR(100)
);

-- ====== MAPA título → gêneros (mesmo conteúdo de antes) ======
INSERT INTO
  tmp_media_genres_map (title, g1, g2, g3)
VALUES
  -- FILMES
  ('The Godfather', 'Crime', 'Drama', NULL),
  ('The Godfather Part II', 'Crime', 'Drama', NULL),
  ('The Dark Knight', 'Action', 'Crime', 'Superhero'),
  ('Pulp Fiction', 'Crime', 'Drama', NULL),
  ('Schindler''s List', 'History', 'Drama', 'War'),
  (
    'The Lord of the Rings: The Fellowship of the Ring',
    'Adventure',
    'Fantasy',
    'Drama'
  ),
  (
    'The Lord of the Rings: The Return of the King',
    'Adventure',
    'Fantasy',
    'War'
  ),
  (
    'Inception',
    'Science Fiction',
    'Thriller',
    'Action'
  ),
  ('Fight Club', 'Drama', 'Thriller', NULL),
  ('Forrest Gump', 'Drama', 'Romance', NULL),
  ('The Matrix', 'Science Fiction', 'Action', NULL),
  ('Goodfellas', 'Crime', 'Drama', NULL),
  ('Se7en', 'Crime', 'Mystery', 'Thriller'),
  (
    'The Silence of the Lambs',
    'Crime',
    'Thriller',
    'Drama'
  ),
  (
    'Interstellar',
    'Science Fiction',
    'Drama',
    'Adventure'
  ),
  ('Parasite', 'Drama', 'Thriller', 'Comedy'),
  ('Spirited Away', 'Animation', 'Fantasy', 'Family'),
  ('City of God', 'Crime', 'Drama', NULL),
  ('The Pianist', 'Drama', 'War', 'Biography'),
  ('Gladiator', 'Action', 'Drama', 'History'),
  ('Saving Private Ryan', 'War', 'Drama', 'History'),
  ('Whiplash', 'Drama', 'Music', NULL),
  ('The Green Mile', 'Drama', 'Fantasy', NULL),
  (
    'The Usual Suspects',
    'Crime',
    'Mystery',
    'Thriller'
  ),
  (
    'Léon: The Professional',
    'Crime',
    'Drama',
    'Thriller'
  ),
  ('Life Is Beautiful', 'Drama', 'War', 'Romance'),
  ('The Intouchables', 'Comedy', 'Drama', NULL),
  ('The Prestige', 'Drama', 'Mystery', 'Thriller'),
  ('The Departed', 'Crime', 'Drama', 'Thriller'),
  ('The Lion King', 'Animation', 'Family', 'Drama'),
  (
    'Back to the Future',
    'Adventure',
    'Science Fiction',
    'Comedy'
  ),
  (
    'Terminator 2: Judgment Day',
    'Action',
    'Science Fiction',
    'Thriller'
  ),
  ('Alien', 'Horror', 'Science Fiction', NULL),
  ('Aliens', 'Action', 'Science Fiction', 'Horror'),
  ('Apocalypse Now', 'War', 'Drama', NULL),
  ('American Beauty', 'Drama', NULL, NULL),
  ('Oldboy', 'Thriller', 'Drama', 'Mystery'),
  ('The Shining', 'Horror', 'Mystery', 'Drama'),
  ('Amélie', 'Romance', 'Comedy', 'Drama'),
  ('Toy Story', 'Animation', 'Family', 'Comedy'),
  ('Toy Story 3', 'Animation', 'Family', 'Adventure'),
  ('Coco', 'Animation', 'Family', 'Music'),
  ('Pan''s Labyrinth', 'Fantasy', 'War', 'Drama'),
  ('The Lives of Others', 'Drama', 'Thriller', NULL),
  ('Amadeus', 'Drama', 'Music', 'Biography'),
  ('Braveheart', 'History', 'War', 'Drama'),
  ('Das Boot', 'War', 'Drama', 'Thriller'),
  ('The Great Dictator', 'Comedy', 'War', 'Drama'),
  ('12 Angry Men', 'Drama', 'Mystery', NULL),
  ('Good Will Hunting', 'Drama', 'Romance', NULL),
  ('Heat', 'Crime', 'Drama', 'Thriller'),
  ('There Will Be Blood', 'Drama', 'History', NULL),
  (
    'No Country for Old Men',
    'Crime',
    'Drama',
    'Thriller'
  ),
  ('The Social Network', 'Drama', 'Biography', NULL),
  (
    'Mad Max: Fury Road',
    'Action',
    'Adventure',
    'Science Fiction'
  ),
  ('Joker', 'Crime', 'Drama', 'Thriller'),
  (
    'Avengers: Endgame',
    'Action',
    'Adventure',
    'Superhero'
  ),
  ('Dune', 'Science Fiction', 'Adventure', 'Drama'),
  (
    'Dune: Part Two',
    'Science Fiction',
    'Adventure',
    'Drama'
  ),
  ('Oppenheimer', 'Drama', 'History', 'Biography'),
  ('Barbie', 'Comedy', 'Fantasy', NULL),
  ('La La Land', 'Romance', 'Music', 'Drama'),
  (
    'The Grand Budapest Hotel',
    'Comedy',
    'Drama',
    'Crime'
  ),
  ('Her', 'Romance', 'Science Fiction', 'Drama'),
  (
    'Blade Runner 2049',
    'Science Fiction',
    'Drama',
    'Mystery'
  ),
  ('Drive', 'Drama', 'Crime', 'Thriller'),
  ('The Hunt', 'Drama', 'Mystery', NULL),
  ('Incendies', 'Drama', 'Mystery', 'War'),
  ('The Handmaiden', 'Drama', 'Romance', 'Mystery'),
  ('Your Name', 'Animation', 'Romance', 'Fantasy'),
  ('RRR', 'Action', 'Drama', 'History'),
  (
    'Crouching Tiger, Hidden Dragon',
    'Action',
    'Romance',
    'Fantasy'
  ),
  (
    'The Truman Show',
    'Drama',
    'Science Fiction',
    NULL
  ),
  ('The Big Lebowski', 'Comedy', 'Crime', NULL),
  (
    'The Incredibles',
    'Animation',
    'Action',
    'Family'
  ),
  (
    'WALL·E',
    'Animation',
    'Science Fiction',
    'Family'
  ),
  ('Up', 'Animation', 'Adventure', 'Family'),
  (
    'The Dark Knight Rises',
    'Action',
    'Crime',
    'Superhero'
  ),
  (
    'Guardians of the Galaxy',
    'Action',
    'Adventure',
    'Science Fiction'
  ),
  (
    'Spider-Man: Into the Spider-Verse',
    'Animation',
    'Action',
    'Superhero'
  ),
  (
    'Spider-Man: Across the Spider-Verse',
    'Animation',
    'Action',
    'Superhero'
  ),
  ('The Batman', 'Action', 'Crime', 'Superhero'),
  -- SÉRIES
  ('Breaking Bad', 'Crime', 'Drama', 'Thriller'),
  (
    'Game of Thrones',
    'Drama',
    'Fantasy',
    'Adventure'
  ),
  ('The Sopranos', 'Crime', 'Drama', NULL),
  ('The Wire', 'Crime', 'Drama', NULL),
  ('Friends', 'Comedy', 'Romance', NULL),
  ('The Office (US)', 'Comedy', NULL, NULL),
  ('The Office (UK)', 'Comedy', NULL, NULL),
  (
    'Stranger Things',
    'Science Fiction',
    'Horror',
    'Drama'
  ),
  ('The Crown', 'Drama', 'History', NULL),
  (
    'The Mandalorian',
    'Science Fiction',
    'Adventure',
    'Action'
  ),
  (
    'The Last of Us',
    'Drama',
    'Horror',
    'Science Fiction'
  ),
  ('House of the Dragon', 'Drama', 'Fantasy', NULL),
  ('Chernobyl', 'Drama', 'History', NULL),
  ('Band of Brothers', 'War', 'Drama', 'History'),
  ('True Detective', 'Crime', 'Mystery', 'Drama'),
  ('Fargo', 'Crime', 'Drama', 'Thriller'),
  ('Dark', 'Science Fiction', 'Mystery', 'Drama'),
  ('Narcos', 'Crime', 'Drama', NULL),
  ('Narcos: Mexico', 'Crime', 'Drama', NULL),
  (
    'Black Mirror',
    'Science Fiction',
    'Drama',
    'Thriller'
  ),
  (
    'Westworld',
    'Science Fiction',
    'Drama',
    'Mystery'
  ),
  ('Succession', 'Drama', NULL, NULL),
  ('Better Call Saul', 'Crime', 'Drama', NULL),
  ('Mr. Robot', 'Drama', 'Thriller', 'Mystery'),
  ('The Boys', 'Action', 'Superhero', 'Comedy'),
  ('The Bear', 'Drama', 'Comedy', NULL),
  (
    'Severance',
    'Science Fiction',
    'Drama',
    'Mystery'
  ),
  ('The Queen''s Gambit', 'Drama', 'Sport', NULL),
  ('The Witcher', 'Fantasy', 'Action', 'Adventure'),
  ('Vikings', 'Drama', 'History', 'Action'),
  (
    'The Handmaid''s Tale',
    'Drama',
    'Science Fiction',
    NULL
  ),
  (
    'Money Heist (La Casa de Papel)',
    'Crime',
    'Drama',
    'Thriller'
  ),
  (
    'Attack on Titan',
    'Animation',
    'Action',
    'Fantasy'
  ),
  ('Death Note', 'Animation', 'Mystery', 'Thriller'),
  (
    'Fullmetal Alchemist: Brotherhood',
    'Animation',
    'Action',
    'Adventure'
  ),
  ('One Piece', 'Animation', 'Action', 'Adventure'),
  (
    'Avatar: The Last Airbender',
    'Animation',
    'Adventure',
    'Family'
  ),
  (
    'Cowboy Bebop',
    'Animation',
    'Action',
    'Science Fiction'
  ),
  ('Sherlock', 'Crime', 'Mystery', 'Drama'),
  ('Peaky Blinders', 'Crime', 'Drama', NULL);

-- ====== EXPANSÃO sem reabrir tabela: 3 INSERTs separados ======
DROP TEMPORARY TABLE IF EXISTS tmp_media_genres_expanded;

CREATE TEMPORARY TABLE tmp_media_genres_expanded (
  title VARCHAR(300) NOT NULL,
  g VARCHAR(100) NOT NULL,
  PRIMARY KEY (title, g)
);

-- g1
INSERT IGNORE INTO tmp_media_genres_expanded (title, g)
SELECT
  title,
  g1
FROM
  tmp_media_genres_map;

-- g2 (somente não nulos)
INSERT IGNORE INTO tmp_media_genres_expanded (title, g)
SELECT
  title,
  g2
FROM
  tmp_media_genres_map
WHERE
  g2 IS NOT NULL;

-- g3 (somente não nulos)
INSERT IGNORE INTO tmp_media_genres_expanded (title, g)
SELECT
  title,
  g3
FROM
  tmp_media_genres_map
WHERE
  g3 IS NOT NULL;

-- ====== INSERÇÃO FINAL em media_genres ======
INSERT IGNORE INTO media_genres (media_id, genre_id)
SELECT
  m.id,
  g.id
FROM
  tmp_media_genres_expanded x
  JOIN media m ON m.title = x.title
  JOIN genres g ON g.name = x.g;

COMMIT;

-- conferências rápidas
SELECT
  'media_genres' AS what,
  COUNT(*) AS total
FROM
  media_genres;

SELECT
  m.title,
  GROUP_CONCAT (
    g.name
    ORDER BY
      g.name SEPARATOR ', '
  ) AS genres
FROM
  media m
  JOIN media_genres mg ON mg.media_id = m.id
  JOIN genres g ON g.id = mg.genre_id
GROUP BY
  m.id
ORDER BY
  m.title
LIMIT
  20;