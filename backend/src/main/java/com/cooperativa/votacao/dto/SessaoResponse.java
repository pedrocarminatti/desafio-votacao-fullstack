package com.cooperativa.votacao.dto;

import java.time.OffsetDateTime;

public record SessaoResponse(
        Long id,
        OffsetDateTime abertaEm,
        OffsetDateTime encerraEm,
        boolean aberta
) {
}
