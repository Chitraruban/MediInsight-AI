package com.mediinsight.api.repository;

import com.mediinsight.api.model.LifestyleSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LifestyleSuggestionRepository extends JpaRepository<LifestyleSuggestion, Long> {
}
