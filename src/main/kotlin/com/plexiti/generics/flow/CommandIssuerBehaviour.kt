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
@Component("message")
class FlowMessageBehaviour: AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(FlowMessageBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    override fun execute(execution: ActivityExecution) {
        val commandName = property("command", execution.bpmnModelElementInstance)
        val eventMessage = if (commandName != null) {
            GenericEventMessage(CommandIssued(execution.processInstanceId, execution.id, commandName))
        } else {
            val queryName = property("query", execution.bpmnModelElementInstance)!!
            GenericEventMessage(QueryRequested(execution.processInstanceId, execution.id, queryName))
        }
        logger.debug(eventMessage.payload.toString())
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

data class CommandIssued(val processInstanceId: String, val executionId: String, val commandName: String)
data class QueryRequested(val processInstanceId: String, val executionId: String, val queryName: String)
