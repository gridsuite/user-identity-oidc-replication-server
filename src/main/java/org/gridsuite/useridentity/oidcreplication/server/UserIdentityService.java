/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server;

import java.util.Optional;

import org.gridsuite.useridentity.oidcreplication.server.dto.UserIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@Service
public class UserIdentityService {

    private UserIdentityRepository userIdentityRepository;
    private UserIdentityDtoService userIdentityDtoService;

    public UserIdentityService(
        UserIdentityRepository userIdentityRepository,
        UserIdentityDtoService userIdentityDtoService
    ) {
        this.userIdentityRepository = userIdentityRepository;
        this.userIdentityDtoService = userIdentityDtoService;
    }

    @Transactional(readOnly = true)
    public Optional<UserIdentity> getIdentity(String sub) {
        return userIdentityRepository.findBySub(sub).map(userIdentityDtoService::toDto);
    }

    @Transactional
    public UserIdentity save(String sub, String idtoken) {
        UserIdentityEntity userIdentityEntity = new UserIdentityEntity(sub, idtoken);
        // call toDto first to ensure that we have valid json, otherwise it throws and we don't save
        UserIdentity userIdentity = userIdentityDtoService.toDto(userIdentityEntity);
        userIdentityRepository.findBySub(sub)
                .ifPresent(savedEntity -> userIdentityEntity.setId(savedEntity.getId()));
        userIdentityRepository.save(userIdentityEntity); // merge or persist
        return userIdentity;
    }
}
