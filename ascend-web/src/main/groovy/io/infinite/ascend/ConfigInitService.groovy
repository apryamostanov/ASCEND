package io.infinite.ascend

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.infinite.ascend.granting.configuration.entities.*
import io.infinite.ascend.granting.configuration.repositories.*
import io.infinite.ascend.granting.server.entities.TrustedPublicKey
import io.infinite.ascend.granting.server.repositories.TrustedPublicKeyRepository
import io.infinite.blackbox.BlackBox
import io.infinite.carburetor.CarburetorLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Duration

@Service
@Slf4j
@BlackBox(level = CarburetorLevel.METHOD)
@CompileStatic
class ConfigInitService {

    @Autowired
    PrototypeGrantRepository grantRepository

    @Autowired
    PrototypeScopeRepository scopeRepository

    @Autowired
    PrototypeAuthenticationRepository authenticationTypeRepository

    @Autowired
    PrototypeIdentityRepository identityTypeRepository

    @Autowired
    PrototypeAuthorizationRepository authorizationTypeRepository

    @Autowired
    TrustedPublicKeyRepository trustedPublicKeyRepository

    @PostConstruct
    void initConfig() {
        if (!authorizationTypeRepository.findAll().empty) {
            return
        }
        trustedPublicKeyRepository.saveAndFlush(new TrustedPublicKey(
                name: "cashqbot-qa",
                publicKey: "30820222300d06092a864886f70d01010105000382020f003082020a02820201009b7e8a8cf855cabb9b3c9e645ae92edb226022b2f04b72aa3f9f6b505b9f2f7d13f8b3fa38e6937b1162288adf6b3056d83b5743afb413f5fa66e18e365a44f876793fbdbbbe0fb2e3bc6ed46b761c90769b4aa5d8e56b504b6005de3c69d30e8f1181972200a8d969a0947644be1d35901027d95796e7e9864a60a7484937770e52a41bd7a2f0c6b958431491cb7a07076870fecb88890ad7bcbe2d27bf8348874cd4b712984d1769376ada5da6c10dcbaed12d4a2a5a0489e37bd4d14a1342297a67e31028032f6118b43b66b9e80f90440830f88c49762562f770cfc8df71cb6ad2a73a880aa1d547391417515b246253bfdd7e84f830630be5825cd96ee702bfd8e39b46d60f9a0ddaa3d4eb820553e6f6ba5309b8ddec3b4ccc32154311af2a3d7a2507c92e39b8019b0eb887bd4931e7aa097276bc0259858435736801ca9f9b49743ed41c6d3c7e59ff5901b4efb97b2672692bf870e7a6771e3dee9a5c058eccab371e2a4481bddfc59d2d7c7cb8158d2994266fe7a7cffcfa7136bb266f67fee3a9fe65a39e9820cd773da218550cff7ff62114f17b219565c041d94011b7dfb13f4c0bd3804cecce514e5a7edafd0cd7dd8c0e3df9a79baa4137708e48e8529f4e5967416d3912e36eb4674422733ff80f2139342baec92065712eb3b99c31753a8f663cd5cf69b4d722f91eddd45e2d883aa5b90c0c08a7af8e6f0203010001"
        ))
        log.info("Initializing config")
        PrototypeGrant managedEmail = grantRepository.saveAndFlush(new PrototypeGrant(httpMethod: "POST", urlRegex: "https:\\/\\/orbit-secured\\.herokuapp\\.com\\/orbit\\/%ascendClientPublicKeyName%\\/managedEmail"))
        PrototypeGrant managedSms = grantRepository.saveAndFlush(new PrototypeGrant(httpMethod: "POST", urlRegex: "https:\\/\\/orbit-secured\\.herokuapp\\.com\\/orbit\\/%ascendClientPublicKeyName%\\/managedSms"))
        PrototypeGrant templates = grantRepository.saveAndFlush(new PrototypeGrant(httpMethod: "POST", urlRegex: "https:\\/\\/orbit-secured\\.herokuapp\\.com\\/orbit\\/%ascendClientPublicKeyName%\\/templates"))
        PrototypeGrant prototypeOtp = grantRepository.saveAndFlush(new PrototypeGrant(httpMethod: "POST", urlRegex: "https:\\/\\/orbit-secured\\.herokuapp\\.com\\/orbit\\/%ascendClientPublicKeyName%\\/prototypeOtp"))
        PrototypeGrant sendOtpSms = grantRepository.saveAndFlush(new PrototypeGrant(httpMethod: "POST", urlRegex: "https:\\/\\/orbit-secured\\.herokuapp\\.com\\/orbit\\/%ascendClientPublicKeyName%\\/sendOtpSms"))
        PrototypeScope managedNotifications = scopeRepository.saveAndFlush(
                new PrototypeScope(
                        name: "managedNotifications",
                        grants: [
                                managedEmail,
                                managedSms,
                                templates,
                                sendOtpSms,
                                prototypeOtp
                        ].toSet()
                )
        )
        PrototypeScope userServices = scopeRepository.saveAndFlush(
                new PrototypeScope(
                        name: "userServices"
                )
        )
        PrototypeAuthentication clientJwt = authenticationTypeRepository.saveAndFlush(
                new PrototypeAuthentication(
                        name: "clientJwt"
                )
        )
        PrototypeAuthentication smsOtp = authenticationTypeRepository.saveAndFlush(
                new PrototypeAuthentication(
                        name: "smsOtp"
                )
        )
        PrototypeAuthentication emailOtp = authenticationTypeRepository.saveAndFlush(
                new PrototypeAuthentication(
                        name: "emailOtp"
                )
        )
        PrototypeIdentity clientPrivateKeyOwner = identityTypeRepository.saveAndFlush(
                new PrototypeIdentity(
                        name: "clientPrivateKeyOwner",
                        authentications: [
                                clientJwt
                        ].toSet()
                )
        )
        PrototypeIdentity emailOwner = identityTypeRepository.saveAndFlush(
                new PrototypeIdentity(
                        name: "emailOwner",
                        authentications: [
                                emailOtp
                        ].toSet()
                )
        )
        PrototypeIdentity phoneNumberOwner = identityTypeRepository.saveAndFlush(
                new PrototypeIdentity(
                        name: "phoneNumberOwner",
                        authentications: [
                                smsOtp
                        ].toSet()
                )
        )
        identityTypeRepository.flush()
        PrototypeAuthorization readRefresh = authorizationTypeRepository.saveAndFlush(new PrototypeAuthorization(name: "readRefresh",
                identities: [
                        phoneNumberOwner
                ].toSet(),
                scopes: [
                        userServices
                ].toSet(),
                durationSeconds: Duration.ofDays(30).seconds.toInteger(),
                maxUsageCount: 3,
                serverNamespace: "OrbitSaaS"
        ))
        authorizationTypeRepository.saveAll([
                new PrototypeAuthorization(name: "app2app",
                        identities: [
                                clientPrivateKeyOwner
                        ].toSet(),
                        scopes: [
                                managedNotifications
                        ].toSet(),
                        durationSeconds: Duration.ofDays(30).seconds.toInteger(),
                        serverNamespace: "OrbitSaaS"
                ),
                new PrototypeAuthorization(name: "read",
                        identities: [
                                phoneNumberOwner
                        ].toSet(),
                        scopes: [
                                userServices
                        ].toSet(),
                        durationSeconds: 30,
                        maxUsageCount: Duration.ofHours(1).seconds.toInteger(),
                        serverNamespace: "OrbitSaaS",
                        refresh: readRefresh
                )
        ])
        authorizationTypeRepository.flush()
    }
}