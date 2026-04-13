package org.example.delni.DTO.In;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {

    @NotEmpty(message = "First name cannot be empty")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "First name must contain only letters")
    @Size(max = 50, min = 2, message = "First name can not be greater than 50 characters or less than 2 characters")
    private String firstName;

    @NotEmpty(message = "Last name cannot be empty")
    @Pattern(regexp = "^[\\p{L} ]+$", message = "Last name must contain only letters")
    @Size(max = 50, min = 2, message = "Last name can not be greater than 50 characters or less than 2 characters")
    private String lastName;

    @NotEmpty(message = "Username cannot be empty")
    @Size(max = 30, min = 3, message = "Username can not be greater than 30 characters or less than 3 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @Email(message = "Email should be valid")
    @NotEmpty(message = "Email cannot be empty")
    private String email;

    @NotEmpty(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;

    @NotEmpty(message = "Phone number cannot be empty")
    @Pattern(regexp = "^966\\d{9}$", message = "Phone number must start with 966 followed by 9 digits")
    private String phoneNumber;

    private String interestTags;
    private String aiPreferenceSummary;
}
