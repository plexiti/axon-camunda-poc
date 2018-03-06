package com.plexiti.horizon.domain

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
        if (command.owner == "kermit")
            throw IllegalArgumentException("Kermit's Credit Card is expired!")
        val eventMessage = GenericEventMessage.asEventMessage<CreditCardCharged>(CreditCardCharged(command.owner, command.amount))
        eventBus.publish(eventMessage)
    }

}

data class ChargeCreditCard(val owner: String, val amount: Float)
data class CreditCardCharged(val owner: String, val amount: Float)
