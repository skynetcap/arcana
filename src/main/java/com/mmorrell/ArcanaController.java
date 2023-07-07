package com.mmorrell;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ArcanaController {

    @RequestMapping("/")
    public String arcanaIndex(Model model) {
        return "index";
    }

    @RequestMapping("/settings")
    public String arcanaSettings(Model model) {
        return "settings";
    }

}
