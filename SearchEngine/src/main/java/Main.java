import java.util.concurrent.ForkJoinPool;

public class Main
{
    public static void main(String[] args) {
        String url = "https://lenta.ru";
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new SiteCrawling(url));
    }
}
