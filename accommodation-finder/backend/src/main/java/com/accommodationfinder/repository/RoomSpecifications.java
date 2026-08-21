package com.accommodationfinder.repository;

import com.accommodationfinder.dto.RoomDtos.RoomSearchCriteria;
import com.accommodationfinder.model.Room;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Builds the dynamic WHERE clause for GET /api/rooms from the supplied filters. */
public final class RoomSpecifications {

    private RoomSpecifications() {
    }

    public static Specification<Room> fromCriteria(RoomSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Listings hidden by a moderator stay in the database for the audit trail but
            // must never come back in a search.
            predicates.add(cb.isFalse(root.get("hidden")));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (hasText(criteria.query())) {
                String like = "%" + criteria.query().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("city")), like),
                        cb.like(cb.lower(root.get("address")), like)));
            }

            if (hasText(criteria.city())) {
                predicates.add(cb.like(cb.lower(root.get("city")),
                        "%" + criteria.city().trim().toLowerCase() + "%"));
            }

            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }

            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            if (criteria.roomType() != null) {
                predicates.add(cb.equal(root.get("roomType"), criteria.roomType()));
            }

            if (criteria.minSize() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sizeSqm"), criteria.minSize()));
            }

            if (criteria.bedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), criteria.bedrooms()));
            }

            if (Boolean.TRUE.equals(criteria.availableOnly())) {
                predicates.add(cb.isTrue(root.get("available")));
            }

            if (criteria.furnished() != null) {
                predicates.add(cb.equal(root.get("furnished"), criteria.furnished()));
            }

            if (criteria.billsIncluded() != null) {
                predicates.add(cb.equal(root.get("billsIncluded"), criteria.billsIncluded()));
            }

            if (criteria.availableFrom() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("availableFrom"), criteria.availableFrom()));
            }

            if (criteria.landlordId() != null) {
                predicates.add(cb.equal(root.get("landlord").get("id"), criteria.landlordId()));
            }

            // Avoid a cartesian product when the caller also asks for a count.
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("landlord", jakarta.persistence.criteria.JoinType.LEFT);
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
