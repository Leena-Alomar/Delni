package org.example.delni.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "VARCHAR(50) NOT NULL")
    @NotEmpty(message = "First name cannot be empty")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "First name must contain only letters")
    @Size(max = 50, min = 2,  message = "First name can not be greater than 50 characters or less than 2 characters")
    private String firstName;

    @Column(columnDefinition = "VARCHAR(50) NOT NULL")
    @NotEmpty(message = "Last name cannot be empty")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Last name must contain only letters")
    @Size(max = 50, min = 2,  message = "Last name can not be greater than 50 characters or less than 2 characters")
    private String lastName;

    @Column(columnDefinition = "VARCHAR(30) NOT NULL UNIQUE")
    @NotEmpty(message = "Username cannot be empty")
    @Size(max = 30, min = 3, message = "Username can not be greater than 30 characters or less than 3 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @Column(columnDefinition = "VARCHAR(100) NOT NULL UNIQUE")
    @Email(message = "Email should be valid")
    @NotEmpty(message = "Email cannot be empty")
    private String email;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    @NotEmpty(message = "Password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;

    @Column(columnDefinition = "VARCHAR(15) NOT NULL UNIQUE")
    @NotEmpty(message = "Phone number cannot be empty")
    @Pattern(regexp = "^966\\d{8}$", message = "Phone number must start with 966 followed by 8 digits")
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String interestTags;

    @Column(columnDefinition = "TEXT")
    private String aiPreferenceSummary;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Relationships

    @OneToMany(mappedBy = "trip")
    private Set<Trip> trip;


}
