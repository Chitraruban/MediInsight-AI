package com.mediinsight.api.repository;

import com.mediinsight.api.model.DoctorQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorQuestionRepository extends JpaRepository<DoctorQuestion, Long> {
}
