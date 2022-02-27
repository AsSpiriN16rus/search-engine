

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Main
{
    @Value("${urlSite}")

    public static void main(String[] args) {
        String textLem =
                "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
        Lemmatizer.LemmatizerText(textLem);
        String url = "http://www.playback.ru/";
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new SiteCrawling(url));

    }
}
