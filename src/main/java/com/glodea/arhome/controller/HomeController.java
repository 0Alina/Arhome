package com.glodea.arhome.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping("/recipes")
    public String recipes() {
        return "recipes";
    }

    @GetMapping("/plans")
    public String plans() {
        return "plans";
    }

    @GetMapping({"/groceries", "/search"})
    public String groceries() {
        return "groceries";
    }

    @GetMapping("/cookbook")
    public String cookbook() {
        return "cookbook";
    }


    @GetMapping("/login")
    public String login() {
        return "login";
    }

}


