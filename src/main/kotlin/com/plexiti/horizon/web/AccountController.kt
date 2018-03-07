package com.plexiti.horizon.web

import org.springframework.stereotype.Controller
import com.plexiti.horizon.model.api.AccountId
import com.plexiti.horizon.model.api.CreateAccount
import com.plexiti.horizon.model.api.CreditToAccount
import com.plexiti.horizon.model.api.AccountSummary
import com.plexiti.horizon.model.api.DocumentAccountSummary
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.GenericQueryMessage
import org.axonframework.queryhandling.QueryBus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Controller
@RequestMapping("/accounts")
class AccountController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Autowired
    private lateinit var queryBus: QueryBus

    @RequestMapping(method = arrayOf(RequestMethod.POST)) @ResponseBody
    fun accounts(@RequestParam(value = "name", required = true) name: String): ResponseEntity<CreateAccount> {
        val command = CreateAccount(name)
        try {
            commandGateway.send<CreateAccount>(command)
            return ResponseEntity(HttpStatus.OK)
        } catch (e: RuntimeException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @RequestMapping("/{name}/credit", method = arrayOf(RequestMethod.PUT)) @ResponseBody
    fun credit(@PathVariable(value = "name", required = true) name: String, @RequestParam(value = "amount", required = true) amount: Float): ResponseEntity<CreditToAccount> {
        val query = DocumentAccountSummary(name)
        val accountSummary = queryBus.query(GenericQueryMessage(query, AccountSummary::class.java)).get()
        val command = CreditToAccount(AccountId(accountSummary.id), amount)
        try {
            commandGateway.send<CreditToAccount>(command)
            return ResponseEntity(HttpStatus.OK)
        } catch (e: RuntimeException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}