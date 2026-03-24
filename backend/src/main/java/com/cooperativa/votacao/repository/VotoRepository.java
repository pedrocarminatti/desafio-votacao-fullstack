package com.cooperativa.votacao.repository;

import com.cooperativa.votacao.domain.OpcaoVoto;
import com.cooperativa.votacao.domain.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaIdAndAssociadoId(Long pautaId, String associadoId);

    @Query("""
            select v.opcao as opcao, count(v) as quantidade
            from Voto v
            where v.sessao.id = :sessaoId
            group by v.opcao
            """)
    List<ResultadoVotacaoProjection> summarizeBySessaoId(Long sessaoId);

    long countBySessaoId(Long sessaoId);

    long countBySessaoIdAndOpcao(Long sessaoId, OpcaoVoto opcao);
}
