package com.accommodationfinder.repository;

import com.accommodationfinder.model.GuideArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuideArticleRepository extends JpaRepository<GuideArticle, Long> {

    List<GuideArticle> findByPublishedTrueOrderBySortOrderAsc();

    List<GuideArticle> findAllByOrderBySortOrderAsc();

    Optional<GuideArticle> findBySlug(String slug);
}
