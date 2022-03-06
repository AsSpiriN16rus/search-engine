package main;

import main.model.Field;
import main.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class SiteCrawling  extends RecursiveAction
{
    private String url;
    private static String urlOne;
    private static int nestingСounter = 0;
    private static Set<String> siteUrlListAfter = new HashSet<>();
//    private static Connection connection = DBConnection.getConnection() ;
    public SiteCrawling(String url) {
        this.url = url;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);

    }


    public static DataSource mysqlDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/search_engine?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("q1w2e3r4t5A1");
        return dataSource;
    }

    public List findAllField() {
        String sql = "SELECT selector FROM search_engine.field";
        return jdbcTemplate.queryForList(sql,String.class);
    }

    public void addField(Page link){
        jdbcTemplate.update("INSERT IGNORE INTO page(path, code,content) VALUES(?,?,?)",
                            link.getPath(),link.getCode(),link.getContent());
    }

    public void lemText(Document document){
        for (Object tag : findAllField()){
            Elements element1 = document.select(tag.toString());
            for (Element em1 : element1){
                Lemmatizer.LemmatizerText(jdbcTemplate,em1.text());
            }
        }
    }

    @Override
    protected void compute() {
        try {
            setDataSource(mysqlDataSource());
            if (nestingСounter == 0 ) {
                nestingСounter = 1;
                urlOne = url;
            }
            List<SiteCrawling> tasks =  new ArrayList<>();
            Thread.sleep(1000);
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .maxBodySize(0).get();


            Elements element = document.select("a");

            lemText(document);

            for (Element em : element){
                String absHref = em.attr("abs:href");
                int indexJava = absHref.indexOf(url);
                if (indexJava != -1 && !siteUrlListAfter.contains(absHref)){
                    SiteCrawling task = new SiteCrawling(absHref);
                    task.fork();
                    tasks.add(task);
                    siteUrlListAfter.add(absHref);
                    String href = url.charAt(urlOne.length() - 1) == '/'  ?
                            absHref.replaceAll(urlOne, "/") : absHref.replaceAll(urlOne, "/");

                    org.jsoup.Connection.Response statusCode = Jsoup.connect(absHref).execute();
                    Page link = new Page(href,statusCode.statusCode(),document.toString()) ;

                    addField(link);



//                    PreparedStatement preparedStatement = connection.prepareStatement("INSERT IGNORE INTO page(path, code,content) " +
//                            "VALUES(?,?,?)");
//                    preparedStatement.setString(1,link.getPath());
//                    preparedStatement.setInt(2,link.getCode());
//                    preparedStatement.setString(3, link.getContent());
//                    preparedStatement.executeUpdate();
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
