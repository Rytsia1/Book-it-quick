package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Category;
import com.DTMK.Online.Bookkeeping.Website.Project.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getCategories(@RequestParam Integer userId) {
        return ResponseEntity.ok(categoryService.getCategories(userId));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        category.setId(id);
        return ResponseEntity.ok(categoryService.updateCategory(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Integer id, @RequestParam Integer userId) {
        categoryService.deleteCategory(userId, id);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Category deleted successfully");
        return ResponseEntity.ok(resp);
    }
}
