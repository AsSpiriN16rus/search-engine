package main.controllers;

import main.SiteView;
import main.model.ApplicationProps;
import main.Lemmatizer;
import main.SearchEngine;
import main.SiteCrawling;
import main.model.PageSearchRelevance;
import main.model.Site;
import main.model.StartIndexingDto;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@RestController
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

    public String startIndexingMoreThreads(int siteId){
        SiteView siteView = new SiteView(jdbcTemplate);
        if (isInterruptRequired) {
            return "index";
        } else {
            siteView.dropTable();
            siteView.addSite(applicationProps.getSites().get(siteId).getUrl(),applicationProps.getSites().get(siteId).getName());
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            String url = applicationProps.getSites().get(siteId).getUrl();
            forkJoinPool.invoke(new SiteCrawling(url,siteId + 1, url, jdbcTemplate));


        }

        Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);

        if (isInterruptRequired) {
            return "index";
        } else {
            lemmatizer.lemText("lemmatizer");
            lemmatizer.outputMap(siteId + 1);
        }

        if (isInterruptRequired) {
            return "index";
        } else {
            lemmatizer.lemText("rank");
        }
        siteView.updateSite(siteId);
        indexingStarted = false;
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public StartIndexingDto startIndexing()
    {
        StartIndexingDto startIndexingDto = new StartIndexingDto();
        if (!indexingStarted) {
            indexingStarted = true;
            for (int siteId = 0; siteId < applicationProps.getSites().size(); siteId++) {
                int finalSiteId = siteId;
                Thread thread = new Thread(() -> {
                    new Thread(startIndexingMoreThreads(finalSiteId)).start();
                });
                thread.start();
                isIndexing = true;
            }
            startIndexingDto.setResult(true);
        } else {
            startIndexingDto.setResult(false);
            startIndexingDto.setError("Индексация уже идет");
        }

        return startIndexingDto;
    }

    @GetMapping("/api/stopIndexing")
    public JSONObject stopIndexing(){
        JSONObject jsonObject = new JSONObject();

        if (!indexingStarted){
            jsonObject.put("result", false);
            jsonObject.put("error", "Индексация не запущена");
        }else {
            isInterruptRequired = true;
            jsonObject.put("result", true);
        }
        return jsonObject;
    }

    @PostMapping("/api/indexPage")
    public JSONObject indexPage(@RequestParam(value = "url") String url) throws IOException {
        JSONObject jsonObject = new JSONObject();
        try {
            org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
        }catch (Exception ex){
            jsonObject.put("result", false);
            jsonObject.put("error", "Страница не найдена");
            return jsonObject;
        }
        if (url.length() == 0){
            jsonObject.put("result", false);
            jsonObject.put("error", "Заполните поле");
        }
        if (url.length() > 0) {
            for (int siteId = 0; siteId < applicationProps.getSites().size(); siteId++) {
                String str = applicationProps.getSites().get(siteId).getUrl();
                if (url.contains(str)) {
                    Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);
                    lemmatizer.lemTextOnePage(url, "lemmatizer", str);
                    lemmatizer.lemTextOnePage(url, "rank", str);
                    jsonObject.put("result", true);
                    break;
                } else {
                    jsonObject.put("result", false);
                    jsonObject.put("error", "Данная страница находится за пределами сайтов," +
                            "указанных в конфигурационном файле");
                }

            }
        }
        return jsonObject;
    }

    @GetMapping("/api/statistics")
    public JSONObject statistics(){
        JSONObject sampleObject = new JSONObject();
        sampleObject.put("result", "true");
        JSONObject total = new JSONObject();
        total.put("sites", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.site", Integer.class));
        total.put("pages", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.page", Integer.class));
        total.put("lemmas", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.lemma", Integer.class));
        total.put("isIndexing", isIndexing);
        
        JSONObject statistics = new JSONObject();

        
        JSONArray detailed = new JSONArray();

        SiteView siteView = new SiteView(jdbcTemplate);
        for (Site siteObj : siteView.findAllSite()){
            JSONObject detailed1 = new JSONObject();

            detailed1.put("url", siteObj.getUrl());
            detailed1.put("name", siteObj.getName());
            detailed1.put("status", siteObj.getStatus());
            String statusTime = String.valueOf(siteObj.getStatusTime());
            detailed1.put("statusTime",statusTime);
            detailed1.put("error", siteObj.getLastError());
            detailed1.put("pages", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.page where site_id = '" + (siteObj.getId()) +"'", Integer.class));
            detailed1.put("lemmas", jdbcTemplate.queryForObject("SELECT count(*) FROM search_engine.lemma where site_id = '" + (siteObj.getId()) +"'", Integer.class));

            detailed.add(detailed1);
        }
        
        statistics.put("total",total);
        statistics.put("detailed", detailed);
        
        sampleObject.put("statistics", statistics);

        return sampleObject;
    }

    @GetMapping("/api/search")
    public JSONObject search(@RequestParam(name = "query") String query, @RequestParam(name = "site",required = false) String site, RedirectAttributes redirectAttributes){
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
        return sampleObject;
    }
}
