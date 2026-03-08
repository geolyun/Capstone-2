package com.capstone.Capstone_2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/terms")
    public String termsPage() {
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacyPage() {
        return "legal/privacy";
    }

    @GetMapping("/legal/terms-content")
    public String termsContent() {
        // "templates/legal/terms.html" 반환
        return "legal/terms :: content";
    }

    @GetMapping("/legal/privacy-content")
    public String privacyContent() {
        // "templates/legal/privacy.html" 반환
        return "legal/privacy :: content";
    }
}