package com.plexiti.horizon.model

import com.plexiti.horizon.model.api.*
import com.plexiti.integration.*
import org.axonframework.eventhandling.saga.EndSaga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga
import org.slf4j.LoggerFactory

// TODO inheritance: replace abstract class with something better
// TODO api: consider to get rid of explicit correlation to flow

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Saga
class PaymentSaga: Flow() {

    protected val logger = LoggerFactory.getLogger(PaymentSaga::class.java)

    private lateinit var paymentId: PaymentId
    private lateinit var accountId: AccountId
    private var paymentAmount: Float = 0F
    private var creditAvailableOnAccount = 0F // just eventually consistent read model info, but useful to show a query example
    private var amountWithdrawnFromAccount = 0F // actually retrieved money deducted by event happening in write model
    private var amountChargedByCreditCard = 0F
    private var creditCardExpired = false

    @StartSaga
    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentRequested) {
        logger.debug(event.toString())
        paymentId = event.paymentId
        accountId = event.accountId
        paymentAmount = event.amount
        creditCardExpired = accountId.id == "kermit" // Kermit's card is always expired :-)
        SagaLifecycle.associateWith("accountId", accountId.id)
        correlateEventToFlow(event, paymentId.id)
    }

    @SagaEventHandler(associationProperty = "referenceId", keyName = "paymentId")
    fun on (event: AmountWithdrawn) {
        logger.debug(event.toString())
        amountWithdrawnFromAccount = event.amount
        correlateEventToFlow(event)
    }

    @SagaEventHandler(associationProperty = "referenceId", keyName = "paymentId")
    fun on (event: CreditCardCharged) {
        logger.debug(event.toString())
        amountChargedByCreditCard = event.amount
    }

    @SagaEventHandler(associationProperty = "accountId")
    fun on(event: CreditCardDetailsUpdated) {
        logger.debug(event.toString())
        creditCardExpired = false
        correlateEventToFlow(event)
    }

    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentPartlyCovered) {
        logger.debug(event.toString())
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentReceived) {
        logger.debug(event.toString())
        correlateEventToFlow(event)
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentCanceled) {
        logger.debug(event.toString())
        correlateEventToFlow(event)
    }

    // TODO alternatives: e.g. mirror all member properties

    override fun bindValuesToFlow(): Map<String, Any> {
        return mapOf (
            "creditAvailable" to (creditAvailableOnAccount > 0F),
            "creditFullyCovering" to (amountWithdrawnFromAccount == paymentAmount)
        )
    }

    // Construct Messages triggered by Flow Engine
    // TODO alternatives: e.g. message class constructors

    @FlowQueryFactory(responseType = AccountSummary::class)
    fun checkBalance(): DocumentAccountSummary {
        val query = DocumentAccountSummary(accountId)
        logger.debug(query.toString())
        return query
    }

    @FlowResponseHandler
    fun handle(accountSummary: AccountSummary) {
        logger.debug(accountSummary.toString())
        creditAvailableOnAccount = accountSummary.balance
    }

    @FlowCommandFactory
    fun chargeCreditCard(): ChargeCreditCard {
        val command = ChargeCreditCard(accountId, paymentId.id, paymentAmount - amountWithdrawnFromAccount, creditCardExpired)
        logger.debug(command.toString())
        return command
    }

    @FlowCommandFactory
    fun withdrawAmount(): WithdrawAmount {
        val command = WithdrawAmount(accountId, paymentId.id, paymentAmount)
        logger.debug(command.toString())
        return command
    }

    @FlowCommandFactory
    fun coverPayment(): CoverPayment {
        val command = CoverPayment(paymentId, if (amountChargedByCreditCard > 0) amountChargedByCreditCard else amountWithdrawnFromAccount)
        logger.debug(command.toString())
        return command
    }

    @FlowCommandFactory
    fun cancelPayment(): CancelPayment {
        val command = CancelPayment(paymentId)
        logger.debug(command.toString())
        return command
    }

    @FlowEventFactory
    fun updateCreditCardReminded(): UpdateCreditCardReminded {
        val event = UpdateCreditCardReminded(accountId)
        logger.debug(event.toString())
        return event
    }

    @FlowCommandFactory
    fun creditAmount(): CreditAmount {
        val command = CreditAmount(accountId, paymentId.id, amountWithdrawnFromAccount)
        logger.debug(command.toString())
        return command
    }

}