package main.service;

import main.repository.FieldRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.RecursiveAction;

@Repository
public class SiteCrawling  extends RecursiveAction
{
    private String url;
    private String urlOne;
    private Integer siteId;
    private static int nestingСounter = 0;

    private static Set<String> siteUrlListAfter = new HashSet<>();


    @Autowired
    private JdbcTemplate jdbcTemplate;

    public SiteCrawling(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SiteCrawling(){}
    public SiteCrawling(String urlOne, Integer siteId, String url, JdbcTemplate jdbcTemplate) {
        this.urlOne = urlOne;
        this.siteId = siteId;
        this.jdbcTemplate = jdbcTemplate;
        this.url = url;
    }

    @Override
    protected void compute() {
        try {
            if (urlOne == url){
                siteUrlListAfter.add(url + "/");
            }

            List<SiteCrawling> tasks =  new ArrayList<>();
            Thread.sleep(1000);
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .maxBodySize(0).get();

            String href = null;
            org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
            if(urlOne == url){
                href = url.replaceAll(urlOne, "/");
            }else{
                href = url.replaceAll(urlOne, "");
            }

            if (url.charAt(url.length() - 1)  != '#' ){
                siteUrlListAfter.add(url);
                FieldRepository fieldRepository = new FieldRepository(jdbcTemplate);
                if (!href.equals("/")) {
                    fieldRepository.addField(siteId, href, statusCode.statusCode(), document.toString());
                }else if (href.equals("/") && urlOne == url){
                    fieldRepository.addField(siteId, href, statusCode.statusCode(), document.toString());
                }
            }

            Elements element = document.select("a");
            for (Element em : element){
                String absHref = em.attr("abs:href");
                int indexJava = absHref.indexOf(url);
                if (indexJava != -1 && !siteUrlListAfter.contains(absHref)){
                    SiteCrawling task = new SiteCrawling(urlOne,siteId, absHref, jdbcTemplate);
                    task.fork();
                    tasks.add(task);
                    siteUrlListAfter.add(absHref);
                }
            }

            for (SiteCrawling item : tasks){
                item.join();
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
