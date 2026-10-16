package in.aitechapp.flowstudio;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LocalVideoExporterTest {
  @Test public void realVideoIsPlayableH264() throws Exception {
    JSONObject project=new JSONObject();
    project.put("name","APK MP4 Smoke Test");
    JSONArray scenes=new JSONArray();
    JSONObject first=new JSONObject();
    first.put("name","First Scene");
    first.put("text","Hello Telugu ఈశ్వరరావు");
    first.put("type","text");
    scenes.put(first);
    project.put("scenes",scenes);
    File f=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"smoke.mp4");
    if(f.exists())f.delete();
    final int[] progress={0};
    LocalVideoExporter.export(project,f,n->progress[0]=n);
    assertTrue("Output MP4 file missing",f.isFile());
    assertTrue("MP4 smaller than 2 KB",f.length()>2048);
    assertEquals("Progress never completed",100,progress[0]);
    MediaExtractor extractor=new MediaExtractor();
    try{
      extractor.setDataSource(f.getAbsolutePath());
      assertTrue("No media tracks",extractor.getTrackCount()>0);
      boolean avc=false;
      for(int i=0;i<extractor.getTrackCount();i++){
        MediaFormat fmt=extractor.getTrackFormat(i);
        if("video/avc".equals(fmt.getString(MediaFormat.KEY_MIME)))avc=true;
      }
      assertTrue("Video/AVC track missing",avc);
    }finally{extractor.release();f.delete();}
  }
}