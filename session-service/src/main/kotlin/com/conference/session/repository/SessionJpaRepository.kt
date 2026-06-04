package com.conference.session.repository

import com.conference.session.entity.SessionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SessionJpaRepository : JpaRepository<SessionEntity, Int>
