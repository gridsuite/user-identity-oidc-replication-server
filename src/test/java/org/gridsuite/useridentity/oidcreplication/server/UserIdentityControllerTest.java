/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentitiesResult;
import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentity;
import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentityError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // cheap way to get a new database every time
public class UserIdentityControllerTest {

    @DynamicPropertySource
    static void makeTestDbSuffix(DynamicPropertyRegistry registry) {
        UUID uuid = UUID.randomUUID();
        registry.add("testDbSuffix", () -> uuid);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    UserIdentityService userIdentityService;

    @Autowired
    UserIdentityRepository userIdentityRepository;

    @Autowired
    ObjectMapper mapper;

    Map<String, String> idtoken1 = Map.of("sub", "foo1", "given_name", "bar1", "family_name", "baz1");
    UserIdentity expected1 = new UserIdentity("foo1", "bar1", "baz1");
    Map<String, String> idtoken2 = Map.of("sub", "foo2", "given_name", "bar2", "family_name", "baz2");
    UserIdentity expected2 = new UserIdentity("foo2", "bar2", "baz2");
    UserIdentity expected1as2 = new UserIdentity("foo1", "bar2", "baz2");

    UserIdentitiesResult expectedMultipleResults = new UserIdentitiesResult(
            Map.of(expected1.getSub(), expected1,
                    expected2.getSub(), expected2),
            null);

    UserIdentityError errorNotExists = new UserIdentityError("notexists", "INVALID_USER_ID");
    UserIdentityError errorJson = new UserIdentityError("errjson", "JsonParseException");

    UserIdentitiesResult expectedPartialResults = new UserIdentitiesResult(
            Map.of(expected1.getSub(), expected1),
            Map.of(errorNotExists.getSub(), errorNotExists));

    UserIdentitiesResult expectedPartialJsonErrResults = new UserIdentitiesResult(
            Map.of(expected1.getSub(), expected1),
            Map.of(errorJson.getSub(), errorJson));

    UserIdentitiesResult expectedNoDataResults = new UserIdentitiesResult(
            null,
            Map.of(errorNotExists.getSub(), errorNotExists));

    @Before
    public void initDB() throws Exception {
        mockMvc.perform(put("/v1/users/identities/" + idtoken1.get("sub"))
                    .content(mapper.writeValueAsString(idtoken1)))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected1)));
        mockMvc.perform(put("/v1/users/identities/" + idtoken2.get("sub"))
                    .content(mapper.writeValueAsString(idtoken2)))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected2)));

        userIdentityRepository.save(new UserIdentityEntity("errjson", "{]"));
    }

    @Test
    public void shouldReturnSingleNames() throws Exception {
        mockMvc.perform(get("/v1/users/identities/foo1"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected1)));

        // rewrite with same data, nothing should be written (hibernate merge not dirty)
        // check the logs to confirm..
        mockMvc.perform(put("/v1/users/identities/" + idtoken1.get("sub"))
                .content(mapper.writeValueAsString(idtoken1)))
            .andExpect(status().isOk())
            .andExpect(content().json(mapper.writeValueAsString(expected1)));

        // try to overwrite with bad data, nothing should be written
        Exception thrown = assertThrows(Exception.class, () -> {
            mockMvc.perform(put("/v1/users/identities/errjson").content("{]"));
        });
        assertTrue("Should start with the error code", thrown.getCause().getMessage().startsWith("Json"));

        // read again, should still work with previous data
        mockMvc.perform(get("/v1/users/identities/foo1"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected1)));

        // overwrite with good data
        // Note: also using a different sub in the token as in the url,
        // is it a good idea to allow this ? the sub in the token is used as a last
        // resort to guess a name, that's why I chose to allow it.. Can be revisted
        // if it becomes a problem.
        mockMvc.perform(put("/v1/users/identities/" + idtoken1.get("sub"))
                    .content(mapper.writeValueAsString(idtoken2)))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected1as2)));

        mockMvc.perform(get("/v1/users/identities/foo1"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expected1as2)));
    }

    @Test
    public void shouldReturn404() throws Exception {
        mockMvc.perform(get("/v1/users/identities/notexists")).andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    public void shouldReturn500Checked() {
        Exception thrown = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/v1/users/identities/errjson"));
        });
        assertTrue("Should start with the error code",
                thrown.getCause().getMessage().startsWith("Json"));
    }

    @Test
    public void shouldReturnMultipleNames() throws Exception {
        mockMvc.perform(get("/v1/users/identities?subs=foo1,foo2")).andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expectedMultipleResults)));
    }

    @Test
    public void shouldReturnPartialNames() throws Exception {
        mockMvc.perform(get("/v1/users/identities?subs=foo1,notexists")).andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expectedPartialResults)));
    }

    @Test
    public void shouldReturn500MultipleChecked() throws Exception {
        mockMvc.perform(get("/v1/users/identities?subs=foo1,errjson")).andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(expectedPartialJsonErrResults)));
    }

    @Test
    public void shouldReturnNoData() throws Exception {
        mockMvc.perform(get("/v1/users/identities?subs=notexists"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                mapper.writeValueAsString(expectedNoDataResults)));
    }

}
