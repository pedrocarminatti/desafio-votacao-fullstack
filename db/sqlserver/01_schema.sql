IF DB_ID('votacao') IS NULL
BEGIN
    CREATE DATABASE votacao;
END;
GO

USE votacao;
GO

IF OBJECT_ID('dbo.votos', 'U') IS NOT NULL
    DROP TABLE dbo.votos;
GO

IF OBJECT_ID('dbo.sessoes_votacao', 'U') IS NOT NULL
    DROP TABLE dbo.sessoes_votacao;
GO

IF OBJECT_ID('dbo.pautas', 'U') IS NOT NULL
    DROP TABLE dbo.pautas;
GO

CREATE TABLE dbo.pautas (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    criada_em DATETIMEOFFSET(6) NOT NULL
);
GO

CREATE TABLE dbo.sessoes_votacao (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    aberta_em DATETIMEOFFSET(6) NOT NULL,
    encerra_em DATETIMEOFFSET(6) NOT NULL,
    CONSTRAINT fk_sessoes_pautas FOREIGN KEY (pauta_id) REFERENCES dbo.pautas(id)
);
GO

CREATE TABLE dbo.votos (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    pauta_id BIGINT NOT NULL,
    associado_id VARCHAR(80) NOT NULL,
    opcao VARCHAR(3) NOT NULL,
    votado_em DATETIMEOFFSET(6) NOT NULL,
    CONSTRAINT fk_votos_sessao FOREIGN KEY (sessao_id) REFERENCES dbo.sessoes_votacao(id),
    CONSTRAINT fk_votos_pauta FOREIGN KEY (pauta_id) REFERENCES dbo.pautas(id),
    CONSTRAINT uk_voto_pauta_associado UNIQUE (pauta_id, associado_id),
    CONSTRAINT ck_votos_opcao CHECK (opcao IN ('SIM', 'NAO'))
);
GO

CREATE INDEX idx_sessao_pauta_abertura
    ON dbo.sessoes_votacao (pauta_id, aberta_em);
GO

CREATE INDEX idx_voto_sessao
    ON dbo.votos (sessao_id);
GO

CREATE INDEX idx_voto_pauta
    ON dbo.votos (pauta_id);
GO
