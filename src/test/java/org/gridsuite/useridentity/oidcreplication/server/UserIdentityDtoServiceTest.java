package org.gridsuite.useridentity.oidcreplication.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Stream;

import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = {
    "spring.autoconfigure.exclude=" +
          "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
          "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
class UserIdentityDtoServiceTest {

    @MockBean
    UserIdentityRepository excluded;

    @Autowired
    UserIdentityDtoService userIdentityDtoService;

    // Just to generate the strings for the test inputs
    // separate from the springboot mapper used inside the userIdentityDtoService
    private static ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("provideArgumentsForTest")
    void test(UserIdentityEntity userIdentityEntity, UserIdentity userIdentity) {
        assertThat(userIdentityDtoService.toDto(userIdentityEntity)).usingRecursiveComparison()
                .isEqualTo(userIdentity);
    }

    static Stream<Arguments> provideArgumentsForTest() throws Exception {
        return Stream.of(
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "name", "foo bar"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "given_name", "foo",
                                "family_name", "bar",
                                "name", "no"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "given_name", "foo",
                                "family_name", "bar",
                                "name", "oof rab"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "given_name", "foo",
                                "nickname", "nope",
                                "preferred_username", "no",
                                "name", "nooo",
                                "family_name", "bar",
                                "middle_name", "nono"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "nickname", "foo",
                                "preferred_username", "nope",
                                "name", "no",
                                "middle_name", "bar"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "preferred_username", "foo",
                                "name", "no",
                                "family_name", "bar"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "name", "foo",
                                "family_name", "bar"))),
                        new UserIdentity("sub1", "foo", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "name", "sub1",
                                "family_name", "bar"))),
                        new UserIdentity("sub1", "s", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1",
                                "family_name", "bar"))),
                        new UserIdentity("sub1", "s", "bar")),
                Arguments.of(new UserIdentityEntity("sub1",
                        mapper.writeValueAsString(Map.of(
                                "sub", "sub1"))),
                        new UserIdentity("sub1", "s", "sub1")),
                Arguments.of(new UserIdentityEntity("sub1", ""),
                        new UserIdentity("sub1", "s", "sub1"))
        );
    }

}
