package main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.ForkJoinPool;

@SpringBootApplication
public class Main
{

    public static void main(String[] args) {

        SpringApplication.run(main.Main.class, args);

//        String textLem =
//                "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
//
//        Lemmatizer.LemmatizerText(textLem);
        String url = "http://www.playback.ru/";
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new SiteCrawling(url));

    }
}
