package com.plexiti.generics.domain

import org.axonframework.commandhandling.model.AggregateIdentifier
import java.io.Serializable

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class Identifier<ID: Any>(): Serializable {

    open lateinit var id: ID protected set

    protected constructor(id: ID): this() {
        this.id = id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Identifier<*>
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.toString() // must exactly be actual id in order to work with Axon
    }

}

abstract class AggregateIdentifiedBy<ID: Any>(): Identifier<ID>() {

    @AggregateIdentifier
    override lateinit var id: ID

    protected constructor(id: ID): this() {
        this.id = id
    }

    override fun toString(): String {
        return "${this::class.qualifiedName}(id=${id})"
    }

}