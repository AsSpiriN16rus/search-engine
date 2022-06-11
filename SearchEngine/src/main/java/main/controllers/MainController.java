package main.controllers;

import main.ApplicationProps;
import main.Lemmatizer;
import main.SiteCrawling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Controller
//@EnableConfigurationProperties(value = ApplicationProps.class)

public class MainController {

    private JdbcTemplate jdbcTemplate;

    private boolean indexingStarted = false;
    private boolean isInterruptRequired = false;

    @Autowired
    private ApplicationProps applicationProps;

    @Autowired
    public MainController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("${web-interface}")
    public String mainPAge(){
        return "index";
    }

    public void dropTable(){
        jdbcTemplate.update("TRUNCATE lemma");
        jdbcTemplate.update("TRUNCATE page");
        jdbcTemplate.update("TRUNCATE `index`");
    }

    public String startIndexing2(int siteId){
        System.out.println("Запуск индексации");

        if (isInterruptRequired) {
            System.out.println("Индексация остановленна");
            return "index";
        } else {
            System.out.println("Start forkJoinPool");
//                    String url = "http://www.playback.ru/";
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            System.out.println(applicationProps.getSites().get(siteId).getUrl());
            forkJoinPool.invoke(new SiteCrawling(0,siteId + 1, applicationProps.getSites().get(siteId).getUrl(), jdbcTemplate));
            System.out.println("End forkJoinPool");
        }

        Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);

        if (isInterruptRequired) {
            System.out.println("Индексация остановленна");
            return "index";
        } else {
            System.out.println("Start Lemmatizer");

            lemmatizer.lemText("lemmatizer");
            System.out.println("End Lemmatizer");
        }

        if (isInterruptRequired) {
            System.out.println("Индексация остановленна");
            return "index";
        } else {
            System.out.println("Start Lemmatizer Rank ");
            lemmatizer.lemText("rank");
            System.out.println("End Lemmatizer Rank ");
        }
        indexingStarted = false;
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public String startIndexing()
    {
//        dropTable();


//        System.out.println(applicationProps.getSites().get(0).getUrl());
//        System.out.println(applicationProps.getSites().get(1).getUrl());

        if (!indexingStarted) {
            indexingStarted = true;
            for (int siteId = 0; siteId < applicationProps.getSites().size(); siteId++) {
                System.out.println("Старт потока - " + siteId+1);
                int finalSiteId = siteId;
                Thread thread = new Thread(() -> {
                    new Thread(startIndexing2(finalSiteId)).start();
                });
                thread.start();
                System.out.println("Завершение потока - " + siteId+1);
    //            startIndexing2(siteId);

            }
        } else {
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

//    @PostMapping("/api/indexPage")
//    public String indexPage(@RequestParam(value = "url") String url){
//        System.out.println("Начало");
//        if (!indexingStarted){
//            indexingStarted = true;
//            System.out.println("Запуск индексации");
//
//
//            if (isInterruptRequired){
//                System.out.println("Индексация остановленна");
//                return "index";
//            }else {
//                System.out.println("Start forkJoinPool");
//                ForkJoinPool forkJoinPool = new ForkJoinPool();
////                forkJoinPool.invoke(new SiteCrawling(url, jdbcTemplate));
//                System.out.println("End forkJoinPool");
//            }
//
//            Lemmatizer lemmatizer = new Lemmatizer(jdbcTemplate);
//
//            if (isInterruptRequired){
//                System.out.println("Индексация остановленна");
//                return "index";
//            }else {
//                System.out.println("Start Lemmatizer");
//
//                lemmatizer.lemText("lemmatizer");
//                System.out.println("End Lemmatizer");
//            }
//
//            if (isInterruptRequired){
//                System.out.println("Индексация остановленна");
//                return "index";
//            }else {
//                System.out.println("Start Lemmatizer Rank ");
//                lemmatizer.lemText("rank");
//                System.out.println("End Lemmatizer Rank ");
//            }
//        }else {
//            System.out.println("Индексация уже идет");
//        }
//        return "index";
//    }

    @GetMapping("/api/statistics")
    public String statistics(){
//        System.out.println("pages - " + jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.page", Integer.class));
//        System.out.println("lemmas - " +jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.lemma", Integer.class));
        return "index";
    }
}
