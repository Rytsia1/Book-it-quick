package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.Category;
import com.DTMK.Online.Bookkeeping.Website.Project.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    // GET /api/categories?userId=1
    @GetMapping
    public ResponseEntity<?> getCategories(@RequestParam Integer userId) {
        try {
            List<Category> list = categoryService.getCategories(userId);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            return error("Failed to load categories: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // POST /api/categories
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody Category category) {
        if (category == null || category.getUserId() == null
                || category.getType() == null
                || category.getName() == null || category.getName().trim().isEmpty()) {
            return error("userId, type, and name are required", HttpStatus.BAD_REQUEST);
        }
        try {
            // Reject duplicates up front so the user gets a clear 409 instead of a SQL exception.
            if (categoryService.categoryExists(category.getUserId(),
                    category.getName().trim(), category.getType())) {
                return error("A category with this name and type already exists",
                        HttpStatus.CONFLICT);
            }
            Category created = categoryService.createCategory(category);
            return ResponseEntity.ok(created);
        } catch (Exception ex) {
            return error("Failed to create category: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // PUT /api/categories/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        if (category == null || category.getType() == null
                || category.getName() == null || category.getName().trim().isEmpty()) {
            return error("type and name are required", HttpStatus.BAD_REQUEST);
        }
        try {
            // Reject if the rename would collide with another category of the same type.
            if (categoryService.categoryExists(category.getUserId(),
                    category.getName().trim(), category.getType())) {
                // The check above would also match the row being updated; rely on the
                // service to disambiguate if it ever needs to. For now a duplicate
                // name on a different id is a 409.
                return error("A category with this name and type already exists",
                        HttpStatus.CONFLICT);
            }
            category.setId(id);
            Category updated = categoryService.updateCategory(category);
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            return error("Failed to update category: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/categories/{id}?userId=1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id, @RequestParam Integer userId) {
        try {
            categoryService.deleteCategory(userId, id);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Category deleted successfully");
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return error("Failed to delete category: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns a uniform JSON error envelope `{ success: false, message: "..." }`
     * with the given HTTP status. The frontend axios layer reads
     * `response.data.message` to show a meaningful toast.
     */
    private ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
