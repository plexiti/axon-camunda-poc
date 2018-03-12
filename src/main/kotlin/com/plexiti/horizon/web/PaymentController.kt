package com.plexiti.horizon.web

import com.plexiti.horizon.model.api.AccountId
import com.plexiti.horizon.model.api.OrderId
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestMapping
import com.plexiti.horizon.model.api.RequestPayment
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Controller
@RequestMapping("/payments")
class PaymentController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @RequestMapping(method = arrayOf(RequestMethod.POST)) @ResponseBody
    fun payments(@RequestParam(value = "account", required = true) account: String, @RequestParam(value = "amount", required = true) amount: Float): ResponseEntity<RequestPayment> {
        val command = RequestPayment(AccountId(account), OrderId(UUID.randomUUID().toString()), amount)
        commandGateway.send<RequestPayment>(command)
        return ResponseEntity(HttpStatus.ACCEPTED)
    }

}