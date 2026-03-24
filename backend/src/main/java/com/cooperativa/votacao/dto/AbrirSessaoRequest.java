package com.cooperativa.votacao.dto;

import jakarta.validation.constraints.Positive;

public record AbrirSessaoRequest(
        @Positive(message = "A duracao deve ser maior que zero")
        Long duracaoSegundos
) {
}
