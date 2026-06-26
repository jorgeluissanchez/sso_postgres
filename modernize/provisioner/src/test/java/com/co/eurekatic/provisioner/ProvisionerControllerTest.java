package com.co.eurekatic.provisioner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc test for {@link ProvisionerController}.
 * Uses {@link MockMvcBuilders#standaloneSetup} instead of
 * {@code @WebMvcTest} to keep the test dependency surface
 * minimal — the controller is small and self-contained,
 * and standalone setup exercises the JSON binding +
 * validation + response shape without booting a Spring
 * context.
 *
 * <p>{@link DockerSocket} is replaced with a Mockito mock so
 * the test never tries to talk to a real Docker daemon —
 * those end-to-end checks live behind the compose smoke
 * test, not here.
 */
class ProvisionerControllerTest {

    private DockerSocket docker;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        docker = mock(DockerSocket.class);
        // The controller doesn't currently pull in
        // ProvisionerProperties — keeps the dependency
        // graph thin.
        mvc = MockMvcBuilders
                .standaloneSetup(new ProvisionerController(docker))
                .build();
    }

    @Test
    void postProvisionReturns201AndForwardsSpecToDocker() throws Exception {
        when(docker.createAndStart(any(), eq("query-service-oracle-dev")))
                .thenReturn("abc123");

        mvc.perform(post("/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instanceName": "oracle-dev",
                                  "dialect": "oracle",
                                  "jdbcUrl": "jdbc:oracle:thin:@db:1521/ORCLPDB1",
                                  "dbUsername": "query",
                                  "dbPassword": "change-me",
                                  "poolSize": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instanceName").value("query-service-oracle-dev"))
                .andExpect(jsonPath("$.containerId").value("abc123"))
                .andExpect(jsonPath("$.status").value("running"));

        verify(docker).createAndStart(any(), eq("query-service-oracle-dev"));
    }

    @Test
    void postProvisionRejectsBlankInstanceName() throws Exception {
        mvc.perform(post("/provision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "instanceName": "",
                                  "dialect": "postgres",
                                  "jdbcUrl": "jdbc:postgresql://db:5432/x",
                                  "dbUsername": "u",
                                  "poolSize": 5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProvisionReturns204() throws Exception {
        mvc.perform(delete("/provision/query-service-oracle-dev"))
                .andExpect(status().isNoContent());
        verify(docker).stopAndRemove("query-service-oracle-dev");
    }

    @Test
    void getStatusReturns200WhenContainerRunning() throws Exception {
        when(docker.status("query-service-postgres")).thenReturn("running");

        mvc.perform(get("/provision/query-service-postgres/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void getStatusReturns404WhenContainerMissing() throws Exception {
        when(docker.status("query-service-gone")).thenReturn(null);

        mvc.perform(get("/provision/query-service-gone/status"))
                .andExpect(status().isNotFound());
    }
}