package uy.um.faltauno.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
