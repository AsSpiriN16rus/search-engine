package main.controllers;

import main.ApplicationProps;
import main.Lemmatizer;
import main.SearchEngine;
import main.SiteCrawling;
import main.model.PageSearchRelevance;
import main.model.Site;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        jdbcTemplate.update("TRUNCATE site");
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
        dropTable();
        JSONObject jsonObject = new JSONObject();
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
            jsonObject.put("result", true);
        } else {
            jsonObject.put("result", false);
            jsonObject.put("error", "Индексация уже идет");
            System.out.println("Индексация уже идет");
        }

        try {
            Files.write(Paths.get("src/main/resources/search_engine_frontend/startIndexing.json"), jsonObject.toJSONString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "statistics.json";
    }

    @GetMapping("/api/stopIndexing")
    public String stopIndexing(){
        JSONObject jsonObject = new JSONObject();

        if (!indexingStarted){
            jsonObject.put("result", false);
            jsonObject.put("error", "Индексация не запущена");
            System.out.println("Индексация не запущена");
        }else {
            isInterruptRequired = true;
            jsonObject.put("result", true);
            System.out.println("Остановка индексации");

        }
        try {
            Files.write(Paths.get("src/main/resources/search_engine_frontend/stopIndexing.json"), jsonObject.toJSONString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "stopIndexing.json";
    }

    @PostMapping("/api/indexPage")
    public String indexPage(@RequestParam(value = "url") String url) throws IOException {
        System.out.println("Начало");
        JSONObject jsonObject = new JSONObject();
        try {
            org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
        }catch (Exception ex){
            ex.getMessage();
            jsonObject.put("result", false);
            jsonObject.put("error", "Страница не найдена");
            Files.write(Paths.get("src/main/resources/search_engine_frontend/indexPage.json"), jsonObject.toJSONString().getBytes());
            return "indexPage.json";
        }
        if (url.length() > 0) {
            for (int siteId = 0; siteId < applicationProps.getSites().size(); siteId++) {
                String str = applicationProps.getSites().get(siteId).getUrl();
                if (url.contains(str) && url.length() > 0) {
                    Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);
                    lemmatizer.lemTextOnePage(url, "lemmatizer", str);
                    lemmatizer.lemTextOnePage(url, "rank", str);
                    System.out.println("true");
                    jsonObject.put("result", true);
                    break;
                } else {
                    jsonObject.put("result", false);
                    jsonObject.put("error", "Данная страница находится за пределами сайтов," +
                            "указанных в конфигурационном файле");
                }

            }
        }else {
            jsonObject.put("result", false);
            jsonObject.put("error", "Заполните поле");
        }
        Files.write(Paths.get("src/main/resources/search_engine_frontend/indexPage.json"), jsonObject.toJSONString().getBytes());
        return "indexPage.json";
    }

    @GetMapping("/api/statistics")
    public String statistics(){

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

        try {
            Files.write(Paths.get("src/main/resources/search_engine_frontend/statistics.json"), sampleObject.toJSONString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "statistics.json";
    }

    @GetMapping("/api/search")
    public String search(@RequestParam(name = "query") String query, @RequestParam(name = "site",required = false) String site,RedirectAttributes redirectAttributes){
        JSONObject sampleObject = new JSONObject();
        if (query.length() > 0 ) {
            ArrayList<PageSearchRelevance> searchList = null;
            Site siteObj = null;
            for (int siteId = 0; siteId < applicationProps.getSites().size(); siteId++) {
                if (applicationProps.getSites().get(siteId).getUrl().equals(site)) {
                    SearchEngine searchEngine = new SearchEngine(jdbcTemplate, query, (siteId + 1));
                    searchList = searchEngine.search();
                    siteObj = new Site(applicationProps.getSites().get(siteId).getUrl(), applicationProps.getSites().get(siteId).getName());
                    break;
                }else if (site == null || site.length() == 0){
                    SearchEngine searchEngine = new SearchEngine(jdbcTemplate, query, (siteId + 1));
                    searchList = searchEngine.search();
                    siteObj = new Site(applicationProps.getSites().get(siteId).getUrl(), applicationProps.getSites().get(siteId).getName());

                }
            }
            if (searchList == null){
                sampleObject.put("result", "false");
                sampleObject.put("error", "Не найдено результатов");
            }else {
                sampleObject.put("result", "true");
                sampleObject.put("count", searchList.size());
                JSONArray jsonArray = new JSONArray();
                for (PageSearchRelevance pageSearchRelevance : searchList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("site", siteObj.getUrl());
                    jsonObject.put("siteName", siteObj.getName());
                    jsonObject.put("uri", pageSearchRelevance.getUri());
                    jsonObject.put("title", pageSearchRelevance.getTitle());
                    jsonObject.put("snippet", pageSearchRelevance.getSnippet());
                    jsonObject.put("relevance", pageSearchRelevance.getRelevance());
                    jsonArray.add(jsonObject);
                }
                sampleObject.put("data", jsonArray);
            }
        }else {
            sampleObject.put("result", "false");
            sampleObject.put("error", "Задан пустой поисковый запрос");
        }

        try {
            Files.write(Paths.get("src/main/resources/search_engine_frontend/search.json"), sampleObject.toJSONString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "search.json";
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
