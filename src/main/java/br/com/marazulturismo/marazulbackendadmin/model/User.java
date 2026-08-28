package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.Position;
import br.com.marazulturismo.marazulbackendadmin.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Date admissionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(name = "senha_hash")
    private String passwordHash;

    private Date disabledAt;

    @Column(name = "cellphone_number")
    private String cellphoneNumber;

    @Embedded
    private CNH cnh;

    @Column(name = "reset_token", unique = true)
    private String resetToken;

    @Column(name = "reset_token_expiration")
    private Date resetTokenExpiration;

    public void setPasswordResetToken(String token, Date expiration) {
        this.resetToken = token;
        this.resetTokenExpiration = expiration;
    }

    public void clearPasswordResetToken() {
        this.resetToken = null;
        this.resetTokenExpiration = null;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void update(String name, String email, String cellphoneNumber, Position position, CNH cnh) {
        this.name = name;
        this.email = email;
        this.cellphoneNumber = cellphoneNumber;
        this.position = position;
        this.cnh = cnh;
    }

    public void deactivate() {
        this.disabledAt = new Date();
    }

    public void activate() {
        this.disabledAt = null;
    }

    public boolean isActive() {
        return disabledAt == null;
    }

    public User(
            String name,
            Date admissionDate,
            Position position,
            UserRole userRole,
            String email,
            String cellphoneNumber,
            CNH cnh,
            String passwordHash) {

        this.name = name;
        this.admissionDate = admissionDate;
        this.position = position;
        this.userRole = userRole;
        this.email = email;
        this.cellphoneNumber = cellphoneNumber;
        this.cnh = cnh;
        this.passwordHash = passwordHash;
    }
}
