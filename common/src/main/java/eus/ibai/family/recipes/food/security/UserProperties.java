package eus.ibai.family.recipes.food.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties
public class UserProperties {

    private List<InMemoryUserDetails> users;

    record InMemoryUserDetails(String username, String password, List<String> roles) {}
}
