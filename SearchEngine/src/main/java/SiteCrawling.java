import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;

public class SiteCrawling  extends RecursiveAction
{
    private final String url;
    private static Set<String> siteUrlList = new HashSet<>();
    private static Connection connection = DBConnection.getConnection();

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
            Elements elements = document.select("head");
            String htmlDocument = elements.toString();

            org.jsoup.Connection.Response statusCode = Jsoup.connect(url).execute();

            Elements element = document.select("a");

            for (Element em : element){
                String absHref = em.attr("abs:href");
                int indexJava = absHref.indexOf(url);
                if (indexJava != -1 && !siteUrlList.contains(absHref)){
                    System.out.println(absHref);
                    siteUrlList.add(absHref);
                    SiteCrawling task = new SiteCrawling(absHref);
                    task.fork();
                    tasks.add(task);
                    String href = absHref.replaceAll(url, "");
                    Statement statement = connection.createStatement();
                    statement.addBatch("INSERT IGNORE INTO page(path, code,content) " +
                            "VALUES('" + href + "', '" + statusCode.statusCode() + "','" + htmlDocument + "')");
                    statement.executeBatch();
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
