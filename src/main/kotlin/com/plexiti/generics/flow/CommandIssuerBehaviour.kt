package com.plexiti.generics.flow

import org.axonframework.eventhandling.*
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component



/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component("command")
class FlowCommandQueuer : AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(FlowCommandQueuer::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    override fun execute(execution: ActivityExecution) {
        val commandName = property("command", execution.bpmnModelElementInstance)
        val event = CommandIssued(execution.processInstanceId, execution.id, commandName)
        val eventMessage = GenericEventMessage.asEventMessage<CommandIssued>(event)
        eventBus.publish(eventMessage)
    }

    override fun signal(execution: ActivityExecution, signalName: String?, signalData: Any?) {
        if (signalName == null) {
            leave(execution)
        } else {
            val bpmnError = if (signalData !is String) BpmnError(signalName) else BpmnError(signalName, signalData)
            propagateBpmnError(bpmnError, execution)
        }
    }

}

data class FlowCommand(val processInstanceId: String, val executionId: String, val commandName: String)
data class CommandIssued(val processInstanceId: String, val executionId: String, val commandName: String)
