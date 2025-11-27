package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String home(){
        log.info("GET /...");
        return "/main"; //resources/templates
    }
    @GetMapping("/main")
    public String main() {
        log.info("GET /main...");
        return "/main"; // src/main/resources/templates/main.html 파일을 찾아서 렌더링
    }
}