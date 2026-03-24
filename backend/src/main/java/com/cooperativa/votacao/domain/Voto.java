package com.cooperativa.votacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(name = "votos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_voto_pauta_associado", columnNames = {"pauta_id", "associado_id"})
}, indexes = {
        @Index(name = "idx_voto_sessao", columnList = "sessao_id"),
        @Index(name = "idx_voto_pauta", columnList = "pauta_id")
})
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoVotacao sessao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(name = "associado_id", nullable = false, length = 80)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private OpcaoVoto opcao;

    @Column(nullable = false)
    private OffsetDateTime votadoEm = OffsetDateTime.now();

    protected Voto() {
    }

    public Voto(SessaoVotacao sessao, Pauta pauta, String associadoId, OpcaoVoto opcao) {
        this.sessao = sessao;
        this.pauta = pauta;
        this.associadoId = associadoId;
        this.opcao = opcao;
    }

    public Long getId() {
        return id;
    }

    public SessaoVotacao getSessao() {
        return sessao;
    }

    public String getAssociadoId() {
        return associadoId;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public OpcaoVoto getOpcao() {
        return opcao;
    }

    public OffsetDateTime getVotadoEm() {
        return votadoEm;
    }
}
