package com.HLDLLD.structural.Proxy;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class YouTubeCacheProxy implements ThirdPartyYouTubeLib{

    private ThirdPartyYouTubeLib thirdPartyYouTubeLib;
    HashMap<String,Video> cachepop = new HashMap<>();
    HashMap<String,Video> cacheall = new HashMap<>();



    public YouTubeCacheProxy(){
        this.thirdPartyYouTubeLib = new ThirdPartyYouTubeClass();
    }
    @Override
    public HashMap<String, Video> popularVideos() {
        if(cachepop.isEmpty()) {
            cachepop = thirdPartyYouTubeLib.popularVideos();
        }
        else {
            System.out.println("Retrieved list from cache.");
        }
        return cachepop;
    }

    @Override
    public Video getVideo(String videoId) {
        Video video = cacheall.get(videoId);
//        Optional<Video> temp = Optional.ofNullable(video);
//        if(Optional.ofNullable(video).isEmpty()){
//
//        }
        if(video == null){
            video = thirdPartyYouTubeLib.getVideo(videoId);
            cacheall.put(videoId, video);
        }
        else {
            System.out.println("Retrieved video '" + videoId + "' from cache.");
        }
        return video;
    }
    public void reset() {
        cachepop.clear();
        cacheall.clear();
    }
}
