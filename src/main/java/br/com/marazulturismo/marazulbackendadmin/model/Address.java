package br.com.marazulturismo.marazulbackendadmin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Address {

    protected Address() {
    }

    public Address(String street, String number, String complement, String city, String state) {
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.city = city;
        this.state = state;
    }

    @Column(name = "address_street")
    private String street;

    @Column(name = "address_number")
    private String number;

    @Column(name = "address_complement")
    private String complement;

    @Column(name = "address_city")
    private String city;

    @Column(name = "address_state")
    private String state;
}
