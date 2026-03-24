package com.cooperativa.votacao.service;

import com.cooperativa.votacao.client.CpfValidationClient;
import com.cooperativa.votacao.client.CpfValidationResult;
import com.cooperativa.votacao.client.VotingPermissionStatus;
import com.cooperativa.votacao.domain.OpcaoVoto;
import com.cooperativa.votacao.domain.Pauta;
import com.cooperativa.votacao.domain.SessaoVotacao;
import com.cooperativa.votacao.domain.Voto;
import com.cooperativa.votacao.dto.AbrirSessaoRequest;
import com.cooperativa.votacao.dto.CriarPautaRequest;
import com.cooperativa.votacao.dto.PautaResponse;
import com.cooperativa.votacao.dto.RegistrarVotoRequest;
import com.cooperativa.votacao.dto.ResultadoResponse;
import com.cooperativa.votacao.dto.SessaoResponse;
import com.cooperativa.votacao.exception.BusinessException;
import com.cooperativa.votacao.exception.NotFoundException;
import com.cooperativa.votacao.repository.PautaRepository;
import com.cooperativa.votacao.repository.ResultadoVotacaoProjection;
import com.cooperativa.votacao.repository.SessaoVotacaoRepository;
import com.cooperativa.votacao.repository.VotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PautaService {

    private static final long DURACAO_PADRAO_SEGUNDOS = 60L;
    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final CpfValidationClient cpfValidationClient;

    public PautaService(PautaRepository pautaRepository,
                        SessaoVotacaoRepository sessaoRepository,
                        VotoRepository votoRepository,
                        CpfValidationClient cpfValidationClient) {
        this.pautaRepository = pautaRepository;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.cpfValidationClient = cpfValidationClient;
    }

    @Transactional
    public PautaResponse criar(CriarPautaRequest request) {
        Pauta pauta = pautaRepository.save(new Pauta(request.titulo(), request.descricao()));
        log.info("Pauta {} criada com titulo '{}'", pauta.getId(), pauta.getTitulo());
        return toResponse(pauta, null);
    }

    @Transactional(readOnly = true)
    public List<PautaResponse> listar() {
        return pautaRepository.findAll()
                .stream()
                .map(pauta -> toResponse(pauta, buscarSessaoAtual(pauta.getId())))
                .toList();
    }

    @Transactional
    public SessaoResponse abrirSessao(Long pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new NotFoundException("Pauta nao encontrada"));

        SessaoVotacao sessaoExistente = buscarSessaoAtual(pautaId);
        OffsetDateTime agora = OffsetDateTime.now();
        if (sessaoExistente != null && sessaoExistente.estaAberta(agora)) {
            log.warn("Tentativa de abrir nova sessao para pauta {} com sessao ainda aberta", pautaId);
            throw new BusinessException("Ja existe uma sessao aberta para esta pauta");
        }

        long duracao = request != null && request.duracaoSegundos() != null
                ? request.duracaoSegundos()
                : DURACAO_PADRAO_SEGUNDOS;

        SessaoVotacao sessao = sessaoRepository.save(new SessaoVotacao(
                pauta,
                agora,
                agora.plusSeconds(duracao)
        ));
        log.info("Sessao {} aberta para pauta {} com duracao de {} segundos", sessao.getId(), pautaId, duracao);

        return toSessaoResponse(sessao);
    }

    @Transactional
    public void registrarVoto(Long pautaId, RegistrarVotoRequest request) {
        Pauta pauta = buscarPauta(pautaId);
        SessaoVotacao sessao = obterSessaoAberta(pautaId);
        validarCpf(request.cpf());

        if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, request.associadoId())) {
            log.warn("Associado {} tentou votar novamente na pauta {}", request.associadoId(), pautaId);
            throw new BusinessException("Associado ja votou nesta pauta");
        }

        votoRepository.save(new Voto(sessao, pauta, request.associadoId(), request.opcao()));
        log.info("Voto '{}' registrado para pauta {} pelo associado {}", request.opcao(), pautaId, request.associadoId());
    }

    @Transactional(readOnly = true)
    public ResultadoResponse obterResultado(Long pautaId) {
        buscarPauta(pautaId);
        SessaoVotacao sessao = buscarSessaoAtual(pautaId);
        if (sessao == null) {
            log.warn("Resultado solicitado para pauta {} sem sessao cadastrada", pautaId);
            throw new BusinessException("A pauta ainda nao possui sessao de votacao");
        }

        log.info("Apurando resultado da pauta {} com base na sessao {}", pautaId, sessao.getId());
        return obterResultadoSeguro(sessao);
    }

    private Pauta buscarPauta(Long pautaId) {
        return pautaRepository.findById(pautaId)
                .orElseThrow(() -> new NotFoundException("Pauta nao encontrada"));
    }

    private SessaoVotacao obterSessaoAberta(Long pautaId) {
        SessaoVotacao sessao = buscarSessaoAtual(pautaId);
        if (sessao == null) {
            log.warn("Tentativa de votar na pauta {} sem sessao aberta", pautaId);
            throw new BusinessException("A pauta nao possui sessao aberta");
        }

        if (!sessao.estaAberta(OffsetDateTime.now())) {
            log.warn("Tentativa de votar na pauta {} com sessao {} encerrada", pautaId, sessao.getId());
            throw new BusinessException("A sessao de votacao esta encerrada");
        }

        return sessao;
    }

    private SessaoVotacao buscarSessaoAtual(Long pautaId) {
        return sessaoRepository.findFirstByPautaIdOrderByAbertaEmDesc(pautaId).orElse(null);
    }

    private PautaResponse toResponse(Pauta pauta, SessaoVotacao sessao) {
        ResultadoResponse resultado = sessao == null ? null : obterResultadoSeguro(sessao);
        return new PautaResponse(
                pauta.getId(),
                pauta.getTitulo(),
                pauta.getDescricao(),
                pauta.getCriadaEm(),
                sessao == null ? null : toSessaoResponse(sessao),
                resultado
        );
    }

    private ResultadoResponse obterResultadoSeguro(SessaoVotacao sessao) {
        long votosSim = 0L;
        long votosNao = 0L;

        for (ResultadoVotacaoProjection resumo : votoRepository.summarizeBySessaoId(sessao.getId())) {
            if (resumo.getOpcao() == OpcaoVoto.SIM) {
                votosSim = resumo.getQuantidade();
            }
            if (resumo.getOpcao() == OpcaoVoto.NAO) {
                votosNao = resumo.getQuantidade();
            }
        }

        long total = votosSim + votosNao;
        String situacao = votosSim > votosNao ? "APROVADA" : votosNao > votosSim ? "REJEITADA" : "EMPATE";
        return new ResultadoResponse(votosSim, votosNao, total, situacao);
    }

    private void validarCpf(String cpf) {
        CpfValidationResult cpfValidationResult = cpfValidationClient.validate(cpf);
        if (!cpfValidationResult.cpfValido()) {
            log.warn("CPF {} rejeitado pelo sistema externo", cpf);
            throw new NotFoundException("CPF invalido no sistema externo");
        }

        if (cpfValidationResult.status() == VotingPermissionStatus.UNABLE_TO_VOTE) {
            log.warn("CPF {} validado mas sem permissao para votar", cpf);
            throw new BusinessException("Associado nao esta apto a votar");
        }

        log.info("CPF {} validado com permissao para votar", cpf);
    }

    private SessaoResponse toSessaoResponse(SessaoVotacao sessao) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getAbertaEm(),
                sessao.getEncerraEm(),
                sessao.estaAberta(OffsetDateTime.now())
        );
    }
}
