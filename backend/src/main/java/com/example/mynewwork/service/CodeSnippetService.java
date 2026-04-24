package com.example.mynewwork.service;

import com.example.mynewwork.model.entity.CodeSnippet;
import com.example.mynewwork.repository.CodeSnippetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeSnippetService {

    private final CodeSnippetRepository codeSnippetRepository;

    public Optional<CodeSnippet> findById(Long id) {
        log.debug("根据ID查询代码片段: {}", id);
        return codeSnippetRepository.findById(id);
    }

    public Page<CodeSnippet> findAll(Pageable pageable) {
        return findAll(null, pageable);
    }

    public Page<CodeSnippet> findAll(String language, Pageable pageable) {
        log.debug("分页查询代码片段, language={}", language);
        if (language != null && !language.isEmpty()) {
            return codeSnippetRepository.findByLanguage(language, pageable);
        }
        return codeSnippetRepository.findAll(pageable);
    }

    public List<CodeSnippet> findAllList() {
        log.debug("查询所有代码片段");
        return codeSnippetRepository.findAll();
    }

    public List<CodeSnippet> findByLanguage(String language) {
        log.debug("根据语言查询代码片段: {}", language);
        return codeSnippetRepository.findByLanguage(language);
    }

    public List<CodeSnippet> searchByKeyword(String keyword) {
        log.debug("搜索代码片段: {}", keyword);
        return codeSnippetRepository.searchByKeyword(keyword);
    }

    @Transactional
    public CodeSnippet save(CodeSnippet codeSnippet) {
        log.debug("保存代码片段: {}", codeSnippet.getTitle());
        return codeSnippetRepository.save(codeSnippet);
    }

    @Transactional
    public CodeSnippet update(Long id, CodeSnippet codeSnippet) {
        log.debug("更新代码片段: {}", id);
        CodeSnippet existing = codeSnippetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("代码片段不存在"));
        existing.setTitle(codeSnippet.getTitle());
        existing.setDescription(codeSnippet.getDescription());
        existing.setLanguage(codeSnippet.getLanguage());
        existing.setCode(codeSnippet.getCode());
        existing.setTags(codeSnippet.getTags());
        return codeSnippetRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("删除代码片段: {}", id);
        codeSnippetRepository.deleteById(id);
    }
}
