package br.com.marazulturismo.marazulbackendadmin.repository;

import br.com.marazulturismo.marazulbackendadmin.model.Address;
import br.com.marazulturismo.marazulbackendadmin.model.City;
import br.com.marazulturismo.marazulbackendadmin.model.Client;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CityRepository cityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private City city;

    @BeforeEach
    void setUp() {
        city = cityRepository.findAll().get(0);
    }

    @Test
    void save_withCpf_normalizesDocumentAndKeepsAddressCity() {
        Address address = persistAddress("100");

        Client client = clientRepository.saveAndFlush(
                new Client("Maria Silva", "123.456.789-00", null, address));

        assertThat(client.getCpf()).isEqualTo("12345678900");
        assertThat(client.getCnpj()).isNull();
        assertThat(client.getAddress().getCity().getId()).isEqualTo(city.getId());
    }

    @Test
    void save_withCnpj_normalizesDocument() {
        Client client = clientRepository.saveAndFlush(
                new Client("Empresa Azul", null, "12.345.678/0001-90", persistAddress("101")));

        assertThat(client.getCpf()).isNull();
        assertThat(client.getCnpj()).isEqualTo("12345678000190");
    }

    @Test
    void save_withoutDocument_isRejected() {
        assertThatThrownBy(() -> clientRepository.saveAndFlush(
                new Client("Sem Documento", null, null, persistAddress("102"))))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Informe somente CPF ou CNPJ para o cliente.");
    }

    @Test
    void save_withCpfAndCnpj_isRejected() {
        assertThatThrownBy(() -> clientRepository.saveAndFlush(
                new Client("Dois Documentos", "123.456.789-00", "12.345.678/0001-90", persistAddress("103"))))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Informe somente CPF ou CNPJ para o cliente.");
    }

    @Test
    void save_withDuplicatedDocumentsOrAddress_isRejected() {
        Address address = persistAddress("104");
        clientRepository.saveAndFlush(new Client("Cliente CPF", "12345678900", null, address));

        assertThatThrownBy(() -> clientRepository.saveAndFlush(
                new Client("Outro Cliente", "123.456.789-00", null, persistAddress("105"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_withAddressAlreadyLinkedToClient_isRejected() {
        Address address = persistAddress("106");
        clientRepository.saveAndFlush(new Client("Primeiro Cliente", "12345678901", null, address));

        assertThatThrownBy(() -> clientRepository.saveAndFlush(
                new Client("Segundo Cliente", null, "12345678000191", address)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void repository_queriesDocumentsAndNameIgnoringCase() {
        Client cpfClient = clientRepository.saveAndFlush(
                new Client("João da Silva", "12345678902", null, persistAddress("107")));
        Client cnpjClient = clientRepository.saveAndFlush(
                new Client("Empresa Silva", null, "12345678000192", persistAddress("108")));

        assertThat(clientRepository.existsByCpf("12345678902")).isTrue();
        assertThat(clientRepository.existsByCnpj("12345678000192")).isTrue();
        assertThat(clientRepository.existsByCpfAndIdNot("12345678902", cpfClient.getId())).isFalse();
        assertThat(clientRepository.existsByCpfAndIdNot("12345678902", cnpjClient.getId())).isTrue();
        assertThat(clientRepository.existsByCnpjAndIdNot("12345678000192", cnpjClient.getId())).isFalse();
        assertThat(clientRepository.existsByCnpjAndIdNot("12345678000192", cpfClient.getId())).isTrue();
        assertThat(clientRepository.findByNameContainingIgnoreCase("sIlVa"))
                .extracting(Client::getName)
                .containsExactlyInAnyOrder("João da Silva", "Empresa Silva");
    }

    private Address persistAddress(String number) {
        Address address = new Address("Rua das Flores", number, null, city);
        entityManager.persist(address);
        return address;
    }
}
