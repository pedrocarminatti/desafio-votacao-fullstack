package com.cooperativa.votacao.repository;

import com.cooperativa.votacao.domain.SessaoVotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacao, Long> {

    Optional<SessaoVotacao> findFirstByPautaIdOrderByAbertaEmDesc(Long pautaId);
}
