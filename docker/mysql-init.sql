-- Script de inicialização do MySQL para o CineLog
-- Este script é executado automaticamente quando o container MySQL é criado

-- Conceder permissões necessárias ao usuário cinelog
GRANT CREATE, TRIGGER ON *.* TO 'cinelog'@'%';
GRANT ALL PRIVILEGES ON audit.* TO 'cinelog'@'%';
GRANT ALL PRIVILEGES ON cinelog.* TO 'cinelog'@'%';
FLUSH PRIVILEGES;

-- Criar o schema audit se não existir
CREATE SCHEMA IF NOT EXISTS audit DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;

SELECT 'MySQL initialization completed successfully!' AS Status;
