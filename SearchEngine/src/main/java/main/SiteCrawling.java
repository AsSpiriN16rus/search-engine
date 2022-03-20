package main;

import liquibase.pro.packaged.D;
import main.model.Field;
import main.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class SiteCrawling  extends RecursiveAction
{
    private String url;
    private static String urlOne;
    private static int nestingСounter = 0;
    private static Set<String> siteUrlListAfter = new HashSet<>();
    public SiteCrawling(String url) {
        this.url = url;
    }
    private static DataBase dataBase = new DataBase();



    public void lemText(Document document, String url){
        StringBuilder stringBuilder = new StringBuilder();
        HashMap <String,Double> documentWeight = new HashMap<>();
        for (Field tag :  dataBase.findAllField()){

            String tagText = document.select(tag.getSelector()).text();
            documentWeight.put(tagText, tag.getWeight());
            stringBuilder.append(tagText + " ");
        }


        Lemmatizer.lemmatizerText(stringBuilder.toString(), true);
        Lemmatizer.lemRank(documentWeight, url);
    }

    @Override
    protected void compute() {
        try {
            if (nestingСounter == 0 ) {
                nestingСounter = 1;
                urlOne = url;
                dataBase.dropTable();
            }

            List<SiteCrawling> tasks =  new ArrayList<>();
            Thread.sleep(1000);
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .maxBodySize(0).get();


            String href = urlOne.charAt(urlOne.length() - 1) == '/'  ?
                    url.replaceAll(urlOne, "/") : url.replaceAll(urlOne, "");

            org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();
            Page link = new Page(href,statusCode.statusCode(),document.toString()) ;
            siteUrlListAfter.add(url);
            dataBase.addField(link);


            Elements element = document.select("a");
            for (Element em : element){
                String absHref = em.attr("abs:href");
                int indexJava = absHref.indexOf(url);
                if (indexJava != -1 && !siteUrlListAfter.contains(absHref)){
                    SiteCrawling task = new SiteCrawling(absHref);
                    task.fork();
                    tasks.add(task);
                    siteUrlListAfter.add(absHref);
                }

            }


            lemText(document,href);

            for (SiteCrawling item : tasks){
                item.join();
            }


        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


}
