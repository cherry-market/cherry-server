package com.cherry.server.product.domain;

import com.cherry.server.global.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "product_tags",
        indexes = {
                @Index(name = "ux_product_tags_product_tag", columnList = "product_id, tag_id", unique = true),
                @Index(name = "idx_product_tags_product_id", columnList = "product_id"),
                @Index(name = "idx_product_tags_tag_id", columnList = "tag_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    public ProductTag(Product product, Tag tag) {
        this.product = product;
        this.tag = tag;
    }
}
