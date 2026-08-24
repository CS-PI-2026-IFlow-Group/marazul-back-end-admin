package br.com.marazulturismo.marazulbackendadmin.model;

import br.com.marazulturismo.marazulbackendadmin.enums.BodyworkModel;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleStatus;
import br.com.marazulturismo.marazulbackendadmin.enums.VehicleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "vehicles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prefix;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BodyworkModel model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Column(name = "manufacturing_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer seats;

    private LocalDate inspectionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    public Vehicle(
            String prefix,
            String licensePlate,
            BodyworkModel model,
            VehicleType type,
            Integer year,
            Integer seats,
            LocalDate inspectionDate,
            VehicleStatus status) {

        this.prefix = prefix;
        this.licensePlate = licensePlate;
        this.model = model;
        this.type = type;
        this.year = year;
        this.seats = seats;
        this.inspectionDate = inspectionDate;
        this.status = status != null ? status : VehicleStatus.ACTIVE;
    }

    public void update(
            String prefix,
            String licensePlate,
            BodyworkModel model,
            VehicleType type,
            Integer year,
            Integer seats,
            LocalDate inspectionDate,
            VehicleStatus status) {

        this.prefix = prefix;
        this.licensePlate = licensePlate;
        this.model = model;
        this.type = type;
        this.year = year;
        this.seats = seats;
        this.inspectionDate = inspectionDate;
        if (status != null) {
            this.status = status;
        }
    }

    public void deactivate() {
        this.status = VehicleStatus.INACTIVE;
    }

    public boolean isInactive() {
        return this.status == VehicleStatus.INACTIVE;
    }
}
