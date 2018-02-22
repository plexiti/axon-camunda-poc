package com.plexiti.horizon.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestMapping
import java.util.concurrent.atomic.AtomicLong
import com.plexiti.generics.web.Resource


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Resource
data class Account(val id: String, val name: String)

@Controller
@RequestMapping("/accounts")
class AccountController {

    private val counter = AtomicLong()

    @RequestMapping(method = arrayOf(RequestMethod.GET)) @ResponseBody
    fun accounts(@RequestParam(value = "name", required = false, defaultValue = "testAccount") name: String): Account {
        return Account("${counter.incrementAndGet()}", String.format(template, name))
    }

    companion object {
        private val template = "%s"
    }

}