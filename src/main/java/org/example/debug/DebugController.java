package org.example.debug;

import org.example.debug.welcome.WelcomeMessageService;
import org.springframework.core.env.Environment;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final Environment env;
    private final WelcomeMessageService msg;

    public DebugController(Environment env, WelcomeMessageService msg) {
        this.env = env; this.msg = msg;
    }

    @GetMapping("/context")
    public Map<String, Object> ctx() {
        return Map.of(
                "activeProfiles", env.getActiveProfiles(),
                "message", msg.message()
        );
    }

    @GetMapping("/whoami")
    public Map<String, Object> whoami(@AuthenticationPrincipal UserDetails user) {
        return Map.of(
                "authenticated", user != null,
                "username", user == null ? null : user.getUsername()
        );
    }

}
