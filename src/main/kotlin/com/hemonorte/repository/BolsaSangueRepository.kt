package com.hemonorte.repository

import com.hemonorte.model.BolsaSangue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BolsaSangueRepository : JpaRepository<BolsaSangue, UUID>