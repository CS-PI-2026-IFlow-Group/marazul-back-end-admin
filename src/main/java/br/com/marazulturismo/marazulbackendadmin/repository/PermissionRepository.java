package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByBaseRouteAndFeature(String baseRoute, String feature);

    List<Permission> findAllByOrderByBaseRouteAscFeatureAsc();
}
