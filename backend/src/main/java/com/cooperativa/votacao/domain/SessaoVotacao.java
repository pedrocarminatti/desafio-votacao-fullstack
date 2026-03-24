package com.cooperativa.votacao.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessoes_votacao", indexes = {
        @Index(name = "idx_sessao_pauta_abertura", columnList = "pauta_id, aberta_em")
})
public class SessaoVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(nullable = false)
    private OffsetDateTime abertaEm;

    @Column(nullable = false)
    private OffsetDateTime encerraEm;

    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Voto> votos = new ArrayList<>();

    protected SessaoVotacao() {
    }

    public SessaoVotacao(Pauta pauta, OffsetDateTime abertaEm, OffsetDateTime encerraEm) {
        this.pauta = pauta;
        this.abertaEm = abertaEm;
        this.encerraEm = encerraEm;
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public OffsetDateTime getAbertaEm() {
        return abertaEm;
    }

    public OffsetDateTime getEncerraEm() {
        return encerraEm;
    }

    public List<Voto> getVotos() {
        return votos;
    }

    public boolean estaAberta(OffsetDateTime agora) {
        return !agora.isBefore(abertaEm) && agora.isBefore(encerraEm);
    }
}
