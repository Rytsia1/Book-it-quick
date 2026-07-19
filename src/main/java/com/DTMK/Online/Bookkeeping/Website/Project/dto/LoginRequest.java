package com.DTMK.Online.Bookkeeping.Website.Project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/auth/login}.
 * <p>
 * Validation rules (all enforced at the controller boundary via
 * {@code @Valid @RequestBody}, so an invalid request never reaches
 * the service layer):
 * <ul>
 *   <li>{@code username}: required, 3-20 chars, only letters, digits,
 *       underscore, dot, and dash. This is the same character class
 *       the frontend already validates, so the rules are consistent.</li>
 *   <li>{@code password}: required, 6-100 chars, no leading or
 *       trailing whitespace.</li>
 * </ul>
 * The character-class constraints are the XSS / injection guard:
 * a payload containing {@code <script>}, NUL bytes, or path
 * separators is rejected before it ever touches the SQL layer
 * (which is already parameterized, but defense-in-depth matters).
 * <p>
 * Keeping the request body in a dedicated DTO (rather than
 * validating the {@code User} entity directly) is the standard
 * Spring approach — the entity represents persistence, the DTO
 * represents the wire format, and they can evolve independently.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_.-]+$",
        message = "Username can only contain letters, digits, underscore, dot, and dash"
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
    @Pattern(
        regexp = "^\\S(.*\\S)?$",
        message = "Password cannot start or end with whitespace"
    )
    private String password;
}
