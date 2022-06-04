package main.controllers;

import main.InitBean;
import main.Lemmatizer;
import main.SiteCrawling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ForkJoinPool;

@Controller
public class MainController {

    private JdbcTemplate jdbcTemplate;

    private boolean indexingStarted = false;
    private boolean isInterruptRequired = false;


    @Autowired
    public MainController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("${web-interface}")
    public String mainPAge(){
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public String startIndexing()
    {
        if (!indexingStarted){
            indexingStarted = true;
            System.out.println("Запуск индексации");


            if (isInterruptRequired){
                System.out.println("Индексация остановленна");
                return "index";
            }else {
                System.out.println("Start forkJoinPool");
                String url = "http://www.playback.ru/";
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                forkJoinPool.invoke(new SiteCrawling(url, jdbcTemplate));
                System.out.println("End forkJoinPool");
            }

            Lemmatizer lemmatizer = new Lemmatizer(jdbcTemplate);

            if (isInterruptRequired){
                System.out.println("Индексация остановленна");
                return "index";
            }else {
                System.out.println("Start Lemmatizer");

                lemmatizer.lemText("lemmatizer");
                System.out.println("End Lemmatizer");
            }

            if (isInterruptRequired){
                System.out.println("Индексация остановленна");
                return "index";
            }else {
                System.out.println("Start Lemmatizer Rank ");
                lemmatizer.lemText("rank");
                System.out.println("End Lemmatizer Rank ");
            }
        }else {
            System.out.println("Индексация уже идет");
        }
        return "index";
    }

    @GetMapping("/api/stopIndexing")
    public String stopIndexing(){
        if (!indexingStarted){
            System.out.println("Индексация не запущена");
        }else {
            System.out.println("Остановка индексации");
            isInterruptRequired = true;
        }
        return "index";
    }

    @PostMapping("/api/indexPage")
    public String indexPage(@RequestParam String url){
        System.out.println(url);
        if (!indexingStarted){
            indexingStarted = true;
            System.out.println("Запуск индексации");


            if (isInterruptRequired){
                System.out.println("Индексация остановленна");
                return "index";
            }else {
                System.out.println("Start forkJoinPool");
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                forkJoinPool.invoke(new SiteCrawling(url, jdbcTemplate));
                System.out.println("End forkJoinPool");
            }

            Lemmatizer lemmatizer = new Lemmatizer(jdbcTemplate);

            if (isInterruptRequired){
                System.out.println("Индексация остановленна");
                return "index";
            }else {
                System.out.println("Start Lemmatizer");

                lemmatizer.lemText("lemmatizer");
                System.out.println("End Lemmatizer");
            }

            if (isInterruptRequired){
                System.out.println("Индексация остановленна");
                return "index";
            }else {
                System.out.println("Start Lemmatizer Rank ");
                lemmatizer.lemText("rank");
                System.out.println("End Lemmatizer Rank ");
            }
        }else {
            System.out.println("Индексация уже идет");
        }
        return "index";
    }

    @GetMapping("/api/statistics")
    public String statistics(){
        System.out.println("pages - " + jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.page", Integer.class));
        System.out.println("lemmas - " +jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.lemma", Integer.class));
        return "index";
    }
}
