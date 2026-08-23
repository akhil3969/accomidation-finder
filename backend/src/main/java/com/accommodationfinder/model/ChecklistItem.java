package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * One step in the "what French landlords will ask you for" checklist.
 *
 * <p>These are content, not code: an admin edits them from the admin panel, which is why
 * the wording, the ordering and the audience filters all live in the database.
 */
@Entity
@Table(name = "checklist_items")
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable key so progress survives an admin rewording the title. */
    @Column(name = "item_key", nullable = false, unique = true, length = 60)
    private String key;

    @Column(nullable = false, length = 200)
    private String title;

    /** Plain-language explanation. Assume the reader has never dealt with France before. */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /** What to do if you cannot get this document. */
    @Column(name = "if_you_cannot", columnDefinition = "TEXT")
    private String ifYouCannot;

    @Column(name = "official_link", length = 500)
    private String officialLink;

    @Column(length = 60)
    private String category;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /** Only show this step to people who are not French/EU nationals. */
    @Column(name = "non_eu_only", nullable = false)
    private boolean nonEuOnly = false;

    /** Only show this step to students. */
    @Column(name = "students_only", nullable = false)
    private boolean studentsOnly = false;

    @Column(nullable = false)
    private boolean essential = true;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getIfYouCannot() { return ifYouCannot; }
    public void setIfYouCannot(String ifYouCannot) { this.ifYouCannot = ifYouCannot; }

    public String getOfficialLink() { return officialLink; }
    public void setOfficialLink(String officialLink) { this.officialLink = officialLink; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isNonEuOnly() { return nonEuOnly; }
    public void setNonEuOnly(boolean nonEuOnly) { this.nonEuOnly = nonEuOnly; }

    public boolean isStudentsOnly() { return studentsOnly; }
    public void setStudentsOnly(boolean studentsOnly) { this.studentsOnly = studentsOnly; }

    public boolean isEssential() { return essential; }
    public void setEssential(boolean essential) { this.essential = essential; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChecklistItem other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
