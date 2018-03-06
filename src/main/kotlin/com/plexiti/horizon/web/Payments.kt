package com.plexiti.horizon.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestMapping
import com.plexiti.generics.web.Resource
import com.plexiti.horizon.domain.RetrievePayment
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Resource
data class Payment(val id: String, val amount: Float)

@Controller
@RequestMapping("/payments")
class PaymentController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @RequestMapping(method = arrayOf(RequestMethod.POST)) @ResponseBody
    fun payments(@RequestParam(value = "account", required = true) account: String, @RequestParam(value = "amount", required = true) amount: Float): ResponseEntity<RetrievePayment> {
        val command = RetrievePayment(account, amount)
        commandGateway.send<RetrievePayment>(command)
        return ResponseEntity(HttpStatus.ACCEPTED)
    }

}