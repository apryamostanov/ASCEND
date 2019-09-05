package io.infinite.ascend.validation.repositories

import io.infinite.ascend.config.entities.Grant
import io.infinite.ascend.validation.entities.Usage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(exported = false)
interface UsageRepository extends JpaRepository<Usage, Long> {

    Set<Usage> findByAuthorizationId(@Param("authorizationId") UUID authorizationId)

}
