package main.controllers;

import main.ApplicationProps;
import main.Lemmatizer;
import main.SiteCrawling;
import main.model.Site;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Controller
//@EnableConfigurationProperties(value = ApplicationProps.class)

public class MainController {

    private JdbcTemplate jdbcTemplate;

    private boolean indexingStarted = false;
    private boolean isInterruptRequired = false;
    private boolean isIndexing = false;

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

    private void addSite(int siteId){
        java.sql.Date date = new java.sql.Date(Calendar.getInstance().getTime().getTime());
        Site site = new Site("INDEXING", date,applicationProps.getSites().get(siteId).getUrl(),applicationProps.getSites().get(siteId).getName());


        jdbcTemplate.update("INSERT IGNORE INTO search_engine.site(status, status_time, url,name) VALUES(?,?,?,?)",
                site.getStatus(),site.getStatusTime(),site.getUrl(),site.getName());
    }

    public String startIndexing2(int siteId){
        System.out.println("Запуск индексации");

        if (isInterruptRequired) {
            System.out.println("Индексация остановленна");
            return "index";
        } else {
            addSite(siteId);



            System.out.println("Start forkJoinPool");
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            System.out.println(applicationProps.getSites().get(siteId).getUrl());
            String url = applicationProps.getSites().get(siteId).getUrl();
            forkJoinPool.invoke(new SiteCrawling(url,siteId + 1, url, jdbcTemplate));
            System.out.println("End forkJoinPool");
        }

        Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);

        if (isInterruptRequired) {
            System.out.println("Индексация остановленна");
            return "index";
        } else {
            System.out.println("Start Lemmatizer");

            lemmatizer.lemText("lemmatizer");
            lemmatizer.outputMap(siteId + 1);
            System.out.println("End Lemmatizer");
        }
//
        if (isInterruptRequired) {
            System.out.println("Индексация остановленна");
            return "index";
        } else {
            System.out.println("Start Lemmatizer Rank ");
            lemmatizer.lemText("rank");
            System.out.println("End Lemmatizer Rank ");
        }
        jdbcTemplate.update("UPDATE search_engine.site SET status = 'INDEXED' where id = '" + (siteId + 1) +"'");
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
                int finalSiteId = siteId;
                Thread thread = new Thread(() -> {
                    new Thread(startIndexing2(finalSiteId)).start();
                });
                thread.start();
                isIndexing = true;
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

    @PostMapping("/api/indexPage")
    public String indexPage(@RequestParam(value = "url") String url){
        System.out.println("Начало");
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
        return "index";
    }

    @GetMapping("/api/statistics")
    public Object statistics(){

//        String filename = "example.json";
        JSONObject sampleObject = new JSONObject();
        sampleObject.put("result", "true");

        JSONObject total = new JSONObject();
        total.put("sites", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.site", Integer.class));
        total.put("pages", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.page", Integer.class));
        total.put("lemmas", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.lemma", Integer.class));
        total.put("isIndexing", isIndexing);

        JSONObject statistics = new JSONObject();

        JSONArray detailed = new JSONArray();


        for (Site site : findAllSite()){
            JSONObject detailed1 = new JSONObject();

            detailed1.put("url", site.getUrl());
            detailed1.put("name", site.getName());
            detailed1.put("status", site.getStatus());
            String statusTime = String.valueOf(site.getStatusTime());
            detailed1.put("statusTime",statusTime);
            detailed1.put("error", site.getLastError());
            detailed1.put("pages", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.page where site_id = '" + (site.getId()) +"'", Integer.class));
            detailed1.put("lemmas", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.lemma where site_id = '" + (site.getId()) +"'", Integer.class));

            detailed.add(detailed1);
        }

        statistics.put("total",total);
        statistics.put("detailed", detailed);

        sampleObject.put("statistics", statistics);

//        try {
//            Files.write(Paths.get(filename), sampleObject.toJSONString().getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return sampleObject;
    }

    public List<Site> findAllSite() {
        String sql = "SELECT * FROM search_engine.site";
        List<Site> sites = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        new Site(
                                rs.getInt("id"),
                                rs.getString("status"),
                                rs.getDate("status_time"),
                                rs.getString("last_error"),
                                rs.getString("url"),
                                rs.getString("name")
                        )
        );
        return sites;
    }
}
