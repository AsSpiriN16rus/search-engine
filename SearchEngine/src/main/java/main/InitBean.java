package main;

import org.apache.tomcat.jni.Time;
import org.hibernate.type.LocalDateTimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.concurrent.ForkJoinPool;

@Component
public class InitBean
{
    private JdbcTemplate jdbcTemplate;
    private String url;

    public InitBean(JdbcTemplate jdbcTemplate,String url){
        this.url = url;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public InitBean(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void run(){
//        System.out.println("Start forkJoinPool");
//        String url = "http://www.playback.ru/";
//        ForkJoinPool forkJoinPool = new ForkJoinPool();
//        forkJoinPool.invoke(new SiteCrawling(url, jdbcTemplate));           // ready forkjoin
//        System.out.println("End forkJoinPool");
//
//        System.out.println("Start Lemmatizer");
//        Lemmatizer lemmatizer = new Lemmatizer(jdbcTemplate);
//        lemmatizer.lemText("lemmatizer");                                       // ready lemmatizer
//        System.out.println("End Lemmatizer");
//
//        System.out.println("Start Lemmatizer Rank ");
//        lemmatizer.lemText("rank");                                      // ready lemmatizer rank
//        System.out.println("End Lemmatizer Rank ");

//        System.out.println("Start Search Rank ");
//        String searchText = "телефон хонор старый дешево";
//        SearchEngine searchEngine = new SearchEngine(jdbcTemplate,searchText);
//        searchEngine.search();
//        System.out.println("End Search Rank ");

    }

}
