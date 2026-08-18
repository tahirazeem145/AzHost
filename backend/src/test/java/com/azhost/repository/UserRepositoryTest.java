package com.azhost.repository;

import com.azhost.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindUserByEmail_ShouldSucceed() {
        User user = new User("developer@azhost.dev", "hashed_password_sample", "Lead Developer");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        Optional<User> foundUser = userRepository.findByEmail("developer@azhost.dev");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getDisplayName()).isEqualTo("Lead Developer");
    }

}
