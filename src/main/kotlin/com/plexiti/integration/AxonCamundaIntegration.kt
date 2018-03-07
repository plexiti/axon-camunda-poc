package com.plexiti.integration;

import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.GenericEventMessage
import org.camunda.bpm.engine.delegate.BpmnError
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Component("command")
class CommandBehaviour: AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(CommandBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    override fun execute(execution: ActivityExecution) {
        val messageName = property("command", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(CommandIssued(execution.processInstanceId, execution.id, messageName))
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

@Component("event")
class EventBehaviour: JavaDelegate {

    private val logger = LoggerFactory.getLogger(CommandBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    override fun execute(execution: DelegateExecution) {
        val messageName = property("event", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(EventRaised(execution.processInstanceId, execution.id, messageName))
        logger.debug(eventMessage.payload.toString())
        eventBus.publish(eventMessage)
    }

}

@Component("query")
class QueryBehaviour: AbstractBpmnActivityBehavior() {

    private val logger = LoggerFactory.getLogger(QueryBehaviour::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    override fun execute(execution: ActivityExecution) {
        val messageName = property("query", execution.bpmnModelElementInstance)!!
        val eventMessage = GenericEventMessage(QueryRequested(execution.processInstanceId, execution.id, messageName))
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

data class CommandIssued(val processInstanceId: String, val executionId: String, val messageName: String)
data class EventRaised(val processInstanceId: String, val executionId: String, val messageName: String)
data class QueryRequested(val processInstanceId: String, val executionId: String, val messageName: String)

internal fun property(property: String, model: BpmnModelElementInstance): String? {
    return model.domElement.childElements.find { it.localName == "extensionElements" }
            ?.childElements?.find { it.localName == "properties" }
            ?.childElements?.find { it.localName == "property" && it.hasAttribute("name") && it.getAttribute("name") == property }
            ?.getAttribute("value")
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaCommandFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaEventFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaQueryFactory(val responseType: KClass<*>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaResponseHandler