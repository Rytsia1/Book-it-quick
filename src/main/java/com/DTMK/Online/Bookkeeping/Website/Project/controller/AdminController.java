package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin-only endpoints. Mapped to {@code /api/admin/**} which is
 * gated by {@code .requestMatchers("/api/admin/**").hasRole("ADMIN")}
 * in {@code SecurityConfig#filterChain}.
 * <p>
 * Every handler method here is additionally annotated with
 * {@code @PreAuthorize("hasRole('ADMIN')")}. That's belt-and-
 * suspenders: the URL rule blocks non-admins before the request
 * reaches the controller, but if someone ever moves a handler out
 * of the {@code /api/admin/**} mapping by accident, the
 * {@code @PreAuthorize} annotation still blocks them at the
 * method level.
 * <p>
 * Real-world examples of admin-only operations in a bookkeeping
 * app would include global analytics, user management, category
 * moderation, audit-log inspection, and feature-flag toggles.
 * The methods here are intentionally minimal — they demonstrate
 * the RBAC plumbing end-to-end and are easy to extend.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;

    /**
     * Bootstrap-check endpoint. Returns the number of ADMIN users
     * in the system. Useful for the "is there an admin yet?"
     * health check (and to confirm a freshly-promoted user is
     * actually admin).
     */
    @org.springframework.web.bind.annotation.GetMapping("/users/admins/count")
    public ResponseEntity<Map<String, Object>> countAdmins() {
        Map<String, Object> body = new HashMap<>();
        body.put("count", userMapper.countAdmins());
        return ResponseEntity.ok(body);
    }

    /**
     * Promote or demote a user. The body is {@code {"role":"ADMIN"}}
     * or {@code {"role":"USER"}}. Only callable by an existing
     * ADMIN (the URL rule plus the {@code @PreAuthorize}
     * annotation on the class).
     */
    @PostMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String newRole = body == null ? null : body.get("role");
        if (newRole == null
                || (!newRole.equals("USER") && !newRole.equals("ADMIN"))) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "role must be 'USER' or 'ADMIN'");
            return ResponseEntity.badRequest().body(err);
        }
        userMapper.updateRole(id, newRole);

        Map<String, Object> body2 = new HashMap<>();
        body2.put("message", "User " + id + " role updated to " + newRole);
        body2.put("userId", id);
        body2.put("role", newRole);
        return ResponseEntity.ok(body2);
    }
}
