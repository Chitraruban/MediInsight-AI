package com.mediinsight.api.repository;

import com.mediinsight.api.model.ReportValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportValueRepository extends JpaRepository<ReportValue, Long> {
}
