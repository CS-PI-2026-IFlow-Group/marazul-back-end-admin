package br.com.marazulturismo.marazulbackendadmin.config;

import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.State;
import br.com.marazulturismo.marazulbackendadmin.repository.CityRepository;
import br.com.marazulturismo.marazulbackendadmin.repository.StateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Transactional
class LocationDataInitializerTest {

    private static final int FEDERATIVE_UNITS = 27;

    @Autowired
    private LocationDataInitializer locationDataInitializer;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CityRepository cityRepository;

    @Test
    void startup_importsEveryFederativeUnit() {
        List<State> states = stateRepository.findAllByOrderByNameAsc();

        assertThat(states).hasSize(FEDERATIVE_UNITS);
        assertThat(states).extracting(State::getAcronym).doesNotHaveDuplicates();
        assertThat(states).allSatisfy(state -> {
            assertThat(state.getName()).isNotBlank();
            assertThat(state.getAcronym()).hasSize(State.ACRONYM_LENGTH);
        });
        assertThat(states)
                .extracting(State::getName, State::getAcronym)
                .contains(
                        tuple("São Paulo", "SP"),
                        tuple("Minas Gerais", "MG"),
                        tuple("Distrito Federal", "DF"));
    }

    @Test
    void startup_importsCitiesAssociatedToTheirState() {
        assertThat(cityRepository.count()).isGreaterThan(5000);
        assertThat(cityRepository.findAll())
                .as("toda cidade precisa de estado, a associação é obrigatória")
                .allSatisfy(city -> assertThat(city.getState()).isNotNull());

        City campinas = cityRepository.findByNameContainingIgnoreCaseOrderByNameAsc("Campinas").stream()
                .filter(city -> city.getState().getAcronym().equals("SP"))
                .findFirst()
                .orElseThrow();

        assertThat(campinas.getName()).isEqualTo("Campinas");
        assertThat(campinas.getState().getName()).isEqualTo("São Paulo");
    }

    @Test
    void run_calledAgain_doesNotDuplicateStatesOrCities() {
        long states = stateRepository.count();
        long cities = cityRepository.count();

        locationDataInitializer.run();
        locationDataInitializer.run();

        assertThat(stateRepository.count()).isEqualTo(states);
        assertThat(cityRepository.count()).isEqualTo(cities);
    }

    @Test
    void findByState_returnsOnlyCitiesOfThatState() {
        State acre = stateRepository.findByAcronym("AC").orElseThrow();

        List<City> byId = cityRepository.findByStateIdOrderByNameAsc(acre.getId());
        List<City> byAcronym = cityRepository.findByStateAcronymIgnoreCaseOrderByNameAsc("ac");

        assertThat(byId).isNotEmpty();
        assertThat(byId).allSatisfy(city -> assertThat(city.getState().getAcronym()).isEqualTo("AC"));
        assertThat(byAcronym)
                .as("a busca por sigla ignora maiúsculas e minúsculas")
                .containsExactlyElementsOf(byId);
    }

    @Test
    void findByNameFragment_ignoresCase() {
        List<City> lowercase = cityRepository.findByNameContainingIgnoreCaseOrderByNameAsc("porto");
        List<City> uppercase = cityRepository.findByNameContainingIgnoreCaseOrderByNameAsc("PORTO");

        assertThat(lowercase).isNotEmpty();
        assertThat(lowercase).containsExactlyElementsOf(uppercase);
        assertThat(lowercase)
                .extracting(City::getName)
                .allSatisfy(name -> assertThat(name).containsIgnoringCase("porto"));
        assertThat(lowercase).extracting(City::getName).contains("Porto Alegre", "Porto Velho");
    }

    @Test
    void findByStateAndNameFragment_combinesBothFilters() {
        State riograndedosul = stateRepository.findByAcronym("RS").orElseThrow();

        List<City> cities = cityRepository
                .findByStateIdAndNameContainingIgnoreCaseOrderByNameAsc(riograndedosul.getId(), "PORTO");

        assertThat(cities).extracting(City::getName).contains("Porto Alegre");
        assertThat(cities).allSatisfy(city -> {
            assertThat(city.getState().getAcronym()).isEqualTo("RS");
            assertThat(city.getName()).containsIgnoringCase("porto");
        });
    }
}
