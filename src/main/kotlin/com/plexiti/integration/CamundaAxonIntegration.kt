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

// TODO clean code: configuration exceptions, e.g. missing commands etc
// TODO infrastructure: non spring environments
// TODO resilience: idempotency / deduplication

@Component("command")
class CommandBehaviour: JavaDelegate {

    private val logger = LoggerFactory.getLogger(CommandBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    override fun execute(execution: DelegateExecution) {
        val messageName = property("command", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(FlowCommandIssued(execution.processBusinessKey, messageName))
        logger.debug(eventMessage.payload.toString())
        eventBus.publish(eventMessage)
    }

}

@Component("act")
class ActBehaviour: AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(ActBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    @Autowired
    private lateinit var runtimeService: RuntimeService

    override fun execute(execution: ActivityExecution) {
        val command = property("command", execution.bpmnModelElementInstance)!!
        val success = property("success", execution.bpmnModelElementInstance)
        val failure = property("failure", execution.bpmnModelElementInstance)
        val eventMessage = GenericEventMessage(FlowCommandIssued(execution.processBusinessKey, command, execution.id, success, failure))
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
    internal fun on(event: FlowCommandSucceeded) {
        logger.debug(event.toString())
        runtimeService.signal(event.flowAssociationId, null, null, event.variables)
    }

    @EventHandler
    internal fun on(event: FlowCommandFailed) {
        logger.debug(event.toString())
        runtimeService.signal(event.flowAssociationId, event.errorCode, event.errorMessage, null)
    }

}

@Component("event")
class EventBehaviour: JavaDelegate {

    private val logger = LoggerFactory.getLogger(ActBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    @Autowired
    private lateinit var runtimeService: RuntimeService

    override fun execute(execution: DelegateExecution) {
        val messageName = property("event", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(FlowEventRaised(execution.processBusinessKey, execution.id, messageName))
        logger.debug(eventMessage.payload.toString())
        eventBus.publish(eventMessage)
    }

    @EventHandler
    internal fun on(event: FlowEventReceived) {
        logger.debug(event.toString())
        runtimeService
            .createMessageCorrelation(event.event)
            .processInstanceBusinessKey(event.sagaAssociationId)
            .setVariables(event.variables)
            .correlate()
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
        val eventMessage = GenericEventMessage(FlowQueryRequested(execution.processBusinessKey, execution.id, messageName))
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
    internal fun on(event: FlowQueryResponded) {
        logger.debug(event.toString())
        runtimeService.signal(event.flowAssociationId, null, null, event.variables)
    }

}

internal fun property(property: String, model: BpmnModelElementInstance): String? {
    val property = model.domElement.childElements.find { it.localName == "extensionElements" }
            ?.childElements?.find { it.localName == "properties" }
            ?.childElements?.find { it.localName == "property" && it.hasAttribute("name") && it.getAttribute("name") == property }
            ?.getAttribute("value")
            ?.trim()
    return if (property == null || property.isEmpty()) null else property
}
