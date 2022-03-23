package main;

import main.model.Field;
import main.model.Page;
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
    private static String urlOne;
    private static int nestingСounter = 0;
    private static Set<String> siteUrlListAfter = new HashSet<>();
    public SiteCrawling(){}
    public SiteCrawling(String url) {
        this.url = url;
    }
//    public static DataBase dataBase = new DataBase();
    @Autowired
    private JdbcTemplate jdbcTemplate;

//    @Autowired
//    public void setDataSource(DataSource dataSource){
//        this.jdbcTemplate = new JdbcTemplate(dataSource);
//
//    }

//    public static DataSource mysqlDataSource() {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
//        dataSource.setUrl("jdbc:mysql://localhost:3306/search_engine?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
//        dataSource.setUsername("root");
//        dataSource.setPassword("q1w2e3r4t5A1");
//        return dataSource;
//    }

//    public void lemText(Document document, String url){
//        StringBuilder stringBuilder = new StringBuilder();
//        HashMap <String,Double> documentWeight = new HashMap<>();
//        for (Field tag :  findAllField()){
//
//            String tagText = document.select(tag.getSelector()).text();
//            documentWeight.put(tagText, tag.getWeight());
//            stringBuilder.append(tagText + " ");
//        }
//
//
////        Lemmatizer.lemmatizerText(stringBuilder.toString(), true);
////        Lemmatizer.lemRank(documentWeight, url);
//    }

    @Override
    protected void compute() {
        try {
//            setDataSource(mysqlDataSource());
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

//
//            lemText(document,href);

            for (SiteCrawling item : tasks){
                item.join();
            }


        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public void addField(Page link){
        jdbcTemplate.update("INSERT IGNORE INTO page(path, code,content) VALUES(?,?,?)",
                link.getPath(),link.getCode(),link.getContent());
    }

    public void dropTable(){
        String sql = "TRUNCATE lemma";
        jdbcTemplate.update(sql);
        sql = "TRUNCATE page";
        jdbcTemplate.update(sql);
        sql = "TRUNCATE `index`";
        jdbcTemplate.update(sql);
    }

        public List<Field> findAllField() {

            String sql = "SELECT * FROM search_engine.field";
            List<Field> fields = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) ->
                            new Field(
                                    rs.getInt("id"),
                                    rs.getString("name"),
                                    rs.getString("selector"),
                                    rs.getDouble("weight")
                            )
            );
        return fields;
        }


}
