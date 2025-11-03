/* ============================================================
   SEED #4 — PEOPLE (nomes reais de atores, diretores, roteiristas)
   - Requer: tabelas criadas
   - Campos: name, birth_date (aproximada), place_of_birth
   ============================================================ */

START TRANSACTION;

INSERT INTO people (name, birth_date, place_of_birth)
VALUES
-- ===================== DIRETORES =====================
('Steven Spielberg', '1946-12-18', 'Cincinnati, USA'),
('Christopher Nolan', '1970-07-30', 'London, UK'),
('Martin Scorsese', '1942-11-17', 'New York, USA'),
('Francis Ford Coppola', '1939-04-07', 'Detroit, USA'),
('Ridley Scott', '1937-11-30', 'South Shields, UK'),
('Quentin Tarantino', '1963-03-27', 'Knoxville, USA'),
('James Cameron', '1954-08-16', 'Kapuskasing, Canada'),
('Peter Jackson', '1961-10-31', 'Wellington, New Zealand'),
('Denis Villeneuve', '1967-10-03', 'Bécancour, Canada'),
('Bong Joon Ho', '1969-09-14', 'Daegu, South Korea'),
('Hayao Miyazaki', '1941-01-05', 'Tokyo, Japan'),
('Stanley Kubrick', '1928-07-26', 'New York, USA'),
('Alfonso Cuarón', '1961-11-28', 'Mexico City, Mexico'),
('Alejandro G. Iñárritu', '1963-08-15', 'Mexico City, Mexico'),
('Guillermo del Toro', '1964-10-09', 'Guadalajara, Mexico'),
('David Fincher', '1962-08-28', 'Denver, USA'),
('Greta Gerwig', '1983-08-04', 'Sacramento, USA'),
('George Lucas', '1944-05-14', 'Modesto, USA'),
('Spike Lee', '1957-03-20', 'Atlanta, USA'),
('Clint Eastwood', '1930-05-31', 'San Francisco, USA'),

-- ===================== ATORES/ATRIZES =====================
('Leonardo DiCaprio', '1974-11-11', 'Los Angeles, USA'),
('Robert De Niro', '1943-08-17', 'New York, USA'),
('Al Pacino', '1940-04-25', 'New York, USA'),
('Brad Pitt', '1963-12-18', 'Shawnee, USA'),
('Morgan Freeman', '1937-06-01', 'Memphis, USA'),
('Tom Hanks', '1956-07-09', 'Concord, USA'),
('Natalie Portman', '1981-06-09', 'Jerusalem, Israel'),
('Christian Bale', '1974-01-30', 'Haverfordwest, UK'),
('Joaquin Phoenix', '1974-10-28', 'San Juan, Puerto Rico'),
('Heath Ledger', '1979-04-04', 'Perth, Australia'),
('Meryl Streep', '1949-06-22', 'Summit, USA'),
('Scarlett Johansson', '1984-11-22', 'New York, USA'),
('Johnny Depp', '1963-06-09', 'Owensboro, USA'),
('Cate Blanchett', '1969-05-14', 'Melbourne, Australia'),
('Harrison Ford', '1942-07-13', 'Chicago, USA'),
('Matt Damon', '1970-10-08', 'Cambridge, USA'),
('Anne Hathaway', '1982-11-12', 'Brooklyn, USA'),
('Keanu Reeves', '1964-09-02', 'Beirut, Lebanon'),
('Uma Thurman', '1970-04-29', 'Boston, USA'),
('Tom Cruise', '1962-07-03', 'Syracuse, USA'),
('Emma Stone', '1988-11-06', 'Scottsdale, USA'),
('Ryan Gosling', '1980-11-12', 'London, Canada'),
('Denzel Washington', '1954-12-28', 'Mount Vernon, USA'),
('Robert Downey Jr.', '1965-04-04', 'New York, USA'),
('Anthony Hopkins', '1937-12-31', 'Port Talbot, Wales'),
('Gary Oldman', '1958-03-21', 'London, UK'),
('Daniel Day-Lewis', '1957-04-29', 'London, UK'),
('Robin Williams', '1951-07-21', 'Chicago, USA'),
('Julia Roberts', '1967-10-28', 'Smyrna, USA'),
('Nicole Kidman', '1967-06-20', 'Honolulu, USA'),
('Charlize Theron', '1975-08-07', 'Benoni, South Africa'),
('Anthony Hopkins', '1937-12-31', 'Port Talbot, Wales'),
('Samuel L. Jackson', '1948-12-21', 'Washington, USA'),
('Will Smith', '1968-09-25', 'Philadelphia, USA'),
('Hugh Jackman', '1968-10-12', 'Sydney, Australia'),
('Jake Gyllenhaal', '1980-12-19', 'Los Angeles, USA'),
('Amy Adams', '1974-08-20', 'Vicenza, Italy'),
('Anne Hathaway', '1982-11-12', 'Brooklyn, USA'),
('Jennifer Lawrence', '1990-08-15', 'Louisville, USA'),
('Marion Cotillard', '1975-09-30', 'Paris, France'),
('Javier Bardem', '1969-03-01', 'Las Palmas, Spain'),
('Penélope Cruz', '1974-04-28', 'Alcobendas, Spain'),
('Daniel Craig', '1968-03-02', 'Chester, UK'),
('Tilda Swinton', '1960-11-05', 'London, UK'),
('Michael Fassbender', '1977-04-02', 'Heidelberg, Germany'),
('Brie Larson', '1989-10-01', 'Sacramento, USA'),
('Zendaya', '1996-09-01', 'Oakland, USA'),
('Florence Pugh', '1996-01-03', 'Oxford, UK'),
('Timothée Chalamet', '1995-12-27', 'New York, USA'),
('Cillian Murphy', '1976-05-25', 'Douglas, Ireland'),
('Rami Malek', '1981-05-12', 'Los Angeles, USA'),
('Adam Driver', '1983-11-19', 'San Diego, USA'),

