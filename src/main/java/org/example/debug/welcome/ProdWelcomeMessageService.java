package org.example.debug.welcome;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("prod")
@Service
public class ProdWelcomeMessageService implements WelcomeMessageService {
    @Override public String message() { return "🚀 Running in PROD mode"; }
}