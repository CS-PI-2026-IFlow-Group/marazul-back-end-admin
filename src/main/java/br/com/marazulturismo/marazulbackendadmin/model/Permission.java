package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"base_route", "feature"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseRoute;

    @Column(nullable = false)
    private String feature;

    public Permission(String baseRoute, String feature) {
        this.baseRoute = baseRoute;
        this.feature = feature;
    }

    public String getAuthority() {
        return baseRoute + ":" + feature;
    }
}
