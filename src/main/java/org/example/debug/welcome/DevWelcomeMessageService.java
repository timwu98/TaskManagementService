package org.example.debug.welcome;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("dev")
@Service
public class DevWelcomeMessageService implements WelcomeMessageService {
    @Override public String message() { return "👩‍💻 Running in DEV mode"; }
}