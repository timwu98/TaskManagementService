package org.example.controller;


import org.example.model.AppUser;
import org.example.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // Constructor Injection (recommended)
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /users → Create new user
    @PostMapping
    public AppUser createUser(@RequestBody AppUser user) {
        return userService.createUser(user);
    }

    // GET /users → Get all users
    @GetMapping
    public List<AppUser> getAllUsers() {
        return userService.getAllUsers();
    }
}
