package com.accommodationfinder.service;

import com.accommodationfinder.dto.NewcomerDtos.GuideDetail;
import com.accommodationfinder.dto.NewcomerDtos.GuideSummary;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.GuideArticle;
import com.accommodationfinder.repository.GuideArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Serves the plain-language explainers, and lets an admin edit them. */
@Service
public class GuideService {

    private final GuideArticleRepository repository;

    public GuideService(GuideArticleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GuideSummary> published() {
        return repository.findByPublishedTrueOrderBySortOrderAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuideDetail bySlug(String slug) {
        GuideArticle article = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Guide '" + slug + "' not found"));
        return toDetail(article);
    }

    // ------------------------------------------------------------- admin side

    @Transactional(readOnly = true)
    public List<GuideDetail> all() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(this::toDetail).toList();
    }

    @Transactional
    public GuideDetail save(Long id, GuideDetail payload) {
        GuideArticle article = id == null
                ? new GuideArticle()
                : repository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Guide", id));

        article.setSlug(payload.slug());
        article.setTitle(payload.title());
        article.setSummary(payload.summary());
        article.setBody(payload.body());
        article.setCategory(payload.category());
        article.setIcon(payload.icon());
        article.setOfficialLink(payload.officialLink());
        article.setLastVerified(LocalDateTime.now());
        if (article.getSortOrder() == null) {
            article.setSortOrder(0);
        }
        return toDetail(repository.save(article));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public GuideDetail setPublished(Long id, boolean published) {
        GuideArticle article = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guide", id));
        article.setPublished(published);
        return toDetail(repository.save(article));
    }

    private GuideSummary toSummary(GuideArticle article) {
        return new GuideSummary(article.getId(), article.getSlug(), article.getTitle(),
                article.getSummary(), article.getCategory(), article.getIcon(),
                article.getOfficialLink());
    }

    private GuideDetail toDetail(GuideArticle article) {
        return new GuideDetail(article.getId(), article.getSlug(), article.getTitle(),
                article.getSummary(), article.getBody(), article.getCategory(),
                article.getIcon(), article.getOfficialLink(),
                article.getLastVerified() == null ? null
                        : article.getLastVerified().toLocalDate().toString());
    }
}
