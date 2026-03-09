package org.example.delni.Service;


import lombok.RequiredArgsConstructor;
import org.example.delni.Model.User;
import org.example.delni.Repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    // Connect with database
    private final UserRepository userRepository;

    /// CURD

    // get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // add a user
    public User addUser(User user) {
        return userRepository.save(user);
    }

    // update a user
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // delete a user
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    ///  extra endpoints

    // find user by email
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    // find user by username
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    // find user by phone number
    public User findUserByPhoneNumber(String phoneNumber) {
        return userRepository.findUserByPhoneNumber(phoneNumber);
    }

    // find user by first name and last name
    public User findUserByFirstNameAndLastName(String firstName, String lastName) {
        return userRepository.findUserByFirstNameAndLastName(firstName, lastName);
    }

    // find user by id
    public User findUserById(Integer id) {
        return userRepository.findUserById(id);
    }


}
