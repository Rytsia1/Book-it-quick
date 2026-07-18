package com.DTMK.Online.Bookkeeping.Website.Project.service;

import com.DTMK.Online.Bookkeeping.Website.Project.entity.User;
import com.DTMK.Online.Bookkeeping.Website.Project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // --- REGISTER FEATURE ---
    public String registerUser(String username, String password) {
        User existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            return "Failed: Username is already registered!";
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password); // Encryption can be added during the finishing stage.
        newUser.setAvatar("default-avatar.png");

        userMapper.insertUser(newUser);
        return "Success: Registration successful!";
    }

    // --- LOGIN FEATURE ---
    public String loginUser(String username, String password) {
        // 1. Find the user in the database.
        User user = userMapper.findByUsername(username);

        // 2. Validate that the user exists.
        if (user == null) {
            return "Failed: Username not found!";
        }

        // 3. Validate that the password matches.
        if (!user.getPassword().equals(password)) {
            return "Failed: Incorrect password!";
        }

        // 4. If all checks pass, a JWT token can be generated here later.
        return "Success: Login successful! Welcome, " + user.getUsername();
    }
}
