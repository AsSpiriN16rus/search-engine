package main;

import main.model.Field;
import main.model.FieldMapper;
import main.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
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

    public void dropTable(){
        String sql = "TRUNCATE lemma";
        jdbcTemplate.update(sql);
        sql = "TRUNCATE page";
        jdbcTemplate.update(sql);
    }
    public List<Field> findAllField() {
        String sql = "SELECT * FROM search_engine.field";
        Field field = jdbcTemplate.queryForObject(sql, new FieldMapper());
        return field;
    }


//    public List findAllField() {
//        String sql = "SELECT selector FROM search_engine.field";
//        return jdbcTemplate.queryForList(sql,String.class);
//    }

    public void addField(Page link){
        jdbcTemplate.update("INSERT IGNORE INTO page(path, code,content) VALUES(?,?,?)",
                            link.getPath(),link.getCode(),link.getContent());
    }
    public void lemText(Document document){
        StringBuilder stringBuilder = new StringBuilder();
        for (Object tag : findAllField()){
            System.out.println(tag.getClass().toString());
//            stringBuilder.append(document.select(tag.toString()).text() + " ");
        }
//        Lemmatizer.LemmatizerText(jdbcTemplate,stringBuilder.toString());
    }

    @Override
    protected void compute() {
        try {

            setDataSource(mysqlDataSource());
            if (nestingСounter == 0 ) {
                nestingСounter = 1;
                urlOne = url;
                dropTable();
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
            addField(link);


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


            lemText(document);


            for (SiteCrawling item : tasks){
                item.join();
            }


        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


}
