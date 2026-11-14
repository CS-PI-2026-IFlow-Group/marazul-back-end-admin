package br.com.marazulturismo.marazulbackendadmin.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripStatusTest {

    @Test
    void next_followsTheTripStagesInOrder() {
        assertThat(TripStatus.SCHEDULED.next()).isEqualTo(TripStatus.CONFIRMED);
        assertThat(TripStatus.CONFIRMED.next()).isEqualTo(TripStatus.LEFT_ORIGIN);
        assertThat(TripStatus.LEFT_ORIGIN.next()).isEqualTo(TripStatus.ARRIVED_DESTINATION);
        assertThat(TripStatus.ARRIVED_DESTINATION.next()).isEqualTo(TripStatus.LEFT_DESTINATION);
        assertThat(TripStatus.LEFT_DESTINATION.next()).isEqualTo(TripStatus.ARRIVED_ORIGIN);
        assertThat(TripStatus.ARRIVED_ORIGIN.next()).isEqualTo(TripStatus.COMPLETED);
        assertThat(TripStatus.COMPLETED.next()).isNull();
    }
}