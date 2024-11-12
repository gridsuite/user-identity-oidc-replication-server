/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentitiesResult;
import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentity;
import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentityError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@RestController
@RequestMapping(value = "/v1")
@Tag(name = "User Identity Oidc Replication Server", description = "User identity Oidc replication server")
public class UserIdentityController {
    private final UserIdentityService userIdentityService;

    public UserIdentityController(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    @GetMapping(value = "/users/identities/{sub}")
    @Operation(summary = "Get User identity from the OIDC sub")
    public ResponseEntity<UserIdentity> getIdentity(@PathVariable("sub") String sub) {
        return ResponseEntity.of(userIdentityService.getIdentity(sub));
    }

    private Optional<String> getUserSpecificErrorCode(Exception e) {
        Throwable cause = e.getCause();
        if (cause != null && e.getCause() instanceof JsonProcessingException) {
            return Optional.of(cause.getClass().getSimpleName());
        } else {
            return Optional.empty();
        }
    }

    @GetMapping(value = "/users/identities")
    @Operation(summary = "Get User identities from the OIDC subs")
    public UserIdentitiesResult getIdentities(@RequestParam("subs") List<String> subs) {
        Map<String, UserIdentity> data = new HashMap<>();
        Map<String, UserIdentityError> errors = new HashMap<>();

        for (String sub : subs) {
            try {
                Optional<UserIdentity> userIdentity = userIdentityService.getIdentity(sub);
                if (userIdentity.isPresent()) {
                    data.put(sub, userIdentity.get());
                } else {
                    // Error objects in the response for exceptions related to this nni only
                    // and continue with the other requested nnis.
                    // TODO for this impl this may be a nonexisting user, or a user that has not yet
                    // stored its idtoken
                    errors.put(sub, new UserIdentityError(sub, "INVALID_USER_ID"));
                }
            } catch (Exception e) {
                Optional<String> errorCode = getUserSpecificErrorCode(e);
                if (errorCode.isPresent()) {
                    // if we want we can standardize error codes instead of use the classname
                    errors.put(sub, new UserIdentityError(sub, errorCode.get()));
                } else {
                    // abort and use spring boot default exception handling for all other
                    // exceptions since they will probably occur again
                    // for the subsequent nnis.
                    throw e;
                }
            }
        }

        UserIdentitiesResult userIdentitiesResult = new UserIdentitiesResult();
        if (data.size() > 0) {
            userIdentitiesResult.setData(data);
        }
        if (errors.size() > 0) {
            userIdentitiesResult.setErrors(errors);
        }
        return userIdentitiesResult;
    }

    @PutMapping(value = "/users/identities/{sub}")
    @Operation(summary = "Store user identity from the oidc idtoken")
    public UserIdentity save(@PathVariable("sub") String sub, @RequestBody String idtoken) {
        return userIdentityService.save(sub, idtoken);
    }
}
