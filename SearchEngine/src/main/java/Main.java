

import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ForkJoinPool;

public class Main
{
    @Value("${urlSite}")
    private String urlSite;

    public static void main(String[] args) {

        String url = "http://www.playback.ru/";
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new SiteCrawling(url));
    }
}
