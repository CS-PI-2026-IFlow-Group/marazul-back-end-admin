package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public static final String ADMIN_PROFILE_NAME = "Administrador";

    // Uma permissão por rota e funcionalidade: create (POST), view (GET), edit (PUT), delete (DELETE).
    // As rotas seguem as URLs da API, em português. /api/auth fica de fora: é pública ou comum a qualquer usuário logado.
    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed("dashboard", "view"),
            new PermissionSeed("funcionario", "create"),
            new PermissionSeed("funcionario", "view"),
            new PermissionSeed("funcionario", "edit"),
            new PermissionSeed("funcionario", "delete"),
            new PermissionSeed("frota", "create"),
            new PermissionSeed("frota", "view"),
            new PermissionSeed("frota", "edit"),
            new PermissionSeed("frota", "delete"),
            new PermissionSeed("perfis", "view"),
            new PermissionSeed("permissoes", "view")
    );

    private final CollaboratorRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            CollaboratorRepository userRepository,
            PermissionRepository permissionRepository,
            ProfileRepository profileRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedAdminProfile();
        seedAdminUser();
    }

    private void seedPermissions() {
        for (PermissionSeed seed : PERMISSIONS) {
            if (!permissionRepository.existsByBaseRouteAndFeature(seed.baseRoute(), seed.feature())) {
                permissionRepository.save(new Permission(seed.baseRoute(), seed.feature()));
                log.info(" [SEED] Permissão criada: {} - {}", seed.baseRoute(), seed.feature());
            }
        }
    }

    private void seedAdminProfile() {
        Profile admin = profileRepository.findByName(ADMIN_PROFILE_NAME)
                .orElseGet(() -> {
                    log.info(" [SEED] Perfil {} criado.", ADMIN_PROFILE_NAME);
                    return profileRepository.save(new Profile(ADMIN_PROFILE_NAME));
                });

        // Também roda em bases já existentes, para o admin receber as permissões de rotas novas.
        if (admin.grantPermissions(permissionRepository.findAll())) {
            log.info(" [SEED] Perfil {} agora tem {} permissões.", ADMIN_PROFILE_NAME, admin.getPermissions().size());
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin@marazul.com.br";

        if (!userRepository.existsByEmail(adminEmail)) {
            Collaborator admin = new Collaborator(
                    "Administrador",
                    new Date(),
                    Position.OTHER,
                    true,
                    profileRepository.findByName(ADMIN_PROFILE_NAME).orElseThrow(),
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

    private record PermissionSeed(String baseRoute, String feature) {
    }
}
