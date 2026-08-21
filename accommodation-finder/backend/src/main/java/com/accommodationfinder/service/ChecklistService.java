package com.accommodationfinder.service;

import com.accommodationfinder.dto.NewcomerDtos.ChecklistEntry;
import com.accommodationfinder.dto.NewcomerDtos.ChecklistView;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.ChecklistItem;
import com.accommodationfinder.model.ChecklistProgress;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.ChecklistItemRepository;
import com.accommodationfinder.repository.ChecklistProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dossier tracker.
 *
 * <p>French landlords ask for a specific bundle of paperwork, and the list is unwritten -
 * everyone who grew up here simply knows it. Arriving without knowing it means losing flats
 * to better-prepared applicants for reasons nobody explains to you. This turns the unwritten
 * list into a visible, tickable one, filtered to the person's actual situation so an EU
 * student is not told to sort out a residence permit they do not need.
 */
@Service
public class ChecklistService {

    private final ChecklistItemRepository itemRepository;
    private final ChecklistProgressRepository progressRepository;

    public ChecklistService(ChecklistItemRepository itemRepository,
                            ChecklistProgressRepository progressRepository) {
        this.itemRepository = itemRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional(readOnly = true)
    public ChecklistView forUser(User user) {
        List<ChecklistItem> items = itemRepository.findByActiveTrueOrderBySortOrderAsc();

        Map<Long, ChecklistProgress> progressByItem = new HashMap<>();
        for (ChecklistProgress progress : progressRepository.findByUserId(user.getId())) {
            progressByItem.put(progress.getItem().getId(), progress);
        }

        boolean nonEu = !Boolean.TRUE.equals(user.getEuNational());
        List<ChecklistEntry> entries = new ArrayList<>();
        int done = 0;
        int essentialRemaining = 0;

        for (ChecklistItem item : items) {
            if (item.isNonEuOnly() && !nonEu) {
                continue;
            }
            if (item.isStudentsOnly() && !user.isStudent()) {
                continue;
            }

            ChecklistProgress progress = progressByItem.get(item.getId());
            boolean complete = progress != null && progress.isDone();
            if (complete) {
                done++;
            } else if (item.isEssential()) {
                essentialRemaining++;
            }

            entries.add(new ChecklistEntry(
                    item.getId(),
                    item.getKey(),
                    item.getTitle(),
                    item.getExplanation(),
                    item.getIfYouCannot(),
                    item.getOfficialLink(),
                    item.getCategory(),
                    item.isEssential(),
                    complete,
                    progress == null ? null : progress.getNote()));
        }

        return new ChecklistView(entries.size(), done, essentialRemaining, entries,
                encouragement(done, entries.size(), essentialRemaining));
    }

    @Transactional
    public ChecklistView toggle(User user, Long itemId, boolean done, String note) {
        ChecklistItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item", itemId));

        ChecklistProgress progress = progressRepository
                .findByUserIdAndItemId(user.getId(), itemId)
                .orElseGet(() -> new ChecklistProgress(user, item));

        progress.setDone(done);
        if (note != null) {
            progress.setNote(note.isBlank() ? null : note.trim());
        }
        progressRepository.save(progress);

        return forUser(user);
    }

    /** A line of encouragement that reflects where they actually are, not a generic cheer. */
    private String encouragement(int done, int total, int essentialRemaining) {
        if (total == 0) {
            return "Your checklist is being set up.";
        }
        if (done == 0) {
            return "Nothing ticked yet, and that is fine - most people start from zero. "
                    + "Begin with your passport and your enrolment letter; everything else "
                    + "gets easier once those two exist.";
        }
        if (essentialRemaining == 0) {
            return "You have every essential document. Your file is stronger than most "
                    + "applicants' - apply with confidence.";
        }
        if (done >= total / 2) {
            return "Over halfway. " + essentialRemaining + " essential item"
                    + (essentialRemaining == 1 ? "" : "s") + " left, and you can start "
                    + "applying for flats before you have all of them.";
        }
        return done + " of " + total + " done. Keep going - each document you gather makes "
                + "the next landlord more likely to say yes.";
    }
}
