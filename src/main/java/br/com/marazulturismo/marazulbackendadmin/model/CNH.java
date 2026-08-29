package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.CNHType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Embeddable
@Data
public class CNH {

    protected CNH() {
    }

    public CNH(String number, CNHType type) {
        this.number = number;
        this.type = type;
    }

    @Column(name = "cnh_number")
    private String number;

    @Column(name = "cnh_type")
    @Enumerated(EnumType.STRING)
    private CNHType type;
}
