package com.cooperativa.votacao;

import com.cooperativa.votacao.client.CpfValidationClient;
import com.cooperativa.votacao.client.CpfValidationResult;
import com.cooperativa.votacao.client.VotingPermissionStatus;
import com.cooperativa.votacao.repository.PautaRepository;
import com.cooperativa.votacao.repository.SessaoVotacaoRepository;
import com.cooperativa.votacao.repository.VotoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private SessaoVotacaoRepository sessaoVotacaoRepository;

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CpfValidationClient cpfValidationClient;

    @BeforeEach
    void limparBanco() {
        votoRepository.deleteAll();
        sessaoVotacaoRepository.deleteAll();
        pautaRepository.deleteAll();
        when(cpfValidationClient.validate(anyString()))
                .thenReturn(new CpfValidationResult(true, VotingPermissionStatus.ABLE_TO_VOTE));
    }

    @Test
    void deveCriarPautaAbrirSessaoRegistrarVotoEApurar() throws Exception {
        MvcResult criacao = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Pauta Teste",
                                  "descricao": "Descricao Teste"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        long pautaId = extrairId(criacao);

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "duracaoSegundos": 120
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aberta").value(true));

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "associadoId": "123",
                                  "cpf": "12345678901",
                                  "opcao": "SIM"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votosSim").value(1))
                .andExpect(jsonPath("$.votosNao").value(0))
                .andExpect(jsonPath("$.situacao").value("APROVADA"));
    }

    @Test
    void deveImpedirSegundoVotoDoMesmoAssociadoNaMesmaPautaMesmoEmNovaSessao() throws Exception {
        MvcResult criacao = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Pauta Teste2",
                                  "descricao": "Descricao Teste2"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        long pautaId = extrairId(criacao);

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "duracaoSegundos": 1
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "associadoId": "assoc-9",
                                  "cpf": "12345678901",
                                  "opcao": "SIM"
                                }
                                """))
                .andExpect(status().isCreated());

        Thread.sleep(1100);

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "duracaoSegundos": 60
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "associadoId": "assoc-9",
                                  "cpf": "12345678901",
                                  "opcao": "NAO"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Associado ja votou nesta pauta"));
    }

    @Test
    void deveRetornar404QuandoCpfForInvalidoNoSistemaExterno() throws Exception {
        when(cpfValidationClient.validate("00000000000"))
                .thenReturn(new CpfValidationResult(false, null));

        long pautaId = criarPauta();
        abrirSessao(pautaId, 60);

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "associadoId": "assoc-404",
                                  "cpf": "00000000000",
                                  "opcao": "SIM"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("CPF invalido no sistema externo"));
    }

    @Test
    void deveRetornarErroQuandoAssociadoEstiverInaptoParaVotar() throws Exception {
        when(cpfValidationClient.validate("99999999999"))
                .thenReturn(new CpfValidationResult(true, VotingPermissionStatus.UNABLE_TO_VOTE));

        long pautaId = criarPauta();
        abrirSessao(pautaId, 60);

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "associadoId": "assoc-422",
                                  "cpf": "99999999999",
                                  "opcao": "SIM"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Associado nao esta apto a votar"));
    }

    private long extrairId(MvcResult result) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("id").asLong();
    }

    private long criarPauta() throws Exception {
        MvcResult criacao = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Pauta Teste3",
                                  "descricao": "Descricao Teste3"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return extrairId(criacao);
    }

    private void abrirSessao(long pautaId, long duracaoSegundos) throws Exception {
        mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessao", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "duracaoSegundos": %d
                                }
                                """.formatted(duracaoSegundos)))
                .andExpect(status().isCreated());
    }
}
