package br.com.marazulturismo.marazulbackendadmin.model;

import java.time.LocalDate;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class CNH {

    @Column(name = "cnh_number")
    private String number;

    @Column(name = "cnh_expiring_date")
    private LocalDate expiringDate;

    @Column(name = "cnh_type")
    @Enumerated(EnumType.STRING)
    private CNHType type;
}
