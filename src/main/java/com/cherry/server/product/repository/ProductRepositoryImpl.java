package com.cherry.server.product.repository;

import com.cherry.server.product.domain.Category;
import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.dto.ProductSearchCondition;
import com.cherry.server.product.dto.ProductSortBy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Slice<Product> findSliceByFilters(
            ProductSearchCondition condition,
            ProductSortBy sortBy,
            LocalDateTime cursorCreatedAt,
            Integer cursorPrice,
            Long cursorId,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> product = query.from(Product.class);

        product.fetch("seller", JoinType.LEFT);
        product.fetch("category", JoinType.LEFT);
        query.distinct(true);

        List<Predicate> predicates = new ArrayList<>();

        if (condition.status() != null) {
            predicates.add(cb.equal(product.get("status"), condition.status()));
        } else {
            predicates.add(cb.notEqual(product.get("status"), ProductStatus.PENDING));
        }

        if (condition.tradeType() != null) {
            predicates.add(tradeTypePredicate(cb, product, condition.tradeType()));
        }

        if (condition.categoryCode() != null) {
            Join<Product, Category> category = product.join("category", JoinType.LEFT);
            predicates.add(cb.equal(category.get("code"), condition.categoryCode()));
        }

        if (condition.minPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get("price"), condition.minPrice()));
        }

        if (condition.maxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get("price"), condition.maxPrice()));
        }

        if (cursorId != null && ((sortBy == ProductSortBy.LATEST && cursorCreatedAt != null) ||
                (sortBy != ProductSortBy.LATEST && cursorPrice != null))) {
            predicates.add(cursorPredicate(cb, product, sortBy, cursorCreatedAt, cursorPrice, cursorId));
        }

        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(sortOrders(cb, product, sortBy));

        TypedQuery<Product> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(0);
        typedQuery.setMaxResults(pageable.getPageSize() + 1);

        List<Product> result = typedQuery.getResultList();

        boolean hasNext = result.size() > pageable.getPageSize();
        List<Product> content = hasNext ? result.subList(0, pageable.getPageSize()) : result;

        return new SliceImpl<>(content, pageable, hasNext);
    }

    private Predicate tradeTypePredicate(CriteriaBuilder cb, Root<Product> product, TradeType tradeType) {
        return switch (tradeType) {
            case BOTH -> cb.equal(product.get("tradeType"), TradeType.BOTH);
            case DIRECT -> cb.or(
                    cb.equal(product.get("tradeType"), TradeType.DIRECT),
                    cb.equal(product.get("tradeType"), TradeType.BOTH)
            );
            case DELIVERY -> cb.or(
                    cb.equal(product.get("tradeType"), TradeType.DELIVERY),
                    cb.equal(product.get("tradeType"), TradeType.BOTH)
            );
        };
    }

    private Predicate cursorPredicate(
            CriteriaBuilder cb,
            Root<Product> product,
            ProductSortBy sortBy,
            LocalDateTime cursorCreatedAt,
            Integer cursorPrice,
            Long cursorId
    ) {
        return switch (sortBy) {
            case LATEST -> cb.or(
                    cb.lessThan(product.get("createdAt"), cursorCreatedAt),
                    cb.and(
                            cb.equal(product.get("createdAt"), cursorCreatedAt),
                            cb.lessThan(product.get("id"), cursorId)
                    )
            );
            case LOW_PRICE -> cb.or(
                    cb.greaterThan(product.get("price"), cursorPrice),
                    cb.and(
                            cb.equal(product.get("price"), cursorPrice),
                            cb.lessThan(product.get("id"), cursorId)
                    )
            );
            case HIGH_PRICE -> cb.or(
                    cb.lessThan(product.get("price"), cursorPrice),
                    cb.and(
                            cb.equal(product.get("price"), cursorPrice),
                            cb.lessThan(product.get("id"), cursorId)
                    )
            );
        };
    }

    private List<Order> sortOrders(CriteriaBuilder cb, Root<Product> product, ProductSortBy sortBy) {
        return switch (sortBy) {
            case LATEST -> List.of(cb.desc(product.get("createdAt")), cb.desc(product.get("id")));
            case LOW_PRICE -> List.of(cb.asc(product.get("price")), cb.desc(product.get("id")));
            case HIGH_PRICE -> List.of(cb.desc(product.get("price")), cb.desc(product.get("id")));
        };
    }
}
