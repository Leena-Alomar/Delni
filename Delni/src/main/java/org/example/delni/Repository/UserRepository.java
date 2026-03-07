package org.example.delni.Repository;

import org.example.delni.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User getUserById(Integer id);

    User getUserByUsername(String username);

    User getUserByEmail(String email);

    User findUserByUsername(String username);

}