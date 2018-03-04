package com.plexiti.horizon.config

import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin
import org.camunda.bpm.engine.impl.persistence.StrongUuidGenerator
import org.camunda.bpm.spring.boot.starter.configuration.Ordering
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component @Order(Ordering.DEFAULT_ORDER + 1)
class CustomFlowConfiguration : ProcessEnginePlugin {

    override fun preInit(processEngineConfiguration: ProcessEngineConfigurationImpl) {
        processEngineConfiguration.isJobExecutorPreferTimerJobs = true
        processEngineConfiguration.jobExecutor.maxWait = 5000
        processEngineConfiguration.idGenerator = StrongUuidGenerator()
    }

    override fun postProcessEngineBuild(processEngine: ProcessEngine?) {
    }

    override fun postInit(processEngineConfiguration: ProcessEngineConfigurationImpl?) {
    }

}
