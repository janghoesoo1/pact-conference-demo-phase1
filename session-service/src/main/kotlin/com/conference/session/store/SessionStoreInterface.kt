package com.conference.session.store

import com.conference.common.model.Session

interface SessionStoreInterface {
    fun getSessions(): List<Session>
    fun getSession(id: Int): Session?
    fun addSession(session: Session): Session
    fun updateSession(id: Int, session: Session): Session?
    fun removeSession(id: Int): Boolean
    fun clear()
}
