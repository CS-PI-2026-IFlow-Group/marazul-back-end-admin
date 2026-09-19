package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CollaboratorRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CollaboratorRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String adminEmail = "admin@marazul.com.br";

        if (!userRepository.existsByEmail(adminEmail)) {
            Collaborator admin = new Collaborator(
                    "Administrador",
                    new Date(),
                    Position.OTHER,
                    UserRole.ADMIN,
                    adminEmail,
                    "(11) 99999-9999",
                    null,
                    passwordEncoder.encode("Admin@123")
            );
            userRepository.save(admin);
            log.info("==================================================");
            log.info(" [SEED] Usuário ADMIN padrão criado com sucesso!");
            log.info(" Email: {}", adminEmail);
            log.info(" Senha: {}", "Admin@123");
            log.info("==================================================");
        }
    }
}
