/* ============================================================
   SEED #5 — CREDITS (reais)
   ✅ Versão corrigida (sem erro de PRIMARY KEY)
   ============================================================ */

START TRANSACTION;

-- 1) Completar PEOPLE com diretores/criadores ausentes (INSERT IGNORE)
INSERT IGNORE INTO people (name) VALUES
('Robert Zemeckis'),
('Lana Wachowski'),
('Lilly Wachowski'),
('Jonathan Demme'),
('Fernando Meirelles'),
('Kátia Lund'),
('Roman Polanski'),
('Damien Chazelle'),
('Frank Darabont'),
('Bryan Singer'),
('Luc Besson'),
('Roberto Benigni'),
('Olivier Nakache'),
('Éric Toledano'),
('Sam Mendes'),
('Park Chan-wook'),
('Jean-Pierre Jeunet'),
('John Lasseter'),
('Lee Unkrich'),
('Florian Henckel von Donnersmarck'),
('Miloš Forman'),
('Mel Gibson'),
('Wolfgang Petersen'),
('Charlie Chaplin'),
('Sidney Lumet'),
('Gus Van Sant'),
('Michael Mann'),
('Paul Thomas Anderson'),
('Joel Coen'),
('Ethan Coen'),
('George Miller'),
('Todd Phillips'),
('Wes Anderson'),
('Spike Jonze'),
('Nicolas Winding Refn'),
('Thomas Vinterberg'),
('Makoto Shinkai'),
('S. S. Rajamouli'),
('Ang Lee'),
('Peter Weir'),
('Brad Bird'),
('Andrew Stanton'),
('Pete Docter'),
('James Gunn'),
('Peter Ramsey'),
('Joaquim Dos Santos'),
('Matt Reeves'),

-- Criadores / Showrunners (séries)
('Vince Gilligan'),
('David Benioff'),
('D. B. Weiss'),
('David Chase'),
('David Simon'),
('Marta Kauffman'),
('David Crane'),
('Greg Daniels'),
('Ricky Gervais'),
('Stephen Merchant'),
('Matt Duffer'),
('Ross Duffer'),
('Peter Morgan'),
('Jon Favreau'),
('Craig Mazin'),
('Neil Druckmann'),
('Ryan Condal'),
('Miguel Sapochnik'),
('Nic Pizzolatto'),
('Noah Hawley'),
('Baran bo Odar'),
('Jantje Friese'),
('Chris Brancato'),
('Carlo Bernard'),
('Doug Miro'),
('Charlie Brooker'),
('Jonathan Nolan'),
('Lisa Joy'),
('Jesse Armstrong'),
('Peter Gould'),
('Sam Esmail'),
('Eric Kripke'),
('Christopher Storer'),
('Dan Erickson'),
('Ben Stiller'),
('Scott Frank'),
('Allan Scott'),
('Lauren Schmidt Hissrich'),
('Michael Hirst'),
('Bruce Miller'),
('Álex Pina'),
('Hajime Isayama'),
('Tetsurō Araki'),
('Yasuhiro Irie'),
('Eiichiro Oda'),
('Michael Dante DiMartino'),
('Bryan Konietzko'),
('Shinichirō Watanabe'),
('Steven Moffat'),
('Mark Gatiss'),
('Steven Knight');

-- 2) Tabela temporária de mapeamentos (título -> pessoa -> papel)
DROP TEMPORARY TABLE IF EXISTS tmp_credits_map;
CREATE TEMPORARY TABLE tmp_credits_map (
  title VARCHAR(300) NOT NULL,
  person_name VARCHAR(200) NOT NULL,
  role ENUM('DIRECTOR','WRITER','ACTOR','PRODUCER','COMPOSER') NOT NULL,
  character_name VARCHAR(200) NULL,
  order_index SMALLINT NULL,
  PRIMARY KEY (title, person_name, role)
);
-- 👆 character_name removido da PK (pode ser NULL)

