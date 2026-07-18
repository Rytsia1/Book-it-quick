package com.DTMK.Online.Bookkeeping.Website.Project.controller;

import com.DTMK.Online.Bookkeeping.Website.Project.config.JwtUtil;
import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth") // Matches the frontend route configuration.
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User request) {
        Map<String, Object> response = new HashMap<>();
        if (userMapper.findByUsername(request.getUsername()) != null) {
            response.put("error", "Username is already registered!");
            return ResponseEntity.badRequest().body(response);
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));
        request.setAvatar("default-avatar.png");
        userMapper.insertUser(request);

        response.put("message", "Registration successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody User request) {
        Map<String, Object> response = new HashMap<>();
        User user = userMapper.findByUsername(request.getUsername());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.put("error", "Incorrect username or password!");
            return ResponseEntity.status(401).body(response);
        }

        // Generate JWT Token
        String token = jwtUtil.generateToken(user.getUsername());

        response.put("message", "Login successful");
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        return ResponseEntity.ok(response);
    }
}
