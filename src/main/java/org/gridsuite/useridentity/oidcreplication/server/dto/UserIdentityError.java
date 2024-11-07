/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useridentity.oidcreplication.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Jon Schuhmacher <jon.harper at rte-france.com>
 */
@Data
@AllArgsConstructor
public class UserIdentityError {
    private String sub;
    private String code;
}
