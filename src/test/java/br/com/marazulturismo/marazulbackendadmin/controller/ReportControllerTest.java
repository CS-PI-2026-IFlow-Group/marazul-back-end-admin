package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.dto.ReportColumnDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportContentDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportFilterDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportPeriodDTO;
import br.com.marazulturismo.marazulbackendadmin.dto.ReportTotalDTO;
import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportDefaultPeriod;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportFilterType;
import br.com.marazulturismo.marazulbackendadmin.enums.ReportValueFormat;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import br.com.marazulturismo.marazulbackendadmin.service.ReportDefinition;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerTest {

    private static final String SLUG = "relatorio-de-teste";
    private static final String FILTERED_SLUG = "relatorio-com-filtro";

    @TestConfiguration
    static class StubReportConfiguration {

        @Bean
        ReportDefinition stubReportDefinition() {
            return new ReportDefinition() {

                @Override
                public String slug() {
                    return SLUG;
                }

                @Override
                public String title() {
                    return "Relatório de Teste";
                }

                @Override
                public List<ReportColumnDTO> columns() {
                    return List.of(
                            ReportColumnDTO.text("destino", "Destino"),
                            ReportColumnDTO.number("quantidade", "Quantidade"),
                            ReportColumnDTO.money("valor", "Valor"),
                            ReportColumnDTO.percent("percentual", "Percentual"),
                            ReportColumnDTO.date("data", "Data"),
                            ReportColumnDTO.centered("status", "Situação"));
                }

                @Override
                public ReportDefaultPeriod defaultPeriod() {
                    return ReportDefaultPeriod.SEMANA;
                }

                @Override
                public ReportContentDTO content(ReportPeriodDTO period) {
                    return ReportContentDTO.of(
                            List.of(Map.of(
                                    "destino", "Camboriú",
                                    "quantidade", 3,
                                    "valor", new BigDecimal("1250.50"),
                                    "percentual", new BigDecimal("75.0"),
                                    "data", LocalDate.of(2026, 1, 15))),
                            List.of(
                                    new ReportTotalDTO(
                                            "valor", "Total recebido", ReportValueFormat.MOEDA,
                                            new BigDecimal("1250.50")),
                                    new ReportTotalDTO("observacao", "Observação", "Total em texto")));
                }
            };
        }

        @Bean
        ReportDefinition filteredReportDefinition() {
            return new ReportDefinition() {

                @Override
                public String slug() {
                    return FILTERED_SLUG;
                }

                @Override
                public String title() {
                    return "Relatório com Filtro";
                }

                @Override
                public List<ReportColumnDTO> columns() {
                    return List.of(ReportColumnDTO.text("clienteId", "Cliente"));
                }

                @Override
                public List<ReportFilterDTO> filters() {
                    return List.of(new ReportFilterDTO("clienteId", "Cliente", ReportFilterType.CLIENTE));
                }

                @Override
                public ReportContentDTO content(ReportPeriodDTO period) {
                    return content(period, Map.of());
                }

                @Override
                public ReportContentDTO content(ReportPeriodDTO period, Map<String, String> filters) {
                    return ReportContentDTO.of(List.of(new LinkedHashMap<String, Object>(filters)));
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private String token;
    private String tokenWithoutPermission;

    @Test
    void reportDefinition_defaultsToMonthPeriod() {
        assertEquals(ReportDefaultPeriod.MES, minimalDefinition(ReportContentDTO.empty()).defaultPeriod());
    }

    @Test
    void reportDefinition_withoutFilters_ignoresTheReceivedValues() {
        ReportContentDTO content = ReportContentDTO.empty();
        ReportDefinition definition = minimalDefinition(content);

        assertEquals(List.of(), definition.filters());
        assertSame(content, definition.content(null, Map.of("clienteId", "1")));
    }

    private static ReportDefinition minimalDefinition(ReportContentDTO content) {
        return new ReportDefinition() {
            @Override
            public String slug() {
                return "relatorio-padrao";
            }

            @Override
            public String title() {
                return "Relatório Padrão";
            }

            @Override
            public List<ReportColumnDTO> columns() {
                return List.of();
            }

            @Override
            public ReportContentDTO content(ReportPeriodDTO period) {
                return content;
            }
        };
    }

    @BeforeEach
    void setUp() {
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Admin Relatorios",
                new Date(),
                Position.OTHER,
                true,
                profileRepository.findByName("Administrador").orElseThrow(),
                "admin.relatorios@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        token = "Bearer " + jwtService.generateToken(admin);

        Profile limited = profileRepository.save(new Profile("Perfil Sem Relatorios"));
        limited.grantPermissions(permissionRepository.findAll().stream()
                .filter(it -> it.getBaseRoute().equals("frota"))
                .map(Permission.class::cast)
                .toList());

        Collaborator withoutPermission = collaboratorRepository.save(new Collaborator(
                "Sem Relatorios",
                new Date(),
                Position.OTHER,
                true,
                limited,
                "sem.relatorios@marazul.test",
                null,
                null,
                "hash-irrelevante"));

        tokenWithoutPermission = "Bearer " + jwtService.generateToken(withoutPermission);
    }

    @Test
    void list_returnsAvailableReports() throws Exception {
        mockMvc.perform(get("/api/relatorios").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].titulo", SLUG).value("Relatório de Teste"))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].exigePeriodo", SLUG).value(true))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].periodoPadrao", SLUG).value("SEMANA"));
    }

    @Test
    void list_showsTheFiltersEachReportAccepts() throws Exception {
        mockMvc.perform(get("/api/relatorios").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].filtros[0]", SLUG).doesNotExist())
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].filtros[0].chave", FILTERED_SLUG).value("clienteId"))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].filtros[0].titulo", FILTERED_SLUG).value("Cliente"))
                .andExpect(jsonPath("$[?(@.relatorio == '%s')].filtros[0].tipo", FILTERED_SLUG).value("CLIENTE"));
    }

    @Test
    void table_passesOnlyTheDeclaredFiltersToTheReport() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", FILTERED_SLUG)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .param("clienteId", " 7 ")
                        .param("outro", "ignorado")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].clienteId").value("7"))
                .andExpect(jsonPath("$.linhas[0].outro").doesNotExist())
                .andExpect(jsonPath("$.linhas[0].dataInicio").doesNotExist());
    }

    @Test
    void table_withBlankFilter_doesNotPassItToTheReport() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", FILTERED_SLUG)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .param("clienteId", "  ")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linhas[0].clienteId").doesNotExist());
    }

    @Test
    void table_withPeriod_returnsHeaderRowsAndTotals() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorio").value(SLUG))
                .andExpect(jsonPath("$.titulo").value("Relatório de Teste"))
                .andExpect(jsonPath("$.periodo.dataInicio").value("2026-01-01"))
                .andExpect(jsonPath("$.periodo.dataFim").value("2026-01-31"))
                .andExpect(jsonPath("$.colunas", hasSize(6)))
                .andExpect(jsonPath("$.colunas[0].chave").value("destino"))
                .andExpect(jsonPath("$.colunas[0].formato").value("TEXTO"))
                .andExpect(jsonPath("$.colunas[1].alinhamento").value("RIGHT"))
                .andExpect(jsonPath("$.colunas[1].formato").value("NUMERO"))
                .andExpect(jsonPath("$.colunas[2].alinhamento").value("RIGHT"))
                .andExpect(jsonPath("$.colunas[2].formato").value("MOEDA"))
                .andExpect(jsonPath("$.colunas[3].alinhamento").value("RIGHT"))
                .andExpect(jsonPath("$.colunas[3].formato").value("PERCENTUAL"))
                .andExpect(jsonPath("$.colunas[4].alinhamento").value("CENTER"))
                .andExpect(jsonPath("$.colunas[4].formato").value("DATA"))
                .andExpect(jsonPath("$.colunas[5].alinhamento").value("CENTER"))
                .andExpect(jsonPath("$.colunas[5].formato").value("TEXTO"))
                .andExpect(jsonPath("$.linhas", hasSize(1)))
                .andExpect(jsonPath("$.linhas[0].destino").value("Camboriú"))
                .andExpect(jsonPath("$.totais[0].titulo").value("Total recebido"))
                .andExpect(jsonPath("$.totais[0].formato").value("MOEDA"))
                .andExpect(jsonPath("$.totais[1].formato").value("TEXTO"))
                .andExpect(jsonPath("$.emitidoEm").exists());
    }

    @Test
    void table_withoutPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("obrigatórias")));
    }

    @Test
    void table_withStartDateAfterEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .param("dataInicio", "2026-02-10")
                        .param("dataFim", "2026-02-01")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("data inicial")));
    }

    @Test
    void table_withUnknownReport_returns404() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", "nao-existe")
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void pdf_returnsApplicationPdfWithFileName() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}/pdf", SLUG)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        containsString("relatorio-" + SLUG + "-2026-01-01-a-2026-01-31.pdf")));
    }

    @Test
    void pdf_passesTheDeclaredFiltersToTheReport() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/relatorios/{relatorio}/pdf", FILTERED_SLUG)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .param("clienteId", "4821")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (PDDocument document = PDDocument.load(pdf)) {
            assertThat(new PDFTextStripper().getText(document)).contains("4821");
        }
    }

    @Test
    void pdf_requestedByThePanel_exposesTheFileNameHeader() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}/pdf", SLUG)
                        .param("dataInicio", "2026-01-01")
                        .param("dataFim", "2026-01-31")
                        .header("Authorization", token)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Content-Disposition")));
    }

    @Test
    void table_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void table_withoutReportPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/relatorios/{relatorio}", SLUG)
                        .header("Authorization", tokenWithoutPermission))
                .andExpect(status().isForbidden());
    }
}