-- 2a) FILMES — Diretores
INSERT INTO tmp_credits_map (title, person_name, role, character_name, order_index) VALUES
('The Godfather', 'Francis Ford Coppola', 'DIRECTOR', NULL, 1),
('The Godfather Part II', 'Francis Ford Coppola', 'DIRECTOR', NULL, 1),
('The Dark Knight', 'Christopher Nolan', 'DIRECTOR', NULL, 1),
('Pulp Fiction', 'Quentin Tarantino', 'DIRECTOR', NULL, 1),
('Schindler''s List', 'Steven Spielberg', 'DIRECTOR', NULL, 1),
('The Lord of the Rings: The Fellowship of the Ring', 'Peter Jackson', 'DIRECTOR', NULL, 1),
('The Lord of the Rings: The Return of the King', 'Peter Jackson', 'DIRECTOR', NULL, 1),
('Inception', 'Christopher Nolan', 'DIRECTOR', NULL, 1),
('Fight Club', 'David Fincher', 'DIRECTOR', NULL, 1),
('Forrest Gump', 'Robert Zemeckis', 'DIRECTOR', NULL, 1),
('The Matrix', 'Lana Wachowski', 'DIRECTOR', NULL, 1),
('The Matrix', 'Lilly Wachowski', 'DIRECTOR', NULL, 2),
('Goodfellas', 'Martin Scorsese', 'DIRECTOR', NULL, 1),
('Se7en', 'David Fincher', 'DIRECTOR', NULL, 1),
('The Silence of the Lambs', 'Jonathan Demme', 'DIRECTOR', NULL, 1),
('Interstellar', 'Christopher Nolan', 'DIRECTOR', NULL, 1),
('Parasite', 'Bong Joon Ho', 'DIRECTOR', NULL, 1),
('Spirited Away', 'Hayao Miyazaki', 'DIRECTOR', NULL, 1),
('City of God', 'Fernando Meirelles', 'DIRECTOR', NULL, 1),
('City of God', 'Kátia Lund', 'DIRECTOR', NULL, 2),
('The Pianist', 'Roman Polanski', 'DIRECTOR', NULL, 1),
('Gladiator', 'Ridley Scott', 'DIRECTOR', NULL, 1),
('Saving Private Ryan', 'Steven Spielberg', 'DIRECTOR', NULL, 1),
('Whiplash', 'Damien Chazelle', 'DIRECTOR', NULL, 1),
('The Green Mile', 'Frank Darabont', 'DIRECTOR', NULL, 1),
('The Usual Suspects', 'Bryan Singer', 'DIRECTOR', NULL, 1),
('Léon: The Professional', 'Luc Besson', 'DIRECTOR', NULL, 1),
('Life Is Beautiful', 'Roberto Benigni', 'DIRECTOR', NULL, 1),
('The Intouchables', 'Olivier Nakache', 'DIRECTOR', NULL, 1),
('The Intouchables', 'Éric Toledano', 'DIRECTOR', NULL, 2),
('The Prestige', 'Christopher Nolan', 'DIRECTOR', NULL, 1),
('The Departed', 'Martin Scorsese', 'DIRECTOR', NULL, 1),
('The Lion King', 'Roger Allers', 'DIRECTOR', NULL, 1),
('Back to the Future', 'Robert Zemeckis', 'DIRECTOR', NULL, 1),
('Terminator 2: Judgment Day', 'James Cameron', 'DIRECTOR', NULL, 1),
('Alien', 'Ridley Scott', 'DIRECTOR', NULL, 1),
('Aliens', 'James Cameron', 'DIRECTOR', NULL, 1),
('Apocalypse Now', 'Francis Ford Coppola', 'DIRECTOR', NULL, 1),
('American Beauty', 'Sam Mendes', 'DIRECTOR', NULL, 1);

-- (... mantém o restante igual até o final ...)

-- 3) Inserir na tabela credits usando JOIN por título e nome
INSERT INTO credits (media_id, person_id, role, character_name, order_index)
SELECT m.id, p.id, t.role, t.character_name, COALESCE(t.order_index, 1)
FROM tmp_credits_map t
JOIN media  m ON m.title = t.title
JOIN people p ON p.name  = t.person_name;

COMMIT;

-- verificações
SELECT 'credits_total' AS what, COUNT(*) FROM credits;
SELECT m.title, c.role, p.name, c.character_name
FROM credits c
JOIN media m ON m.id = c.media_id
JOIN people p ON p.id = c.person_id
ORDER BY m.title, c.role, c.order_index
LIMIT 40;
