/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server;

import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@Service
public class UserIdentityDtoService {

    private ObjectMapper objectMapper;

    public UserIdentityDtoService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private String computeFirstName(String sub, String name, String givenName, String nickname, String preferredUsername) {
        // Try to do our best with heterogeneous data from differing identity providers
        // Feel free to improve this upon discovering new data formats..

        // return given nam if exists
        if (givenName != null) {
            return givenName;
        }
        // or return first Name
        if (name != null) {
            String[] names = name.split(" ");
            if (names.length >= 2) {
                return names[0];
            }
        }
        // or nickname
        if (nickname != null) {
            return nickname;
        }
        // or preferred user name
        if (preferredUsername != null) {
            return preferredUsername;
        }

        if (name != null && !name.equals(sub)) {
            // a name without any whitespace almost last resort
            // also not a copy of the sub because we use the sub in the lastname so avoid
            // "sub sub" to look nicer
            return name;
        }
        // Last resort, just to display something not too terrible, not sure if
        // it's a good idea. Looks like "S Sub".
        return sub.substring(0, 1);
    }

    private String computeLastName(String sub, String name, String familyName, String middleName) {
        // Try to do our best with heterogeneous data from differing identity providers
        // Feel free to improve this upon discovering new data formats..

        // return family name if exists
        if (familyName != null) {
            return familyName;
        }
        // or return last Name
        if (name != null) {
            String[] names = name.split(" ");
            if (names.length >= 2) {
                return names[names.length - 1];
            }
        }
        // or middle name
        if (middleName != null) {
            return middleName;
        }
        // Last resort use the sub, just to display something
        // not too terrible, not sure if it's a good idea we
        // don't use the "name" field here more precisly
        // because we will use it for the firstname as a last resort
        // so avoid duplicating
        return sub;
    }

    public UserIdentity toDto(UserIdentityEntity userIdentityEntity) {
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(userIdentityEntity.getIdtoken());
        } catch (JsonProcessingException e) {
            throw new UserIdentityException(e.getClass().getSimpleName() + ": Error parsing idtoken", e);
        }

        // Openid Connect idtoken spec:
        //  name | End-User's full name in displayable form including all name
        //         parts, possibly including titles and suffixes, ordered according
        //         to the End-User's locale and preferences.
        //  given_name | Given name(s) or first name(s) of the End-User. Note that
        //               in some cultures, people can have multiple given names; all
        //               can be present, with the names being separated by space
        //               characters.
        //  family_name | Surname(s) or last name(s) of the End-User. Note that in
        //                some cultures, people can have multiple family names or no
        //                family name; all can be present, with the names being
        //                separated by space characters.
        //  middle_name | Middle name(s) of the End-User. Note that in some cultures,
        //                 people can have multiple middle names; all can be present,
        //                 with the names being separated by space characters.
        //                 Also note that in some cultures, middle names are not used.
        //  nickname | Casual name of the End-User that may or may not be the
        //             same as the given_name. For instance, a nickname value of Mike
        //             might be returned alongside a given_name value of Michael.
        //  preferred_username | Shorthand name by which the End-User wishes
        //                       to be referred to at the RP, such as janedoe or j.doe. This value
        //                       MAY be any valid JSON string including special characters such as
        //                       @, /, or whitespace. The RP MUST NOT rely upon this value being
        //                       unique, as discussed in Section 5.7.

        // as of 2024-09-06, azure returned for my user
        //  "name": "myfirstName mylastName",
        //  "preferred_username": "myemail",
        String name = parsed.path("name").textValue();
        String givenName = parsed.path("given_name").textValue();
        String familyName = parsed.path("family_name").textValue();
        String middleName = parsed.path("middle_name").textValue();
        String nickname = parsed.path("nickname").textValue();
        String preferredUsername = parsed.path("preferred_username").textValue();
        String sub = parsed.path("sub").textValue();
        if (sub == null) {
            sub = userIdentityEntity.getSub();
        }

        return new UserIdentity(userIdentityEntity.getSub(),
                                computeFirstName(sub, name, givenName, nickname, preferredUsername),
                                computeLastName(sub, name, familyName, middleName)
        );
    }
}
