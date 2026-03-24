USE votacao;
GO

INSERT INTO dbo.pautas (titulo, descricao, criada_em)
VALUES
    ('Aprovacao do balanco anual', 'Pauta inicial para validar a integracao com SQL Server.', SYSDATETIMEOFFSET());
GO
