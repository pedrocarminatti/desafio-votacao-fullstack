package com.cooperativa.votacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank(message = "O titulo e obrigatorio")
        @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres")
        String titulo,
        @NotBlank(message = "A descricao e obrigatoria")
        @Size(max = 500, message = "A descricao deve ter no maximo 500 caracteres")
        String descricao
) {
}
