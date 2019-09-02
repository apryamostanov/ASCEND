package io.infinite.ascend

import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import io.infinite.ascend.config.entities.AscendInstance
import io.infinite.ascend.config.repositories.*
import io.infinite.blackbox.BlackBox
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.hateoas.config.EnableHypermediaSupport

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
@Slf4j
class GrantingApp implements CommandLineRunner {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    AscendInstanceRepository ascendInstanceRepository

    @Autowired
    AuthenticationTypeRepository authenticationTypeRepository

    @Autowired
    AuthorizationTypeRepository authorizationTypeRepository

    @Autowired
    GrantRepository grantRepository

    @Autowired
    IdentityTypeRepository identityTypeRepository

    @Autowired
    ScopeRepository scopeRepository

    @Value('${ascendConfigInitPluginDir}')
    String ascendConfigInitPluginDir

    @Value('${ascendStartupInit:false}')
    Boolean ascendStartupInit

    static void main(String[] args) {
        SpringApplication.run(GrantingApp.class, args)
    }

    @Override
    void run(String... args) throws Exception {
        runWithLogging()
    }

    @BlackBox
    void runWithLogging() {
        log.info("Starting Ascend...")
        AscendInstance ascendInstance = ascendInstanceRepository.getAscendInfo()
        if (ascendInstance == null && ascendStartupInit) {
            log.info("Loading configuration data")
            Binding binding = new Binding()
            binding.setVariable("applicationContext", applicationContext)
            binding.setVariable("ascendInstanceRepository", ascendInstanceRepository)
            binding.setVariable("authenticationTypeRepository", authenticationTypeRepository)
            binding.setVariable("authorizationTypeRepository", authorizationTypeRepository)
            binding.setVariable("grantRepository", grantRepository)
            binding.setVariable("identityTypeRepository", identityTypeRepository)
            binding.setVariable("scopeRepository", scopeRepository)
            getAuthenticationGroovyScriptEngine().run("ConfigInit.groovy", binding)
            ascendInstance = new AscendInstance()
            ascendInstance.isDataInitialized = true
            ascendInstanceRepository.saveAndFlush(ascendInstance)
            log.info("Finished loading configuration data")
            log.info("All set-up and running. Enjoy the Infinite Ascend!")
        }
    }

    @Memoized
    GroovyScriptEngine getAuthenticationGroovyScriptEngine() {
        return new GroovyScriptEngine(ascendConfigInitPluginDir, this.getClass().getClassLoader())
    }

}
