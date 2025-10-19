package com.capstone.Capstone_2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/terms")
    public String termsPage() {
        return "legal/terms"; // templates/legal/terms.html
    }

    @GetMapping("/privacy")
    public String privacyPage() {
        return "legal/privacy"; // templates/legal/privacy.html
    }
}