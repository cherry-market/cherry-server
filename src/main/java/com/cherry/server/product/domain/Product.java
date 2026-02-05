package com.cherry.server.product.domain;

import com.cherry.server.global.common.BaseTimeEntity;
import com.cherry.server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(ProductEntityListener.class)
@Getter
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_user_id")
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int price;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductTag> productTags = new ArrayList<>();

    @Builder
    public Product(User seller, String title, String description, int price, ProductStatus status, TradeType tradeType, Category category) {
        this.seller = seller;
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status;
        this.tradeType = tradeType;
        this.category = category;
    }

    public void activate() {
        if (this.status == ProductStatus.PENDING) {
            this.status = ProductStatus.SELLING;
        }
    }
}
