package com.conference.attendee.store

import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.Attendee
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class AttendeeStore {
    private val nextId = AtomicInteger(4)
    private val store: MutableMap<Int, Attendee> = ConcurrentHashMap(
        mapOf(
            1 to Attendee(1, "Jim", "Gough", "gough@mail.com"),
            2 to Attendee(2, "Matt", "Auburn", "auburn@mail.com"),
            3 to Attendee(3, "Daniel", "Bryant", "bryant@mail.com")
        )
    )

    fun getAttendees(): List<Attendee> = store.values.toList()

    fun getAttendee(id: Int): Attendee =
        store[id] ?: throw ResourceNotFoundException("Attendee not found with id: $id")

    fun addAttendee(attendee: Attendee): Attendee {
        val id = nextId.getAndIncrement()
        val saved = attendee.copy(id = id)
        store[id] = saved
        return saved
    }

    fun updateAttendee(id: Int, attendee: Attendee): Attendee {
        if (!store.containsKey(id)) {
            throw ResourceNotFoundException("Attendee not found with id: $id")
        }
        val updated = attendee.copy(id = id)
        store[id] = updated
        return updated
    }

    fun removeAttendee(id: Int) {
        if (!store.containsKey(id)) {
            throw ResourceNotFoundException("Attendee not found with id: $id")
        }
        store.remove(id)
    }
}
