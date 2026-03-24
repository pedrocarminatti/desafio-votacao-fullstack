package com.cooperativa.votacao.dto;

import java.time.OffsetDateTime;

public record PautaResponse(
        Long id,
        String titulo,
        String descricao,
        OffsetDateTime criadaEm,
        SessaoResponse sessao,
        ResultadoResponse resultado
) {
}
