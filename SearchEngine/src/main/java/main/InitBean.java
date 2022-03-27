package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ForkJoinPool;

@Component
public class InitBean
{
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public InitBean(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void run(){
        System.out.println("Start forkJoinPool");
        String url = "http://www.playback.ru/";
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new SiteCrawling(url, jdbcTemplate));
        System.out.println("End forkJoinPool");
        System.out.println("Start Lemmatizer");
        Lemmatizer lemmatizer = new Lemmatizer(jdbcTemplate);
        lemmatizer.lemText(url);
        System.out.println("End Lemmatizer");
    }
}
