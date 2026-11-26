package com.sxmxdx.backend.repository;

import com.sxmxdx.backend.entity.ViewJournalAudit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViewJournalAuditRepository extends JpaRepository<ViewJournalAudit, Long> {

    List<ViewJournalAudit> findAllByOrderByDateActionDesc();
}
