package com.example.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.Crawler;
import com.example.server.mapper.ClientMapper;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class ClientController {

    @Autowired
    private ClientMapper clientmapper;

    @GetMapping("/client")
    public List<Crawler> getMethodName() {
        return clientmapper.selectList(null);
    }
    
    
}
