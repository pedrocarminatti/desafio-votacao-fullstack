package com.cooperativa.votacao.controller;

import com.cooperativa.votacao.dto.AbrirSessaoRequest;
import com.cooperativa.votacao.dto.CriarPautaRequest;
import com.cooperativa.votacao.dto.PautaResponse;
import com.cooperativa.votacao.dto.RegistrarVotoRequest;
import com.cooperativa.votacao.dto.ResultadoResponse;
import com.cooperativa.votacao.dto.SessaoResponse;
import com.cooperativa.votacao.service.PautaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/pautas", "/api/v1/pautas"})
public class PautaController {

    private final PautaService pautaService;

    public PautaController(PautaService pautaService) {
        this.pautaService = pautaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PautaResponse criar(@Valid @RequestBody CriarPautaRequest request) {
        return pautaService.criar(request);
    }

    @GetMapping
    public List<PautaResponse> listar() {
        return pautaService.listar();
    }

    @PostMapping("/{pautaId}/sessao")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse abrirSessao(@PathVariable Long pautaId,
                                      @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        return pautaService.abrirSessao(pautaId, request);
    }

    @PostMapping("/{pautaId}/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public void votar(@PathVariable Long pautaId, @Valid @RequestBody RegistrarVotoRequest request) {
        pautaService.registrarVoto(pautaId, request);
    }

    @GetMapping("/{pautaId}/resultado")
    public ResultadoResponse resultado(@PathVariable Long pautaId) {
        return pautaService.obterResultado(pautaId);
    }
}
