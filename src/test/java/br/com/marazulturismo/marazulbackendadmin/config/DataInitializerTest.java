package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import br.com.marazulturismo.marazulbackendadmin.model.Profile;
import br.com.marazulturismo.marazulbackendadmin.model.Collaborator;
import br.com.marazulturismo.marazulbackendadmin.repository.PermissionRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.ProfileRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.CollaboratorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class DataInitializerTest {

    private static final String ADMIN_PROFILE_NAME = "Administrador";

    private static final Map<RequestMethod, String> FEATURE_BY_METHOD = Map.of(
            RequestMethod.POST, "create",
            RequestMethod.GET, "view",
            RequestMethod.PUT, "edit",
            RequestMethod.DELETE, "delete");

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CollaboratorRepository userRepository;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void startup_createsPermissionsAndAdminProfileWithAllOfThem() {
        List<Permission> permissions = permissionRepository.findAll();

        assertThat(permissions)
                .extracting(Permission::getBaseRoute, Permission::getFeature)
                .containsExactlyInAnyOrder(
                        tuple("dashboard", "view"),
                        tuple("funcionario", "create"),
                        tuple("funcionario", "view"),
                        tuple("funcionario", "edit"),
                        tuple("funcionario", "delete"),
                        tuple("frota", "create"),
                        tuple("frota", "view"),
                        tuple("frota", "edit"),
                        tuple("frota", "delete"),
                        tuple("perfis", "create"),
                        tuple("perfis", "view"),
                        tuple("perfis", "edit"),
                        tuple("perfis", "delete"),
                        tuple("permissoes", "view"));
        assertThat(adminProfile().getPermissions()).containsExactlyInAnyOrderElementsOf(permissions);
    }

    @Test
    void startup_createsAdminWithSystemAccessAndAdminProfile() {
        Collaborator admin = userRepository.findByEmail("admin@marazul.com.br").orElseThrow();

        assertThat(admin.getIsUser()).isTrue();
        assertThat(admin.getProfile().getName()).isEqualTo(ADMIN_PROFILE_NAME);
    }

    @Test
    void run_calledAgain_doesNotDuplicateAnything() {
        long permissions = permissionRepository.count();
        long profiles = profileRepository.count();
        long users = userRepository.count();
        int adminPermissions = adminProfile().getPermissions().size();

        dataInitializer.run();
        dataInitializer.run();

        assertThat(permissionRepository.count()).isEqualTo(permissions);
        assertThat(profileRepository.count()).isEqualTo(profiles);
        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(adminProfile().getPermissions()).hasSize(adminPermissions);
    }

    @Test
    void run_grantsAdminPermissionsCreatedAfterFirstStartup() {
        Permission newPermission = permissionRepository.save(new Permission("nova-rota", "create"));

        dataInitializer.run();

        assertThat(adminProfile().getPermissions()).contains(newPermission);
    }

    @Test
    void everyProtectedEndpoint_isCoveredByAPermission() {
        Set<String> registered = permissionRepository.findAll().stream()
                .map(p -> p.getBaseRoute() + " - " + p.getFeature())
                .collect(Collectors.toSet());

        Set<String> uncovered = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            for (String path : info.getPatternValues()) {
                if (!path.startsWith("/api/") || path.startsWith("/api/auth/")) {
                    continue;
                }
                String baseRoute = path.split("/")[2];
                for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                    String expected = baseRoute + " - " + FEATURE_BY_METHOD.get(method);
                    if (!registered.contains(expected)) {
                        uncovered.add(method + " " + path + " -> " + expected);
                    }
                }
            }
        }

        assertThat(uncovered)
                .as("Endpoints sem permissão cadastrada; adicione-as em DataInitializer.PERMISSIONS")
                .isEmpty();
    }

    private Profile adminProfile() {
        return profileRepository.findByName(ADMIN_PROFILE_NAME).orElseThrow();
    }
}
