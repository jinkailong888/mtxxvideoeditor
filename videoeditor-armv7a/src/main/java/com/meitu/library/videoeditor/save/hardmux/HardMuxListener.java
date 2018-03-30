package com.meitu.library.videoeditor.save.hardmux;


/**
 * Created by wyh3 on 2018/3/30.
 */
public interface HardMuxListener {

   void onVideoFrame(byte[] data, double pts) ;

   void onAudioFrame(byte[] data, double pts) ;

   void onVideoDone() ;

   void onAudioDone() ;
}
