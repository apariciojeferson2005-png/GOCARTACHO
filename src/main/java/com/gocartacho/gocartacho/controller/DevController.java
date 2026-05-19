package com.gocartacho.gocartacho.controller;

import com.gocartacho.gocartacho.util.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("dev")
public class DevController {

    private final DataSeeder dataSeeder;

    @PostMapping("/seed")
    public String seedData(
            @RequestParam(defaultValue = "10") int usuarios,
            @RequestParam(defaultValue = "15") int comercios) {
        return dataSeeder.seed(usuarios, comercios);
    }
}
