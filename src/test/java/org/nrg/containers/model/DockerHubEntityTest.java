package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.config.DockerHubEntityTestConfig;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.containers.services.DockerHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = DockerHubEntityTestConfig.class)
public class DockerHubEntityTest {

    @Autowired private ObjectMapper mapper;
    @Autowired private DockerHubService dockerHubService;
    @Autowired private ConfigService mockConfigService;

    @Test
    public void testSpringConfiguration() {
        assertThat(dockerHubService, not(nullValue()));
    }

    @Test
    public void testCreateDockerHub() throws Exception {
        // Because we obscure the username and password in the json representation in docker hubs,
        // we have to write the json directly, not create an object an serialized json from it.
        final String hubToCreateJson = "{" +
                "\"id\": 0" +
                ", \"name\": \"a hub name\"" +
                ", \"url\": \"http://localhost\"" +
                ", \"default\": false" +
                "}";
        final DockerHub hubToCreate = mapper.readValue(hubToCreateJson, DockerHub.class);
        final DockerHub created = dockerHubService.create(hubToCreate);
        assertNotEquals(0L, created.id());
        assertEquals(hubToCreate.name(), created.name());
        assertEquals(hubToCreate.url(), created.url());
        assertEquals(hubToCreate.isDefault(), created.isDefault());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final DockerHubEntity createdEntity = dockerHubService.retrieve(created.id());
        assertEquals(created.id(), createdEntity.getId());
        assertEquals(created.name(), createdEntity.getName());
        assertEquals(created.url(), createdEntity.getUrl());
    }

    @Test
    @DirtiesContext
    public void testUpdateDockerHubEntity() throws Exception {
        final String hubToCreateJson = "{" +
                "\"id\": 0" +
                ", \"name\": \"a hub entity name\"" +
                ", \"url\": \"http://localhost\"" +
                ", \"default\": false" +
                "}";
        final DockerHub hubToCreate = mapper.readValue(hubToCreateJson, DockerHub.class);
        final DockerHubEntity hubEntityToCreate = DockerHubEntity.fromPojo(hubToCreate);
        final DockerHubEntity created = dockerHubService.create(hubEntityToCreate);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final String hubToUpdateJson = "{" +
                "\"id\": " + created.getId() +
                ", \"name\": \"some other hub entity name\"" +
                ", \"url\": \"http://localhost\"" +
                ", \"default\": false" +
                "}";
        final DockerHub hubToUpdate = mapper.readValue(hubToUpdateJson, DockerHub.class);
        final DockerHubEntity hubEntityToUpdate = DockerHubEntity.fromPojo(hubToUpdate);

        dockerHubService.update(hubEntityToUpdate);

        final DockerHubEntity retrieved = dockerHubService.retrieve(created.getId());
        assertEquals(hubEntityToUpdate, retrieved);
    }

    @Test
    @DirtiesContext
    public void testUpdateDockerHubPojo() throws Exception {
        final String hubToCreateJson = "{" +
                "\"id\": 0" +
                ", \"name\": \"a hub pojo name\"" +
                ", \"url\": \"http://localhost\"" +
                ", \"default\": false" +
                "}";
        final DockerHub hubToCreate = mapper.readValue(hubToCreateJson, DockerHub.class);
        final DockerHub created = dockerHubService.create(hubToCreate);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final String hubToUpdateJson = "{" +
                "\"id\": " + created.id() +
                ", \"name\": \"some other hub pojo name\"" +
                ", \"url\": \"http://localhost\"" +
                ", \"default\": false" +
                "}";
        final DockerHub hubToUpdate = mapper.readValue(hubToUpdateJson, DockerHub.class);

        dockerHubService.update(hubToUpdate);

        final DockerHub updated = dockerHubService.retrieveHub(created.id());
        assertEquals(hubToUpdate, updated);
    }
}
