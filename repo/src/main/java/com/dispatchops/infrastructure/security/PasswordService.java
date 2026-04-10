package com.dispatchops.infrastructure.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordService {

    private final int bcryptRounds;

    public PasswordService(@Value("${security.bcrypt.rounds:12}") int bcryptRounds) {
        this.bcryptRounds = bcryptRounds;
    }

    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(bcryptRounds));
    }

    public boolean verify(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
