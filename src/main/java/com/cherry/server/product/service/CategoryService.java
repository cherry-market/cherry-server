package com.cherry.server.product.service;

import com.cherry.server.product.dto.CategoryResponse;
import com.cherry.server.product.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}

