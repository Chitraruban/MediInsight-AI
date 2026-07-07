package com.mediinsight.api.repository;

import com.mediinsight.api.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByReportIdOrderByCreatedAtAsc(Long reportId);
    List<ChatMessage> findByReportIsNullOrderByCreatedAtAsc();
    void deleteByReportId(Long reportId);
}
