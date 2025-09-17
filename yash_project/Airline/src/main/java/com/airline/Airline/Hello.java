package com.airline.Airline;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Hello {

    @RequestMapping("/test")
    public String greet(){
        return "hello world";
    }
}
