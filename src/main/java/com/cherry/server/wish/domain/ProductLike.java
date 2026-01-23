package com.cherry.server.wish.domain;

import com.cherry.server.global.common.BaseTimeEntity;
import com.cherry.server.product.domain.Product;
import com.cherry.server.user.domain.User;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "product_likes",
        indexes = {
                @Index(name = "ux_product_like_user_product", columnList = "user_id, product_id", unique = true)
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private ProductLike(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    public static ProductLike create(User user, Product product) {
        return new ProductLike(user, product);
    }
}
