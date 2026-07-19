package com.DTMK.Online.Bookkeeping.Website.Project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /api/auth/register}.
 * <p>
 * Same validation rules as {@link LoginRequest}. Kept as a separate
 * DTO so future register-only fields (e.g. email, confirm-password,
 * terms-of-service checkbox) can be added without changing the
 * shape of the {@code /login} endpoint.
 */
@Data
public class RegisterRequest {

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
