package com.marceloituccayasi.ocv.identityaccess.presentation;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Presents the authentication page and the minimal authenticated landing page.
 */
@Controller
public class AuthenticationPageController {

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/")
    String home(Authentication authentication, Model model) {
        model.addAttribute(
                "username",
                authentication.getName());

        return "home";
    }

}
