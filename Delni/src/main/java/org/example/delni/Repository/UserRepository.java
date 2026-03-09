package org.example.delni.Repository;

import org.example.delni.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserById(Integer id);

    User findUserByEmail(String email);

    User findUserByUsername(String username);

    User findUserByPhoneNumber(String phoneNumber);

    User findUserByFirstNameAndLastName(String firstName, String lastName);

}