package com.vti.authen.oauth2;

import com.vti.authen.UserPrincipal;
import com.vti.entity.User;
import com.vti.entity.enums.UserRole;
import com.vti.entity.enums.UserStatus;
import com.vti.rabbitmqClient.service.RabbitMQSender;
import com.vti.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitMQSender rabbitMQSender;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        String name = oAuth2User.getAttribute("name");

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (name != null && !name.isBlank() && (user.getFullName() == null || user.getFullName().isBlank())) {
                user.setFullName(name);
                user = userRepository.save(user);
            }
        } else {
            user = new User();
            user.setEmail(email);
            String baseUsername = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
            String username = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + counter++;
            }
            user.setUsername(username);
            user.setFullName(name != null ? name : username);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.ACTIVE);

            user = userRepository.save(user);

            try {
                rabbitMQSender.sendUserCreatedEvent(user);
            } catch (Exception e) {
                log.error("Failed to send RabbitMQ user created event for userId={}: {}", user.getId(), e.getMessage(), e);
            }
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }
}
