package com.example.mynewwork.repository;

import com.example.mynewwork.model.entity.CodeSnippet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, Long> {

    List<CodeSnippet> findByLanguage(String language);

    @Query("SELECT c FROM CodeSnippet c WHERE c.title LIKE %:keyword% OR c.description LIKE %:keyword% OR c.tags LIKE %:keyword%")
    List<CodeSnippet> searchByKeyword(@Param("keyword") String keyword);

    Page<CodeSnippet> findByLanguage(String language, Pageable pageable);
}
