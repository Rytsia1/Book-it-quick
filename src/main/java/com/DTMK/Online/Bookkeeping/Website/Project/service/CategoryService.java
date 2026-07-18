package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Category;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> getCategories(Integer userId) {
        return categoryMapper.findCategoriesByUserId(userId);
    }

    public Category createCategory(Category category) {
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insertCategory(category);
        return category;
    }

    public Category updateCategory(Category category) {
        LocalDateTime now = LocalDateTime.now();
        category.setUpdatedAt(now);
        categoryMapper.updateCategory(category);
        return categoryMapper.findByIdAndUserId(category.getId(), category.getUserId());
    }

    public void deleteCategory(Integer userId, Integer categoryId) {
        categoryMapper.deleteCategory(categoryId, userId);
    }

    public boolean categoryExists(Integer userId, String name, Integer type) {
        return categoryMapper.countByNameAndType(userId, name, type) > 0;
    }
}
