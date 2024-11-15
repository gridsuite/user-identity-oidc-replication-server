/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentitiesResult {
    @JsonInclude(Include.NON_NULL)
    private Map<String, UserIdentity> data;
    @JsonInclude(Include.NON_NULL)
    private Map<String, UserIdentityError> errors;
}
