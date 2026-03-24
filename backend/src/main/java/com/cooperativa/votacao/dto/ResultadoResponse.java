package com.cooperativa.votacao.dto;

public record ResultadoResponse(
        long votosSim,
        long votosNao,
        long totalVotos,
        String situacao
) {
}
