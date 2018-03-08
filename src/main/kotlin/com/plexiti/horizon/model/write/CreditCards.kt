package com.plexiti.horizon.model.write

import com.plexiti.horizon.model.api.ChargeCreditCard
import com.plexiti.horizon.model.api.CreditCardCharged
import com.plexiti.horizon.model.api.CreditCardDetailsUpdated
import com.plexiti.horizon.model.api.UpdateCreditCardDetails
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.GenericEventMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Service
class CreditCardService() {

    private val logger = LoggerFactory.getLogger(Account::class.java)

    @Autowired
    private lateinit var eventBus: EventBus

    @CommandHandler
    fun handle(command: ChargeCreditCard) {
        logger.debug(command.toString())
        if (command.expired)
            throw IllegalArgumentException("Credit Card is expired!")
        val eventMessage = GenericEventMessage.asEventMessage<CreditCardCharged>(CreditCardCharged(command.accountId, command.amount))
        eventBus.publish(eventMessage)
    }

    @CommandHandler
    fun handle(command: UpdateCreditCardDetails) {
        logger.debug(command.toString())
        val eventMessage = GenericEventMessage.asEventMessage<CreditCardDetailsUpdated>(CreditCardDetailsUpdated(command.accountId))
        eventBus.publish(eventMessage)
    }

}

