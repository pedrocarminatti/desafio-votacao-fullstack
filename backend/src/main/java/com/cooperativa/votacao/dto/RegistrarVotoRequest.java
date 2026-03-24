package com.cooperativa.votacao.dto;

import com.cooperativa.votacao.domain.OpcaoVoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarVotoRequest(
        @NotBlank(message = "O identificador do associado e obrigatorio")
        @Size(max = 80, message = "O identificador do associado deve ter no maximo 80 caracteres")
        String associadoId,
        @NotBlank(message = "O CPF e obrigatorio")
        @Size(min = 11, max = 14, message = "O CPF deve ter entre 11 e 14 caracteres")
        String cpf,
        @NotNull(message = "A opcao do voto e obrigatoria")
        OpcaoVoto opcao
) {
}
