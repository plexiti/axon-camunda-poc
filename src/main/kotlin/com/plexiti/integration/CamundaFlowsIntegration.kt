package com.plexiti.integration;

import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.GenericEventMessage
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component("command")
class CommandBehaviour: AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(CommandBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    @Autowired
    private lateinit var runtimeService: RuntimeService

    override fun execute(execution: ActivityExecution) {
        val messageName = property("command", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(CommandIssued(execution.processBusinessKey, execution.id, messageName))
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

    @EventHandler
    fun on(event: CommandSucceeded) {
        logger.debug(event.toString())
        runtimeService.signal(event.executionId, null, null, event.variables)
    }

    @EventHandler
    fun on(event: CommandFailed) {
        logger.debug(event.toString())
        runtimeService.signal(event.executionId, event.errorCode, event.errorMessage, null)
    }

}

@Component("event")
class EventBehaviour: JavaDelegate {

    private val logger = LoggerFactory.getLogger(CommandBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    @Autowired
    private lateinit var runtimeService: RuntimeService

    override fun execute(execution: DelegateExecution) {
        val messageName = property("event", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(EventRaised(execution.processBusinessKey, execution.id, messageName))
        logger.debug(eventMessage.payload.toString())
        eventBus.publish(eventMessage)
    }

    @EventHandler
    fun on(event: EventReceived) {
        logger.debug(event.toString())
        runtimeService.createMessageCorrelation(event.event).processInstanceBusinessKey(event.correlationKey).correlate()
    }

}

@Component("query")
class QueryBehaviour: AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(QueryBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    @Autowired
    private lateinit var runtimeService: RuntimeService

    override fun execute(execution: ActivityExecution) {
        val messageName = property("query", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(QueryRequested(execution.processBusinessKey, execution.id, messageName))
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

    @EventHandler
    fun on(event: QueryResponded) {
        logger.debug(event.toString())
        runtimeService.signal(event.executionId, null, null, event.variables)
    }

}

internal fun property(property: String, model: BpmnModelElementInstance): String? {
    return model.domElement.childElements.find { it.localName == "extensionElements" }
            ?.childElements?.find { it.localName == "properties" }
            ?.childElements?.find { it.localName == "property" && it.hasAttribute("name") && it.getAttribute("name") == property }
            ?.getAttribute("value")
}
