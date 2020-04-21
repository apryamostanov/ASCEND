package io.infinite.ascend.validation.server.services

import groovy.util.logging.Slf4j
import io.infinite.ascend.common.entities.Claim
import io.infinite.ascend.common.entities.Authorization
import io.infinite.ascend.common.repositories.AuthorizationRepository
import io.infinite.ascend.common.services.JwtService
import io.infinite.ascend.validation.other.AscendForbiddenException
import io.infinite.ascend.validation.other.AscendUnauthorizedException
import io.infinite.blackbox.BlackBox
import io.infinite.carburetor.CarburetorLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@BlackBox(level = CarburetorLevel.METHOD)
@Slf4j
@Service
class ServerAuthorizationValidationService {

    @Autowired
    JwtService jwtService

    @Autowired
    AuthorizationRepository authorizationRepository

    @Value("jwtAccessKeyPublic")
    String jwtAccessKeyPublic

    void validateJwtClaim(String jwt, Claim claim) {
        Authorization authorization = jwtService.jwt2Authorization(jwt.replace("Bearer ", ""), jwtService.loadPublicKeyFromHexString(jwtAccessKeyPublic))
        validateAuthorizationClaim(authorization, claim)
    }

    void validateAuthorizationClaim(Authorization authorization, Claim claim) {
        if (authorization.expiryDate.before(new Date())) {
            throw new AscendForbiddenException("Expired Authorization")
        }
        for (grant in authorization.scope.grants) {
            if (grant.httpMethod.toLowerCase() == claim.method.toLowerCase()) {
                if (grant.urlRegex != null) {
                    String processedUrlRegex = replaceSubstitutes(grant.urlRegex, authorization)
                    log.debug("Processed URL regex", processedUrlRegex)
                    if (claim.incomingUrl.matches(processedUrlRegex)) {
                        log.debug("URL matched regex.")
                        Optional<Authorization> existingAuthorization = authorizationRepository.findByGuid(authorization.guid)
                        if (existingAuthorization.isPresent()) {
                            if (authorization.maxUsageCount > 0 && existingAuthorization.get().claims.size() >= authorization.maxUsageCount) {
                                throw new AscendUnauthorizedException("Exceeded maximum usage count")
                            }
                            existingAuthorization.get().claims.add(claim)
                            authorizationRepository.saveAndFlush(existingAuthorization.get())
                        } else {
                            authorization.claims.add(claim)
                            authorizationRepository.saveAndFlush(authorization)
                        }
                        log.debug("Authorized")
                        return
                    }
                }
            }
        }
        log.debug("No matching grant found")
        throw new AscendUnauthorizedException("Unauthorized")
    }

    String replaceSubstitutes(String iStringWithSubstitutes, Authorization iAuthorization) {
        String processedString = iStringWithSubstitutes
        iAuthorization.identity?.authenticatedCredentials?.each {
            processedString = processedString.replace("%" + it.key + "%", it.value)
        }
        return processedString
    }

}
