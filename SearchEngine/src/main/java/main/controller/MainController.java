package main.controller;

import main.model.ApplicationProps;
import main.service.Lemmatizer;
import main.service.SearchEngine;
import main.service.SiteCrawling;
import main.model.PageSearchRelevance;
import main.model.Site;
import main.model.StartIndexingDto;
import main.repository.DropTableRepository;
import main.repository.LemmaRepository;
import main.repository.PageRepository;
import main.repository.SiteRepository;
import org.apache.log4j.Logger;
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

    private static final String RESULT = "result";
    private static final String ERROR = "error";
    private static final String SITES_COUNT = "sites";
    private static final String PAGES_COUNT = "pages";
    private static final String LEMMAS_COUNT = "lemmas";
    private static final String IS_INDEXING = "isIndexing";
    private static final String URL = "url";
    private static final String NAME = "name";
    private static final String STATUS_SITE = "status";
    private static final String STATUS_TIME = "statusTime";
    private static final String TOTAL = "total";
    private static final String DETAILED = "detailed";
    private static final String STATISTICS = "statistics";
    private static final String COUNT = "count";
    private static final String SITE = "site";
    private static final String SITE_NAME = "siteName";
    private static final String URI = "uri";
    private static final String TITLE = "title";
    private static final String SNIPPET = "snippet";
    private static final String RELEVANCE = "relevance";
    private static final String DATA = "data";

    private Logger logger = Logger.getLogger(MainController.class);
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
        logger.info("Запуск индексации");
        SiteRepository siteRepository = new SiteRepository(jdbcTemplate);
        if (isInterruptRequired) {
            logger.info("Индексация остановленна");
            return "index";
        } else {
            DropTableRepository dropTableRepository = new DropTableRepository(jdbcTemplate);
            dropTableRepository.dropTable();
            siteRepository.addSite(applicationProps.getSites().get(siteId).getUrl(),applicationProps.getSites().get(siteId).getName());

            logger.info("Start forkJoinPool");
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            String url = applicationProps.getSites().get(siteId).getUrl();
            forkJoinPool.invoke(new SiteCrawling(url,siteId + 1, url, jdbcTemplate));
            logger.info("End forkJoinPool");

        }

        Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);

        if (isInterruptRequired) {
            logger.info("Индексация остановленна");
            return "index";
        } else {
            logger.info("Start Lemmatizer");
            lemmatizer.lemText("lemmatizer");
            lemmatizer.outputMap(siteId + 1);
            logger.info("End Lemmatizer");
        }

        if (isInterruptRequired) {
            logger.info("Индексация остановленна");
            return "index";
        } else {
            logger.info("Start Lemmatizer Rank");
            lemmatizer.lemText("rank");
            logger.info("End Lemmatizer Rank");
        }
        siteRepository.updateSite(siteId);
        indexingStarted = false;
        return "index";
    }

    @GetMapping("/api/startIndexing")
    public StartIndexingDto startIndexing()
    {
        StartIndexingDto startIndexingDto = new StartIndexingDto();

        JSONObject jsonObject = new JSONObject();
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
            jsonObject.put(RESULT, true);
        } else {
            startIndexingDto.setResult(false);
            startIndexingDto.setError("Индексация уже идет");
            logger.info("Индексация уже идет");
        }

        return startIndexingDto;
    }

    @GetMapping("/api/stopIndexing")
    public JSONObject stopIndexing(){
        JSONObject jsonObject = new JSONObject();

        if (!indexingStarted){
            jsonObject.put(RESULT, false);
            jsonObject.put(ERROR, "Индексация не запущена");
            logger.info("Индексация не запущена");
        }else {
            isInterruptRequired = true;
            jsonObject.put(RESULT, true);
            logger.info("Остановка индексации");
        }

        return jsonObject;
    }

    @PostMapping("/api/indexPage")
    public JSONObject indexPage(@RequestParam(value = "url") String url) throws IOException {
        logger.info("Начало индексации одной страницы");
        JSONObject jsonObject = new JSONObject();
        try {
            org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
        }catch (Exception ex){
            ex.getMessage();
            jsonObject.put(RESULT, false);
            jsonObject.put(ERROR, "Страница не найдена");
            return jsonObject;
        }
        if (url.length() == 0){
            jsonObject.put(RESULT, false);
            jsonObject.put(ERROR, "Заполните поле");
        }
        if (url.length() > 0) {
            for (int siteId = 0; siteId < applicationProps.getSites().size(); siteId++) {
                String str = applicationProps.getSites().get(siteId).getUrl();
                if (url.contains(str)) {
                    Lemmatizer lemmatizer = new Lemmatizer(siteId + 1, jdbcTemplate);
                    lemmatizer.lemTextOnePage(url, "lemmatizer", str);
                    lemmatizer.lemTextOnePage(url, "rank", str);
                    jsonObject.put(RESULT, true);
                    break;
                } else {
                    jsonObject.put(RESULT, false);
                    jsonObject.put(ERROR, "Данная страница находится за пределами сайтов," +
                            "указанных в конфигурационном файле");
                }

            }
        }
        
        return jsonObject;
    }

    @GetMapping("/api/statistics")
    public JSONObject statistics(){
        JSONObject sampleObject = new JSONObject();
        sampleObject.put(RESULT, "true");
        SiteRepository siteRepository = new SiteRepository(jdbcTemplate);
        PageRepository pageRepository = new PageRepository(jdbcTemplate);
        LemmaRepository lemmaRepository = new LemmaRepository(jdbcTemplate);
        JSONObject total = new JSONObject();
        total.put(SITES_COUNT, siteRepository.countSite());
        total.put(PAGES_COUNT, pageRepository.countPage());
        total.put(LEMMAS_COUNT, lemmaRepository.countLemma());
        total.put(IS_INDEXING, isIndexing);
        
        JSONObject statistics = new JSONObject();

        
        JSONArray detailed = new JSONArray();

        for (Site siteObj : siteRepository.findAllSite()){
            JSONObject detailed1 = new JSONObject();

            detailed1.put(URL, siteObj.getUrl());
            detailed1.put(NAME, siteObj.getName());
            detailed1.put(STATUS_SITE, siteObj.getStatus());
            String statusTime = String.valueOf(siteObj.getStatusTime());
            detailed1.put(STATUS_TIME,statusTime);
            detailed1.put(ERROR, siteObj.getLastError());
            detailed1.put(PAGES_COUNT, pageRepository.countPageWithId(siteObj.getId()));
            detailed1.put(LEMMAS_COUNT, lemmaRepository.countLemmaWithId(siteObj.getId()));

            detailed.add(detailed1);
        }
        
        statistics.put(TOTAL,total);
        statistics.put(DETAILED, detailed);
        
        sampleObject.put(STATISTICS, statistics);

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
                sampleObject.put(RESULT, "false");
                sampleObject.put(ERROR, "Не найдено результатов");
            }else {
                sampleObject.put(RESULT, "true");
                sampleObject.put(COUNT, searchList.size());
                JSONArray jsonArray = new JSONArray();
                for (PageSearchRelevance pageSearchRelevance : searchList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(SITE, siteObj.getUrl());
                    jsonObject.put(SITE_NAME, siteObj.getName());
                    jsonObject.put(URI, pageSearchRelevance.getUri());
                    jsonObject.put(TITLE, pageSearchRelevance.getTitle());
                    jsonObject.put(SNIPPET, pageSearchRelevance.getSnippet());
                    jsonObject.put(RELEVANCE, pageSearchRelevance.getRelevance());
                    jsonArray.add(jsonObject);
                }
                sampleObject.put(DATA, jsonArray);
            }
        }else {
            sampleObject.put(RESULT, "false");
            sampleObject.put(ERROR, "Задан пустой поисковый запрос");
        }
        return sampleObject;
    }
}
