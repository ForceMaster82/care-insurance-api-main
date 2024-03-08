package kr.caredoc.careinsurance

import org.springframework.data.domain.AfterDomainEventPublication
import org.springframework.data.domain.DomainEvents
import kotlin.reflect.KClass

abstract class AggregateRoot(id: String) : BaseEntity(id) {
    @Transient
    private var internalDomainEvents: MutableList<Any> = mutableListOf()

    @get:DomainEvents
    val domainEvents: List<Any>
        get() = internalDomainEvents.toList()

    @AfterDomainEventPublication
    open fun clearEvents() = internalDomainEvents.clear()

    protected fun registerEvent(event: Any) = internalDomainEvents.add(event)

    protected fun updateEvent(eventClass: KClass<*>, event: Any?) {
        internalDomainEvents.removeIf { it::class == eventClass }
        event?.let { registerEvent(it) }
    }

    override fun postLoad() {
        this.internalDomainEvents = mutableListOf()
        super.postLoad()
    }
}
