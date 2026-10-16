package in.aitechapp.flowstudio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/** Offline H.264/MP4 renderer. Does not call a web/AI service. */
public final class LocalVideoExporter {
  public interface Progress { void update(int percent); }
  public static final int W=480, H=270, FPS=12, SECONDS_PER_SCENE=4;
  private LocalVideoExporter(){}

  public static File export(JSONObject project,File file, Progress cb) throws Exception {
    JSONArray arr=project.optJSONArray("scenes");
    if(arr==null || arr.length()==0) throw new IllegalArgumentException("Add at least one scene");
    int scenes=Math.min(8,arr.length());
    int count=scenes*SECONDS_PER_SCENE*FPS;
    MediaCodec codec=null;MediaMuxer mux=null;boolean started=false;
    Bitmap bmp=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
    Canvas canvas=new Canvas(bmp);
    int[] pixels=new int[W*H];
    try{
      MediaFormat fmt=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,W,H);
      fmt.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
      fmt.setInteger(MediaFormat.KEY_BIT_RATE,650000);
      fmt.setInteger(MediaFormat.KEY_FRAME_RATE,FPS);
      fmt.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
      codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
      codec.configure(fmt,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
      codec.start();
      mux=new MediaMuxer(file.getAbsolutePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
      int track=-1;
      MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
      int queued=0;boolean done=false;
      long lastOutput=System.currentTimeMillis();
      while(!done){
        if(queued<=count){
          int idx=codec.dequeueInputBuffer(10000);
          if(idx>=0){
            long pts=(queued*1000000L)/FPS;
            if(queued==count){
              codec.queueInputBuffer(idx,0,0,pts,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else{
              int sceneIndex=Math.min(queued/(FPS*SECONDS_PER_SCENE),scenes-1);
              float phase=(queued%(FPS*SECONDS_PER_SCENE))/(float)(FPS*SECONDS_PER_SCENE);
              JSONObject scene=arr.optJSONObject(sceneIndex);
              render(canvas,project,scene,sceneIndex,scenes,phase);
              bmp.getPixels(pixels,0,W,0,0,W,H);
              Image frame=codec.getInputImage(idx);
              if(frame==null) throw new IllegalStateException("Device encoder cannot accept YUV420 image buffers");
              try { fillYuv(frame,pixels); } finally { frame.close(); }
              codec.queueInputBuffer(idx,0,W*H*3/2,pts,0);
              cb.update(Math.min(95,(queued*95)/count));
            }
            queued++;
          }
        }
        int out;
        while((out=codec.dequeueOutputBuffer(info,queued>count?10000:0))>=0){
          ByteBuffer data=codec.getOutputBuffer(out);
          if(data!=null && info.size>0 && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
            if(!started)throw new IllegalStateException("MP4 track not initialized");
            data.position(info.offset); data.limit(info.offset+info.size);
            mux.writeSampleData(track,data,info);
            lastOutput=System.currentTimeMillis();
          }
          boolean eos=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
          codec.releaseOutputBuffer(out,false);
          if(eos){done=true;break;}
        }
        if(out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
          track=mux.addTrack(codec.getOutputFormat());
          mux.start();started=true;
        }
        if(queued>count && System.currentTimeMillis()-lastOutput>30000)
          throw new IllegalStateException("Encoder timed out while finishing video");
      }
      cb.update(100);
      return file;
    } catch(Exception e){if(file.exists())file.delete();throw e;}
    finally{
      if(codec!=null){try{codec.stop();}catch(Exception ignored){} try{codec.release();}catch(Exception ignored){}}
      if(mux!=null){if(started)try{mux.stop();}catch(Exception ignored){}try{mux.release();}catch(Exception ignored){}}
      bmp.recycle();
    }
  }

  private static void fillYuv(Image img,int[] px){
    if(img.getFormat()!=ImageFormat.YUV_420_888)throw new IllegalStateException("Unsupported encoder image format: "+img.getFormat());
    Image.Plane[] p=img.getPlanes();
    ByteBuffer y=p[0].getBuffer(),u=p[1].getBuffer(),v=p[2].getBuffer();
    int ys=p[0].getRowStride(),yp=p[0].getPixelStride();
    int us=p[1].getRowStride(),up=p[1].getPixelStride();
    int vs=p[2].getRowStride(),vp=p[2].getPixelStride();
    for(int row=0;row<H;row++){
      for(int col=0;col<W;col++){
        int c=px[row*W+col],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
        int yy=((66*r+129*g+25*b+128)>>8)+16;
        y.put(row*ys+col*yp,(byte)Math.max(0,Math.min(255,yy)));
        if((row&1)==0 && (col&1)==0){
          int uu=((-38*r-74*g+112*b+128)>>8)+128;
          int vv=((112*r-94*g-18*b+128)>>8)+128;
          u.put((row/2)*us+(col/2)*up,(byte)Math.max(0,Math.min(255,uu)));
          v.put((row/2)*vs+(col/2)*vp,(byte)Math.max(0,Math.min(255,vv)));
        }
      }
    }
  }

  private static String title(JSONObject scene){
    if(scene==null)return "New Scene";
    String x=scene.optString("text",scene.optString("prompt",""));
    if(x.trim().isEmpty())x=scene.optString("name","Scene");
    return x;
  }
  private static ArrayList<String> wrap(Paint paint,String content,int maxWidth,int maxLines){
    ArrayList<String> rows=new ArrayList<>();
    String clean=content.replace('\n',' ').trim();
    if(clean.length()>180) clean=clean.substring(0,180)+"...";
    String[] words=clean.split("\\s+");
    String line="";
    for(String word:words){
      String n=line.isEmpty()?word:line+" "+word;
      if(paint.measureText(n)>maxWidth && !line.isEmpty()){
        rows.add(line);line=word;
        if(rows.size()>=maxLines-1)break;
      }else line=n;
    }
    if(!line.isEmpty())rows.add(line);
    return rows;
  }
  public static void render(Canvas canvas,JSONObject project,JSONObject scene,int current,int total,float t){
    int w=canvas.getWidth(),h=canvas.getHeight();
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setShader(new LinearGradient(0,0,w,h,new int[]{Color.rgb(9,13,30),Color.rgb(25,36,70),Color.rgb(49,23,73)},null,Shader.TileMode.CLAMP));
    canvas.drawRect(0,0,w,h,p);p.setShader(null);
    float radius=Math.min(w,h)*(.22f+.12f*t);
    p.setColor(Color.argb(100,119,204,230));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
    canvas.drawCircle(w*.79f,h*.48f,radius,p);
    canvas.drawCircle(w*.79f,h*.48f,radius*.70f,p);
    p.setStyle(Paint.Style.FILL);
    p.setColor(Color.rgb(119,230,220));p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextSize(14);
    canvas.drawText("AI TECH FLOW STUDIO   /   LOCAL VIDEO",22,26,p);
    p.setColor(Color.rgb(172,185,225));p.setTextSize(13);
    canvas.drawText("SCENE "+(current+1)+" / "+total,22,h-20,p);
    String content=title(scene);
    p.setColor(Color.rgb(247,247,255));p.setTextSize(29);p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));
    ArrayList<String> lines=wrap(p,content,w-75,3);
    float lineHeight=43,base=h*.46f-(lines.size()-1)*lineHeight*.5f;
    int opacity=(int)(Math.min(1f,Math.min(t*5f,(1f-t)*5f))*255);
    p.setAlpha(Math.max(0,opacity));
    float x=24+(1-t)*14;
    for(int i=0;i<lines.size();i++)canvas.drawText(lines.get(i),x,base+i*lineHeight,p);
    p.setAlpha(255);p.setColor(Color.rgb(132,226,227));
    canvas.drawRoundRect(22,h-42,22+(w-44)*t,h-39,2,2,p);
  }
}
