package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.In.UserRequest;
import org.example.delni.DTO.Out.UserResponse;
import org.example.delni.Model.User;
import org.example.delni.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUserResponses() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public UserResponse addUserResponse(User user) {
        return toUserResponse(addUser(user));
    }

    @Transactional
    public UserResponse addUserResponse(UserRequest request) {
        return addUserResponse(toUser(request));
    }

    @Transactional
    public User updateUser(User user) {
        User existingUser = findUserById(user.getId());
        mergeIntoExistingUser(existingUser, user);
        return userRepository.save(existingUser);
    }

    @Transactional
    public UserResponse updateUserResponse(User user) {
        return toUserResponse(updateUser(user));
    }

    @Transactional
    public UserResponse updateUserResponse(Integer id, UserRequest request) {
        User existingUser = findUserById(id);
        applyRequestToUser(existingUser, request);
        return toUserResponse(userRepository.save(existingUser));
    }

    @Transactional
    public void deleteUser(Integer id) {
        findUserById(id);
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        User user = userRepository.findUserByEmail(email);
        if (user == null) {
            throw new ApiException("User not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findUserByUsername(String username) {
        User user = userRepository.findUserByUsername(username);
        if (user == null) {
            throw new ApiException("User not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findUserByPhoneNumber(String phoneNumber) {
        User user = userRepository.findUserByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new ApiException("User not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findUserByFirstNameAndLastName(String firstName, String lastName) {
        User user = userRepository.findUserByFirstNameAndLastName(firstName, lastName);
        if (user == null) {
            throw new ApiException("User not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findUserById(Integer id) {
        User user = userRepository.findUserById(id);
        if (user == null) {
            throw new ApiException("User not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(Integer id) {
        return toUserResponse(findUserById(id));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getInterestTags(),
                user.getAiPreferenceSummary(),
                user.getCreatedAt()
        );
    }

    private User toUser(UserRequest request) {
        User user = new User();
        applyRequestToUser(user, request);
        return user;
    }

    private void applyRequestToUser(User user, UserRequest request) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setInterestTags(request.getInterestTags());
        user.setAiPreferenceSummary(request.getAiPreferenceSummary());
    }

    private void mergeIntoExistingUser(User target, User source) {
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setUsername(source.getUsername());
        target.setEmail(source.getEmail());
        target.setPassword(source.getPassword());
        target.setPhoneNumber(source.getPhoneNumber());
        target.setInterestTags(source.getInterestTags());
        target.setAiPreferenceSummary(source.getAiPreferenceSummary());
    }
}