-- ===================== BRASILEIROS =====================
('Wagner Moura', '1976-06-27', 'Salvador, Brazil'),
('Alice Braga', '1983-04-15', 'São Paulo, Brazil'),
('Rodrigo Santoro', '1975-08-22', 'Petrópolis, Brazil'),
('Fernanda Montenegro', '1929-10-16', 'Rio de Janeiro, Brazil'),
('Sônia Braga', '1950-06-08', 'Maringá, Brazil'),
('Selton Mello', '1972-12-30', 'Passos, Brazil'),
('Lázaro Ramos', '1978-11-01', 'Salvador, Brazil'),
('Leandra Leal', '1982-09-08', 'Rio de Janeiro, Brazil'),
('Caio Blat', '1980-06-02', 'São Paulo, Brazil'),
('Chico Diaz', '1959-02-16', 'Mexico City, Mexico'),
('Bruno Gagliasso', '1982-04-13', 'Rio de Janeiro, Brazil'),
('Matheus Nachtergaele', '1968-01-03', 'São Paulo, Brazil'),
('Camila Pitanga', '1977-06-14', 'Rio de Janeiro, Brazil'),
('Lima Duarte', '1930-03-29', 'Desemboque, Brazil'),
('Irandhir Santos', '1978-08-22', 'Barreiros, Brazil'),
('Letícia Sabatella', '1971-03-08', 'Belo Horizonte, Brazil'),
('Débora Falabella', '1979-02-22', 'Belo Horizonte, Brazil'),
('Cauã Reymond', '1980-05-20', 'Rio de Janeiro, Brazil'),
('Taís Araújo', '1978-11-25', 'Rio de Janeiro, Brazil'),
('Lázaro Ramos', '1978-11-01', 'Salvador, Brazil');

COMMIT;

-- conferência rápida
SELECT 'people_total' AS what, COUNT(*) AS total FROM people;
SELECT name, place_of_birth FROM people ORDER BY RAND() LIMIT 10;
