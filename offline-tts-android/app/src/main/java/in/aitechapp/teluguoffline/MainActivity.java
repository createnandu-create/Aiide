package in.aitechapp.teluguoffline;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
  TextToSpeech tts;
  Voice offlineVoice;
  TextView status;
  EditText text;
  Button play, export;
  boolean exporting=false;
  ArrayList<String> chunks=new ArrayList<>();
  ArrayList<File> generated=new ArrayList<>();
  int index=0;
  final Locale TE=new Locale("te","IN");

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    ScrollView scroller=new ScrollView(this);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24,26,24,40);root.setBackgroundColor(Color.rgb(17,26,43));
    scroller.addView(root);setContentView(scroller);
    TextView title=new TextView(this);title.setText("తెలుగు Offline Voice");
    title.setTextSize(26);title.setTypeface(null,Typeface.BOLD);title.setTextColor(Color.WHITE);root.addView(title);
    TextView note=new TextView(this);note.setText("Uses installed Android Telugu OFFLINE speech. No cloud key, no voice cloning. Phone needs an offline Telugu TTS voice.");
    note.setTextColor(Color.rgb(198,216,239));note.setTextSize(14);root.addView(note);
    text=new EditText(this);text.setMinLines(7);text.setGravity(48);text.setTextSize(19);
    text.setTextColor(Color.WHITE);text.setHintTextColor(Color.LTGRAY);
    text.setHint("తెలుగులో వ్రాయండి");text.setText("నమస్కారం! ఇది తెలుగు ఆఫ్‌లైన్ వాయిస్ పరీక్ష.");
    root.addView(text,new LinearLayout.LayoutParams(-1,-2));
    Button check=button(root,"1. Check offline Telugu voice");
    Button install=button(root,"2. Install Telugu voice data (Android settings)");
    play=button(root,"3. ▶ Speak offline");
    Button stop=button(root,"■ Stop");
    export=button(root,"4. Export WAV to Music folder");
    status=new TextView(this);status.setTextColor(Color.rgb(164,221,190));status.setTextSize(16);
    status.setPadding(0,22,0,0);status.setText("Checking installed voices...");
    root.addView(status);
    check.setOnClickListener(v->chooseOfflineVoice());
    install.setOnClickListener(v->{
      try{startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));}
      catch(Exception e){try{
        Intent i=new Intent("com.android.settings.TTS_SETTINGS");startActivity(i);
      }catch(Exception ignored){setStatus("Open Settings → Accessibility → Text-to-speech → Install voice data → Telugu.");}}
    });
    play.setOnClickListener(v->speak());
    stop.setOnClickListener(v->{exporting=false;if(tts!=null)tts.stop();setStatus("Stopped.");});
    export.setOnClickListener(v->exportWav());
    tts=new TextToSpeech(this,this);
  }
  Button button(LinearLayout root,String label){
    Button b=new Button(this);b.setAllCaps(false);b.setText(label);
    root.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;
  }
  void setStatus(String s){runOnUiThread(()->status.setText(s));}
  @Override public void onInit(int result){
    if(result!=TextToSpeech.SUCCESS){setStatus("Android TTS engine initialization failed. Install a speech engine.");return;}
    tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
      @Override public void onStart(String id){}
      @Override public void onDone(String id){
        if(!id.startsWith("FILE-")||!exporting)return;
        synchronized(MainActivity.this){
          index++;
          setStatus("Rendering offline WAV chunk "+index+" / "+chunks.size());
          if(index>=chunks.size()){exporting=false;new Thread(()->mergeFiles()).start();}
          else synthNext();
        }
      }
      @Override public void onError(String id){exporting=false;setStatus("Speech synthesis failed at "+id+". Please check installed offline voice.");}
      @Override public void onError(String id,int code){onError(id);}
    });
    chooseOfflineVoice();
  }
  void chooseOfflineVoice(){
    if(tts==null)return;
    offlineVoice=null;
    Set<Voice> voices=tts.getVoices();
    if(voices!=null){
      for(Voice v:voices){
        if(v.getLocale()!=null&&"te".equals(v.getLocale().getLanguage())&&!v.isNetworkConnectionRequired()){
          offlineVoice=v;break;
        }
      }
    }
    if(offlineVoice!=null&&tts.setVoice(offlineVoice)==TextToSpeech.SUCCESS){
      tts.setLanguage(offlineVoice.getLocale());
      setStatus("Offline Telugu voice READY: "+offlineVoice.getName()+". Test with internet switched off.");
    }else{
      setStatus("No usable offline Telugu voice is installed. Tap Install Telugu voice data above, then return and Check again. App will not silently use an online voice.");
    }
  }
  boolean ready(){
    if(tts==null||offlineVoice==null){setStatus("Offline Telugu voice not ready. Install Telugu voice data first.");return false;}
    return true;
  }
  ArrayList<String> split(String input){
    ArrayList<String> out=new ArrayList<>();input=input.replace("\r"," ").trim();
    while(!input.isEmpty()){
      int n=Math.min(180,input.length());
      if(n<input.length()){
        int pos=Math.max(input.lastIndexOf(' ',n),Math.max(input.lastIndexOf('।',n),input.lastIndexOf('.',n)));
        if(pos>65)n=pos+1;
      }
      String p=input.substring(0,n).trim();
      if(!p.isEmpty())out.add(p);
      input=input.substring(n).trim();
    }
    return out;
  }
  void speak(){
    if(!ready())return;
    ArrayList<String> parts=split(text.getText().toString());
    if(parts.isEmpty()){setStatus("Enter Telugu text first.");return;}
    exporting=false;tts.stop();
    tts.setSpeechRate(1.0f);
    for(int j=0;j<parts.size();j++){
      Bundle b=new Bundle();
      int r=tts.speak(parts.get(j),j==0?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,b,"SPEAK-"+j);
      if(r!=TextToSpeech.SUCCESS){setStatus("Offline speech queue failed at part "+(j+1));return;}
    }
    setStatus("Reading "+parts.size()+" parts with offline voice. No network/API key required by this app.");
  }
  void exportWav(){
    if(!ready())return;
    if(exporting){setStatus("Export already running.");return;}
    chunks=split(text.getText().toString());
    if(chunks.isEmpty()){setStatus("Enter Telugu text first.");return;}
    if(chunks.size()>350){setStatus("For this test, export up to 350 chunks per file (roughly 45,000–60,000 characters).");return;}
    tts.stop();index=0;generated.clear();exporting=true;
    setStatus("Rendering offline WAV: "+chunks.size()+" parts. Keep app open.");
    synthNext();
  }
  synchronized void synthNext(){
    if(!exporting)return;
    File f=new File(getCacheDir(),"telugu-part-"+index+".wav");generated.add(f);
    int rc=tts.synthesizeToFile(chunks.get(index),new Bundle(),f,"FILE-"+index);
    if(rc!=TextToSpeech.SUCCESS){exporting=false;setStatus("Your TTS engine refused file export at "+(index+1));}
  }
  static int le32(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16)|((b[p+3]&255)<<24);}
  static int le16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
  static void w32(OutputStream o,long v)throws IOException {for(int i=0;i<4;i++)o.write((int)(v>>(8*i))&255);}
  static void w16(OutputStream o,int v)throws IOException{o.write(v&255);o.write((v>>8)&255);}
  static class Part{File file;long start,len;int rate,channels,bits;Part(File f,long s,long l,int r,int c,int b){file=f;start=s;len=l;rate=r;channels=c;bits=b;}}
  Part inspect(File f)throws IOException {
    try(RandomAccessFile raf=new RandomAccessFile(f,"r")){
      if(raf.length()<44)throw new IOException("Part too small");
      byte[] head=new byte[12];raf.readFully(head);
      if(!new String(head,0,4,StandardCharsets.US_ASCII).equals("RIFF")||
         !new String(head,8,4,StandardCharsets.US_ASCII).equals("WAVE"))throw new IOException("TTS engine did not output WAV");
      int rate=0,ch=0,bits=0;long start=-1,len=0;
      while(raf.getFilePointer()+8<=raf.length()){
        byte[] chunk=new byte[8];raf.readFully(chunk);
        String type=new String(chunk,0,4,StandardCharsets.US_ASCII);
        long size=le32(chunk,4)&0xffffffffL;
        long pos=raf.getFilePointer();
        if(size<0||pos+size>raf.length())throw new IOException("Invalid WAV chunk");
        if(type.equals("fmt ")){
          if(size<16)throw new IOException("Invalid WAV format");
          byte[] fmt=new byte[16];raf.readFully(fmt);
          if(le16(fmt,0)!=1)throw new IOException("Only PCM WAV export supported");
          ch=le16(fmt,2);rate=le32(fmt,4);bits=le16(fmt,14);
        }
        if(type.equals("data")){start=pos;len=size;break;}
        raf.seek(pos+size+(size%2));
      }
      if(start<0||rate<1||ch<1||bits!=16)throw new IOException("Unexpected TTS audio format");
      return new Part(f,start,len,rate,ch,bits);
    }
  }
  void mergeFiles(){
    try{
      ArrayList<Part> parts=new ArrayList<>();long bytes=0;int rate=0,ch=0,bits=0;
      for(File f:generated){
        Part p=inspect(f);
        if(parts.isEmpty()){rate=p.rate;ch=p.channels;bits=p.bits;}
        if(rate!=p.rate||ch!=p.channels||bits!=p.bits)throw new IOException("Audio format changed between parts");
        bytes+=p.len;parts.add(p);
      }
      if(bytes>0xffffffffL-44)throw new IOException("Audio too large for WAV");
      ContentValues values=new ContentValues();
      values.put(MediaStore.Audio.Media.DISPLAY_NAME,"telugu-offline-"+System.currentTimeMillis()+".wav");
      values.put(MediaStore.Audio.Media.MIME_TYPE,"audio/wav");
      values.put(MediaStore.Audio.Media.RELATIVE_PATH,Environment.DIRECTORY_MUSIC+"/TeluguVoiceStudio");
      values.put(MediaStore.Audio.Media.IS_PENDING,1);
      Uri uri=getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values);
      if(uri==null)throw new IOException("Cannot create Music file");
      try(OutputStream out=getContentResolver().openOutputStream(uri)){
        if(out==null)throw new IOException("Cannot open output");
        out.write("RIFF".getBytes(StandardCharsets.US_ASCII));w32(out,36+bytes);
        out.write("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));w32(out,16);w16(out,1);w16(out,ch);
        w32(out,rate);w32(out,rate*ch*2);w16(out,ch*2);w16(out,16);
        out.write("data".getBytes(StandardCharsets.US_ASCII));w32(out,bytes);
        byte[] buf=new byte[32768];
        for(Part p:parts){try(RandomAccessFile input=new RandomAccessFile(p.file,"r")){
          input.seek(p.start);long remaining=p.len;
          while(remaining>0){int count=(int)Math.min(remaining,buf.length);input.readFully(buf,0,count);out.write(buf,0,count);remaining-=count;}
        }}
      }
      values.clear();values.put(MediaStore.Audio.Media.IS_PENDING,0);
      getContentResolver().update(uri,values,null,null);
      for(File f:generated)f.delete();
      setStatus("WAV SAVED in Music/TeluguVoiceStudio. Offline synthesis done, "+parts.size()+" chunks.");
    }catch(Exception e){setStatus("WAV export error: "+e.getMessage());}
  }
  @Override protected void onDestroy(){exporting=false;if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
