package br.com.marazulturismo.marazulbackendadmin.controller;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.service.JwtService;
import br.com.marazulturismo.marazulbackendadmin.service.PasswordSetupEmailService;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Envio do e-mail de definição de senha pelos dois caminhos de criação de colaborador
 * (POST /api/funcionario e POST /api/auth/register) e pelo reenvio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordSetupEmailTest {

    private static final long TOKEN_EXPIRATION_MS = 15L * 60 * 1000;
    private static final String DEFINE_PASSWORD_LINK = "http://localhost:3000/define-password?token=";

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Profile adminProfile;
    private String token;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));

        adminProfile = profileRepository.findByName("Administrador").orElseThrow();
        Collaborator admin = collaboratorRepository.save(new Collaborator(
                "Administrador E-mail",
                new Date(),
                Position.OTHER,
                true,
                adminProfile,
                "admin.email@marazul.test",
                null,
                null,
                "hash-irrelevante"));
        token = "Bearer " + jwtService.generateToken(admin);
    }

    @Test
    void createEmployee_withAccess_sendsEmailWithValidToken() throws Exception {
        long before = System.currentTimeMillis();

        mockMvc.perform(post("/api/funcionario")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("painel@marazul.test", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warning").doesNotExist());

        assertSetupEmailSentTo("painel@marazul.test", before);
    }

    @Test
    void createEmployee_withoutAccess_sendsNothingAndCreatesNoToken() throws Exception {
        mockMvc.perform(post("/api/funcionario")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("sem.acesso@marazul.test", false)))
                .andExpect(status().isCreated());

        assertNoEmailAndNoToken("sem.acesso@marazul.test");
    }

    @Test
    void createEmployee_whenMailServerFails_keepsEmployeeAndReturnsWarning() throws Exception {
        doThrow(new MailSendException("SMTP indisponível")).when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(post("/api/funcionario")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("falha.smtp@marazul.test", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.warning").value(PasswordSetupEmailService.SEND_FAILURE_WARNING));

        assertThat(collaboratorRepository.findByEmail("falha.smtp@marazul.test")).isPresent();
    }

    @Test
    void register_withAccess_sendsEmailWithValidToken() throws Exception {
        long before = System.currentTimeMillis();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("registro@marazul.test", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aviso").doesNotExist());

        assertSetupEmailSentTo("registro@marazul.test", before);
    }

    @Test
    void register_withoutAccess_sendsNothingAndCreatesNoToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("registro.sem.acesso@marazul.test", false)))
                .andExpect(status().isCreated());

        assertNoEmailAndNoToken("registro.sem.acesso@marazul.test");
    }

    @Test
    void register_whenMailServerFails_keepsCollaboratorAndReturnsWarning() throws Exception {
        doThrow(new MailSendException("SMTP indisponível")).when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("registro.falha@marazul.test", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aviso").value(PasswordSetupEmailService.SEND_FAILURE_WARNING));

        assertThat(collaboratorRepository.findByEmail("registro.falha@marazul.test")).isPresent();
    }

    @Test
    void resend_forEmployeeWithAccess_sendsNewToken() throws Exception {
        Collaborator employee = saveEmployee("reenvio@marazul.test", true);
        employee.setPasswordResetToken("token-antigo", new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_MS));
        collaboratorRepository.save(employee);
        long before = System.currentTimeMillis();

        mockMvc.perform(post("/api/funcionario/{id}/reenviar-email", employee.getId())
                        .header("Authorization", token))
                .andExpect(status().isOk());

        String newToken = assertSetupEmailSentTo("reenvio@marazul.test", before);
        assertThat(newToken).isNotEqualTo("token-antigo");
    }

    @Test
    void resend_forEmployeeWithoutAccess_returns400AndSendsNothing() throws Exception {
        Collaborator employee = saveEmployee("reenvio.sem.acesso@marazul.test", false);

        mockMvc.perform(post("/api/funcionario/{id}/reenviar-email", employee.getId())
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());

        assertNoEmailAndNoToken("reenvio.sem.acesso@marazul.test");
    }

    @Test
    void resend_whenMailServerFails_returns503WithWarning() throws Exception {
        Collaborator employee = saveEmployee("reenvio.falha@marazul.test", true);
        doThrow(new MailSendException("SMTP indisponível")).when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(post("/api/funcionario/{id}/reenviar-email", employee.getId())
                        .header("Authorization", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value(PasswordSetupEmailService.SEND_FAILURE_WARNING));
    }

    @Test
    void tokenFromEmail_definesPassword() throws Exception {
        mockMvc.perform(post("/api/funcionario")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("define.senha@marazul.test", true)))
                .andExpect(status().isCreated());
        String emailToken = assertSetupEmailSentTo("define.senha@marazul.test", 0);

        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"%s","novaSenha":"Senha@123"}
                                """.formatted(emailToken)))
                .andExpect(status().isOk());

        Collaborator saved = collaboratorRepository.findByEmail("define.senha@marazul.test").orElseThrow();
        assertThat(passwordEncoder.matches("Senha@123", saved.getPasswordHash())).isTrue();
        assertThat(saved.getResetToken()).isNull();
    }

    @Test
    void expiredToken_returns400WithClearMessage() throws Exception {
        Collaborator employee = saveEmployee("token.expirado@marazul.test", true);
        employee.setPasswordResetToken("token-expirado", new Date(System.currentTimeMillis() - 1000));
        collaboratorRepository.save(employee);

        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"token-expirado","novaSenha":"Senha@123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token inválido ou expirado."));
    }

    @Test
    void setupEmail_hasSharedLayoutButtonAndTextLink() throws Exception {
        mockMvc.perform(post("/api/funcionario")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content(collaboratorJson("visual@marazul.test", true)))
                .andExpect(status().isCreated());
        String emailToken = assertSetupEmailSentTo("visual@marazul.test", 0);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertBrandedEmail(captor.getValue(), DEFINE_PASSWORD_LINK + emailToken, "Definir minha senha");
    }

    @Test
    void resetEmail_hasSharedLayoutButtonAndTextLink() throws Exception {
        saveEmployee("recuperar@marazul.test", true);

        mockMvc.perform(post("/api/auth/recuperar-senha")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"recuperar@marazul.test"}
                                """))
                .andExpect(status().isOk());

        // A recuperação continua assíncrona
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        String resetToken = collaboratorRepository.findByEmail("recuperar@marazul.test").orElseThrow().getResetToken();
        assertBrandedEmail(captor.getValue(), "http://localhost:3000/reset-password?token=" + resetToken,
                "Redefinir minha senha");
    }

    // Remetente identificado, layout compartilhado, botão + link em texto, prazo e nada externo
    private static void assertBrandedEmail(MimeMessage message, String link, String buttonLabel) throws Exception {
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("no-reply@marazul.test");
        assertThat(from.getPersonal()).isEqualTo("Marazul Turismo");

        String html = htmlOf(message);
        assertThat(html)
                .contains("Marazul Turismo")
                .contains("Sistema administrativo")
                .contains(buttonLabel)
                .contains("Este link é válido por <strong>15 minutos</strong>")
                .doesNotContain("<style")
                .doesNotContain("<link")
                .doesNotContainPattern("src=\"(?!https?://|cid:)");
        // Uma vez no href do botão, outra no href do link em texto e outra como texto visível
        assertThat(html.split(java.util.regex.Pattern.quote(link), -1)).hasSize(4);
    }

    // Confere destinatário, link e validade de 15 minutos; devolve o token enviado
    private String assertSetupEmailSentTo(String email, long sentAfterMs) throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();

        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(email);

        Collaborator saved = collaboratorRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getResetToken()).isNotBlank();
        assertThat(htmlOf(message)).contains(DEFINE_PASSWORD_LINK + saved.getResetToken());

        if (sentAfterMs > 0) {
            assertThat(saved.getResetTokenExpiration().getTime())
                    .isBetween(sentAfterMs + TOKEN_EXPIRATION_MS, System.currentTimeMillis() + TOKEN_EXPIRATION_MS);
        }
        return saved.getResetToken();
    }

    private void assertNoEmailAndNoToken(String email) {
        verify(mailSender, never()).send(any(MimeMessage.class));
        Collaborator saved = collaboratorRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getResetToken()).isNull();
    }

    private static String htmlOf(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        Multipart multipart = (Multipart) content;
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            html.append(htmlOf(multipart.getBodyPart(i)));
        }
        return html.toString();
    }

    private Collaborator saveEmployee(String email, boolean isUser) {
        return collaboratorRepository.save(new Collaborator(
                "Colaborador Teste",
                new Date(),
                Position.OTHER,
                isUser,
                adminProfile,
                email,
                null,
                null,
                null));
    }

    private String collaboratorJson(String email, boolean isUser) {
        return """
                {
                  "name": "Colaborador Novo",
                  "email": "%s",
                  "isUser": %s,
                  "profileId": %d,
                  "position": "OTHER"
                }
                """.formatted(email, isUser, adminProfile.getId());
    }
}
