package com.cherry.server.product.domain;

import com.cherry.server.global.common.SpringContext;
import com.cherry.server.product.cache.ProductCacheInvalidator;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

public class ProductEntityListener {

    @PostPersist
    public void onPostPersist(Product product) {
        SpringContext.getBean(ProductCacheInvalidator.class).invalidateProductListCache();
    }

    @PostUpdate
    public void onPostUpdate(Product product) {
        SpringContext.getBean(ProductCacheInvalidator.class).invalidateProductListCache();
    }

    @PostRemove
    public void onPostRemove(Product product) {
        SpringContext.getBean(ProductCacheInvalidator.class).invalidateProductListCache();
    }
}
