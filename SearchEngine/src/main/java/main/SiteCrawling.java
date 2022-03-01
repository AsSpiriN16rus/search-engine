package main;

import main.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class SiteCrawling  extends RecursiveAction
{
    private final String url;
    private static Set<String> siteUrlListAfter = new HashSet<>();
    private static Connection connection = DBConnection.getConnection() ;
    public SiteCrawling(String url) {
        this.url = url;
    }



    @Override
    protected void compute() {
        try {
            List<SiteCrawling> tasks =  new ArrayList<>();
            Thread.sleep(1000);
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .maxBodySize(0).get();


            Elements element = document.select("a");

            for (Element em : element){
                String absHref = em.attr("abs:href");
                int indexJava = absHref.indexOf(url);
                if (indexJava != -1 && !siteUrlListAfter.contains(absHref)){
                    SiteCrawling task = new SiteCrawling(absHref);
                    task.fork();
                    tasks.add(task);
                    siteUrlListAfter.add(absHref);

                    String href = url.charAt(url.length() - 1) == '/'  ?
                            absHref.replaceAll(url, "/") : absHref.replaceAll(url, "");

                    org.jsoup.Connection.Response statusCode = Jsoup.connect(absHref).execute();
                    Page link = new Page(href,statusCode.statusCode(),document.toString()) ;

                    PreparedStatement preparedStatement = connection.prepareStatement("INSERT IGNORE INTO page(path, code,content) " +
                            "VALUES(?,?,?)");
                    preparedStatement.setString(1,link.getPath());
                    preparedStatement.setInt(2,link.getCode());
                    preparedStatement.setString(3, link.getContent());
                    preparedStatement.executeUpdate();
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
