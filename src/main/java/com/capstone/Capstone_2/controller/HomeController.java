package com.capstone.Capstone_2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Principal principal) {
        if (principal != null) {
            return "home";
        }
        return "index";
    }
}