package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Microservice;
import com.co.eurekatic.common.repository.MicroserviceRepository;
import com.co.eurekatic.ssoadmin.dto.MicroserviceRequest;
import com.co.eurekatic.ssoadmin.dto.MicroserviceResponse;
import com.co.eurekatic.ssoadmin.exception.DuplicateException;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicroserviceServiceTest {

    @Mock MicroserviceRepository repo;
    @InjectMocks MicroserviceService service;

    private static Microservice sample(long id) {
        Microservice m = new Microservice();
        m.setId(id);
        m.setServiceId("svc-" + id);
        m.setDescription("desc " + id);
        m.setRequestUri("/req");
        m.setTargetUriPath("/path");
        m.setTargetUrlHost("host");
        m.setTargetUrlPort("8080");
        return m;
    }

    @Test
    void createRejectsDuplicateServiceId() {
        when(repo.existsByServiceId("dup")).thenReturn(true);
        MicroserviceRequest req = new MicroserviceRequest(null, "dup", null, null, null, null, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("dup");
    }

    @Test
    void createPersistsNewMicroservice() {
        when(repo.existsByServiceId("new")).thenReturn(false);
        when(repo.save(any(Microservice.class))).thenAnswer(inv -> {
            Microservice m = inv.getArgument(0);
            m.setId(42L);
            return m;
        });

        MicroserviceResponse resp = service.create(new MicroserviceRequest(
                null, "new", "d", "/req", "/path", "host", "8080"));

        assertThat(resp.id()).isEqualTo(42L);
        assertThat(resp.serviceId()).isEqualTo("new");
        assertThat(resp.targetUrlPort()).isEqualTo("8080");
    }

    @Test
    void updateRejectsMissingId() {
        MicroserviceRequest req = new MicroserviceRequest(null, "x", null, null, null, null, null);
        assertThatThrownBy(() -> service.update(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateThrowsWhenMicroserviceMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        MicroserviceRequest req = new MicroserviceRequest(99L, "x", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(req))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateAppliesAllFields() {
        Microservice m = sample(7L);
        when(repo.findById(7L)).thenReturn(Optional.of(m));
        when(repo.save(m)).thenReturn(m);

        MicroserviceResponse resp = service.update(new MicroserviceRequest(
                7L, "renamed", "new desc", "/new-req", "/new-path", "newhost", "9090"));

        assertThat(resp.serviceId()).isEqualTo("renamed");
        assertThat(resp.description()).isEqualTo("new desc");
        assertThat(resp.requestUri()).isEqualTo("/new-req");
        assertThat(resp.targetUriPath()).isEqualTo("/new-path");
        assertThat(resp.targetUrlHost()).isEqualTo("newhost");
        assertThat(resp.targetUrlPort()).isEqualTo("9090");
    }

    @Test
    void getAllMapsAllMicroservices() {
        when(repo.findAll()).thenReturn(List.of(sample(1L), sample(2L)));
        List<MicroserviceResponse> all = service.getAll();
        assertThat(all).extracting(MicroserviceResponse::serviceId)
                .containsExactlyInAnyOrder("svc-1", "svc-2");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByServiceIdThrowsWhenMissing() {
        when(repo.findByServiceId("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByServiceId("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(repo.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
