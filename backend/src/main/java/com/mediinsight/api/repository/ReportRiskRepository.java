package com.mediinsight.api.repository;

import com.mediinsight.api.model.ReportRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRiskRepository extends JpaRepository<ReportRisk, Long> {
}
