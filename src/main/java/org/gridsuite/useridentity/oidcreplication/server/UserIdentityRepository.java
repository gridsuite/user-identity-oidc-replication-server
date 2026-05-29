package org.gridsuite.useridentity.oidcreplication.server;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, UUID> {

    Optional<UserIdentityEntity> findBySub(String sub);
}
