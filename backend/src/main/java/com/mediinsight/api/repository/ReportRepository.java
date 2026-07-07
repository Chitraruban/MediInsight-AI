package com.mediinsight.api.repository;

import com.mediinsight.api.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findAllByOrderByUploadedAtDesc();
}
