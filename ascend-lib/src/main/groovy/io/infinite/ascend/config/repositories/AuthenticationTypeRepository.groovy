package io.infinite.ascend.config.repositories


import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource
interface AuthenticationTypeRepository extends JpaRepository<io.infinite.ascend.config.entities.AuthenticationType, Long> {


}
