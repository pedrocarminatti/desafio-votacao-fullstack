package com.cooperativa.votacao.repository;

import com.cooperativa.votacao.domain.OpcaoVoto;

public interface ResultadoVotacaoProjection {

    OpcaoVoto getOpcao();

    long getQuantidade();
}
