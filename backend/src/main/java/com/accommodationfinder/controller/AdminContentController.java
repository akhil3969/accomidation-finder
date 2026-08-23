package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AdminDtos.AidParameterRow;
import com.accommodationfinder.dto.AdminDtos.ChecklistItemRow;
import com.accommodationfinder.dto.NewcomerDtos.GuideDetail;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.AidParameter;
import com.accommodationfinder.model.ChecklistItem;
import com.accommodationfinder.repository.AidParameterRepository;
import com.accommodationfinder.repository.ChecklistItemRepository;
import com.accommodationfinder.service.GuideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Editing the words and the numbers.
 *
 * <p>The guidance a newcomer reads, the documents on their checklist and the official
 * figures behind the aid estimate are all data, not code. When CAF publishes new ceilings
 * in October, or a rule changes, an admin fixes it here in a minute. That is the difference
 * between a project that ages well and one that quietly starts lying to people.
 */
@RestController
@RequestMapping("/api/admin/content")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin content", description = "Guides, checklist items and official aid figures")
public class AdminContentController {

    private final GuideService guideService;
    private final ChecklistItemRepository checklistItemRepository;
    private final AidParameterRepository aidParameterRepository;

    public AdminContentController(GuideService guideService,
                                  ChecklistItemRepository checklistItemRepository,
                                  AidParameterRepository aidParameterRepository) {
        this.guideService = guideService;
        this.checklistItemRepository = checklistItemRepository;
        this.aidParameterRepository = aidParameterRepository;
    }

    // ------------------------------------------------------------------ guides

    @GetMapping("/guides")
    @Operation(summary = "All guides, published or not")
    public List<GuideDetail> guides() {
        return guideService.all();
    }

    @PostMapping("/guides")
    @Operation(summary = "Create a guide")
    public GuideDetail create(@RequestBody GuideDetail payload) {
        return guideService.save(null, payload);
    }

    @PutMapping("/guides/{id}")
    @Operation(summary = "Update a guide")
    public GuideDetail update(@PathVariable Long id, @RequestBody GuideDetail payload) {
        return guideService.save(id, payload);
    }

    @PatchMapping("/guides/{id}/published")
    @Operation(summary = "Publish or unpublish a guide")
    public GuideDetail publish(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return guideService.setPublished(id, Boolean.TRUE.equals(body.get("published")));
    }

    @DeleteMapping("/guides/{id}")
    @Operation(summary = "Delete a guide")
    public ResponseEntity<Void> deleteGuide(@PathVariable Long id) {
        guideService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------------- checklist

    @GetMapping("/checklist")
    @Operation(summary = "Every checklist item, in display order")
    public List<ChecklistItemRow> checklist() {
        return checklistItemRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toRow)
                .toList();
    }

    @PostMapping("/checklist")
    @Operation(summary = "Add a checklist item")
    public ChecklistItemRow createItem(@RequestBody ChecklistItemRow payload) {
        return toRow(checklistItemRepository.save(apply(new ChecklistItem(), payload)));
    }

    @PutMapping("/checklist/{id}")
    @Operation(summary = "Edit a checklist item")
    public ChecklistItemRow updateItem(@PathVariable Long id, @RequestBody ChecklistItemRow payload) {
        ChecklistItem item = checklistItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item", id));
        return toRow(checklistItemRepository.save(apply(item, payload)));
    }

    @DeleteMapping("/checklist/{id}")
    @Operation(summary = "Delete a checklist item")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        checklistItemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------- aid figures

    @GetMapping("/aid-parameters")
    @Operation(summary = "The official figures behind every aid estimate")
    public List<AidParameterRow> aidParameters() {
        return aidParameterRepository.findAllByOrderByKeyAsc().stream()
                .map(parameter -> new AidParameterRow(
                        parameter.getId(),
                        parameter.getKey(),
                        parameter.getValue(),
                        parameter.getLabel(),
                        parameter.getSource(),
                        parameter.getEffectiveFrom() == null ? null : parameter.getEffectiveFrom().toString(),
                        parameter.getLastVerified() == null ? null
                                : parameter.getLastVerified().toLocalDate().toString()))
                .toList();
    }

    @PutMapping("/aid-parameters/{id}")
    @Operation(summary = "Correct a figure after CAF or Action Logement publishes a new one")
    public AidParameterRow updateParameter(@PathVariable Long id, @RequestBody AidParameterRow payload) {
        AidParameter parameter = aidParameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aid parameter", id));

        if (payload.value() != null) {
            parameter.setValue(payload.value());
        }
        if (payload.label() != null) {
            parameter.setLabel(payload.label());
        }
        if (payload.source() != null) {
            parameter.setSource(payload.source());
        }
        // Editing it is the act of verifying it.
        parameter.setLastVerified(LocalDateTime.now());

        AidParameter saved = aidParameterRepository.save(parameter);
        return new AidParameterRow(saved.getId(), saved.getKey(), saved.getValue(), saved.getLabel(),
                saved.getSource(),
                saved.getEffectiveFrom() == null ? null : saved.getEffectiveFrom().toString(),
                saved.getLastVerified().toLocalDate().toString());
    }

    // ----------------------------------------------------------------- mapping

    private ChecklistItemRow toRow(ChecklistItem item) {
        return new ChecklistItemRow(
                item.getId(), item.getKey(), item.getTitle(), item.getExplanation(),
                item.getIfYouCannot(), item.getOfficialLink(), item.getCategory(),
                item.getSortOrder(), item.isNonEuOnly(), item.isStudentsOnly(),
                item.isEssential(), item.isActive());
    }

    private ChecklistItem apply(ChecklistItem item, ChecklistItemRow payload) {
        if (payload.key() != null) {
            item.setKey(payload.key());
        }
        item.setTitle(payload.title());
        item.setExplanation(payload.explanation());
        item.setIfYouCannot(payload.ifYouCannot());
        item.setOfficialLink(payload.officialLink());
        item.setCategory(payload.category());
        item.setSortOrder(payload.sortOrder() == null ? 0 : payload.sortOrder());
        item.setNonEuOnly(payload.nonEuOnly());
        item.setStudentsOnly(payload.studentsOnly());
        item.setEssential(payload.essential());
        item.setActive(payload.active());
        return item;
    }
}
