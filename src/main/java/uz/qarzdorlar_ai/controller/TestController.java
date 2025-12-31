package uz.qarzdorlar_ai.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/")
    public String index(){
        return "registry-telegram-bot";
    }

    @GetMapping("/api/open/tr")
    public String tr(){
        return "transaction-create";
    }

}
