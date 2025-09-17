package com.HLDLLD.structural.Proxy;

public class Demo {

    public static void main(String[] args) {
        YouTubeDownloader naive = new YouTubeDownloader(new ThirdPartyYouTubeClass());
        YouTubeDownloader smart = new YouTubeDownloader(new YouTubeCacheProxy());
        long naivee = test(naive);
        long smarte = test(smart);
        System.out.print("Time saved by caching proxy: " + (naivee - smarte) + "ms");

    }

    private static long test(YouTubeDownloader downloader) {
        long startTime = System.currentTimeMillis();

        // User behavior in our app:
        downloader.renderPopularVideos();
        downloader.renderVideoPage("catzzzzzzzzz");
        downloader.renderPopularVideos();
        downloader.renderVideoPage("dancesvideoo");
        // Users might visit the same page quite often.
        downloader.renderVideoPage("catzzzzzzzzz");
        downloader.renderVideoPage("someothervid");

        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.print("Time elapsed: " + estimatedTime + "ms\n");
        return estimatedTime;
    }
}
