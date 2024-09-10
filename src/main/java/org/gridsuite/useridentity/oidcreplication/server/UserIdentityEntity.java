/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

/**
 * @author Jon Schuhmacher <jon.schuhmacher at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_identities", indexes = {@Index(name = "user_identities_sub_index", columnList = "sub")})
public class UserIdentityEntity {

    public UserIdentityEntity(String sub, String idtoken) {
        this(null, sub, idtoken);
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "sub", nullable = false, unique = true)
    private String sub;

    @Column(name = "idtoken", length = 4096)
    private String idtoken;

}
