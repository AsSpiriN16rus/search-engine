package main.controllers;

import main.InitBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public MainController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("${web-interface}")
    public String mainPAge(){
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public String startIndexing(){
        return null;
    }

    @GetMapping("/api/stopIndexing")
    public String stopIndexing(){
        return null;
    }

    @PostMapping("/api/indexPage")
    public String indexPage(@RequestParam String url){
        return null;
    }
}
