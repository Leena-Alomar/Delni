package org.example.delni.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiResponse;
import org.example.delni.DTO.In.UserRequest;
import org.example.delni.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.status(200).body(userService.getAllUserResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(userService.getUserResponseById(id));
    }

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody @Valid UserRequest request) {
        return ResponseEntity.status(200).body(userService.addUserResponse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody @Valid UserRequest request) {
        return ResponseEntity.status(200).body(userService.updateUserResponse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.status(200).body(new ApiResponse("User deleted"));
    }
}
