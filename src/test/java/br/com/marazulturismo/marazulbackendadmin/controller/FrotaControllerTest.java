package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.model.User;
import br.com.marazulturismo.marazulbackendadmin.repository.UserRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.VeiculoRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre os critérios de aceitação da HU29 ponta a ponta: autenticação
 * obrigatória, campos obrigatórios, validação de ENUM, placa duplicada e
 * exclusão lógica.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FrotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VeiculoRepository veiculoRepository;

    private String token;

    private static final String VEICULO_VALIDO = """
            {
              "prefixo": "1001",
              "placa": "ABC1D23",
              "modelo": "MARCOPOLO",
              "tipo": "LD",
              "ano": 2022,
              "assentos": 46
            }
            """;

    @BeforeEach
    void setUp() {
        veiculoRepository.deleteAll();

        User admin = userRepository.save(new User(
                "Admin Teste",
                new Date(),
                Position.DRIVER,
                UserRole.ADMIN,
                "admin@marazul.test",
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void listar_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/frota"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void cadastrar_semToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/frota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEICULO_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listar_retornaVeiculoComTodosOsCamposPreenchidos() throws Exception {
        cadastrarVeiculoValido();

        mockMvc.perform(get("/api/frota").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].prefixo").value("1001"))
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"))
                .andExpect(jsonPath("$[0].modelo").value("MARCOPOLO"))
                .andExpect(jsonPath("$[0].tipo").value("LD"))
                .andExpect(jsonPath("$[0].ano").value(2022))
                .andExpect(jsonPath("$[0].assentos").value(46))
                .andExpect(jsonPath("$[0].status").value("ATIVO"));
    }

    @Test
    void cadastrar_semStatus_atribuiAtivo() throws Exception {
        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEICULO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.dataVistoria").doesNotExist());
    }

    @Test
    void cadastrar_incompleto_retorna400ComMensagemPorCampo() throws Exception {
        String semObrigatorios = """
                {
                  "placa": "ABC1D23"
                }
                """;

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semObrigatorios))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.prefixo").exists())
                .andExpect(jsonPath("$.modelo").exists())
                .andExpect(jsonPath("$.tipo").exists())
                .andExpect(jsonPath("$.ano").exists())
                .andExpect(jsonPath("$.assentos").exists());
    }

    @Test
    void cadastrar_tipoForaDoEnum_retorna400CitandoValoresAceitos() throws Exception {
        String tipoInvalido = VEICULO_VALIDO.replace("\"LD\"", "\"SEMI_LEITO\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tipoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("tipo")))
                .andExpect(jsonPath("$.erro").value(containsString("CONVENCIONAL")));
    }

    @Test
    void cadastrar_placaDuplicada_retorna409() throws Exception {
        cadastrarVeiculoValido();

        String outroPrefixo = VEICULO_VALIDO.replace("\"1001\"", "\"1002\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outroPrefixo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("ABC1D23")));
    }

    @Test
    void cadastrar_placaDuplicadaEmCaixaBaixa_retorna409() throws Exception {
        cadastrarVeiculoValido();

        String mesmaPlacaMinuscula = VEICULO_VALIDO.replace("\"ABC1D23\"", "\"abc1d23\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mesmaPlacaMinuscula))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastrar_placaComHifen_normalizaEAceita() throws Exception {
        String comHifen = VEICULO_VALIDO.replace("\"ABC1D23\"", "\"abc-1234\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comHifen))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1234"));
    }

    @Test
    void cadastrar_placaComHifenDuplicandoExistente_retorna409() throws Exception {
        cadastrarVeiculoValido();

        // ABC1D23 ja cadastrada; "abc-1d23" e a mesma placa em outro formato.
        String mesmaPlacaComHifen = VEICULO_VALIDO.replace("\"ABC1D23\"", "\"abc-1d23\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mesmaPlacaComHifen))
                .andExpect(status().isConflict());
    }

    @Test
    void listarEnums_retornaCatalogoCompletoComDescricoes() throws Exception {
        mockMvc.perform(get("/api/frota/enums").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipos", hasSize(3)))
                .andExpect(jsonPath("$.modelos", hasSize(4)))
                .andExpect(jsonPath("$.status", hasSize(3)))
                .andExpect(jsonPath("$.tipos[?(@.valor=='CONVENCIONAL')].descricao").value("Convencional"))
                .andExpect(jsonPath("$.modelos[?(@.valor=='IRIZAR_BRASIL')].descricao").value("Irizar Brasil"))
                .andExpect(jsonPath("$.status[?(@.valor=='EM_MANUTENCAO')].descricao").value("Em manutenção"));
    }

    @Test
    void listarEnums_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/frota/enums"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarEnums_naoColideComBuscaPorId() throws Exception {
        // /api/frota/enums nao pode ser interpretado como /api/frota/{id}
        mockMvc.perform(get("/api/frota/enums").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipos").exists());
    }

    @Test
    void cadastrar_placaEmFormatoInvalido_retorna400() throws Exception {
        String placaInvalida = VEICULO_VALIDO.replace("\"ABC1D23\"", "\"12345\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(placaInvalida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.placa").exists());
    }

    @Test
    void excluir_inativaEMantemRegistroConsultavel() throws Exception {
        Long id = idDoVeiculoCadastrado();

        mockMvc.perform(delete("/api/frota/{id}", id).header("Authorization", token))
                .andExpect(status().isNoContent());

        // Continua consultável, preservando o histórico...
        mockMvc.perform(get("/api/frota/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));

        // ...mas sai da seleção de novas viagens.
        mockMvc.perform(get("/api/frota").param("status", "ATIVO").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void atualizar_alteraStatusParaEmManutencao() throws Exception {
        Long id = idDoVeiculoCadastrado();

        String emManutencao = VEICULO_VALIDO.replace(
                "\"assentos\": 46", "\"assentos\": 46,\n  \"status\": \"EM_MANUTENCAO\"");

        mockMvc.perform(put("/api/frota/{id}", id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emManutencao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_MANUTENCAO"));
    }

    @Test
    void buscarPorId_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/frota/{id}", 9999).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void listar_comStatusForaDoEnum_retorna400() throws Exception {
        mockMvc.perform(get("/api/frota").param("status", "SUCATEADO").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("EM_MANUTENCAO")));
    }

    @Test
    void conflito_trazMesmaMensagemEmErroEEmMessage() throws Exception {
        cadastrarVeiculoValido();

        String outroPrefixo = VEICULO_VALIDO.replace("\"1001\"", "\"1002\"");

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outroPrefixo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("ABC1D23")))
                .andExpect(jsonPath("$.message").value(containsString("ABC1D23")));
    }

    @Test
    void erroDeValidacao_mantemCamposNaRaizEAdicionaMessage() throws Exception {
        String semObrigatorios = """
                {
                  "placa": "ABC1D23"
                }
                """;

        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semObrigatorios))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.prefixo").exists())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void naoAutenticado_trazErroEMessage() throws Exception {
        mockMvc.perform(get("/api/frota"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void naoEncontrado_trazErroEMessage() throws Exception {
        mockMvc.perform(get("/api/frota/{id}", 9999).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    private void cadastrarVeiculoValido() throws Exception {
        mockMvc.perform(post("/api/frota")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VEICULO_VALIDO))
                .andExpect(status().isCreated());
    }

    private Long idDoVeiculoCadastrado() throws Exception {
        cadastrarVeiculoValido();
        return veiculoRepository.findAll().get(0).getId();
    }
}
