package com.mmorrell;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @PostMapping("/settings/save")
    public String arcanaSettingsSave(Model model, @RequestParam String rpcServer) {
        System.out.println("server: " + rpcServer);
        return "settings";
    }

}
