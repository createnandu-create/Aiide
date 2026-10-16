package in.aitechapp.flowstudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private static final int IMPORT=31, EXPORT=32, SAVE_VIDEO=33;
  private static final int BG=0xff090f1e, PANEL=0xff19243a, INK=0xfff3f5ff, MUTED=0xffa9b8d8, ACCENT=0xffbba7ff, AQUA=0xff86e4e3;
  private JSONArray projects=new JSONArray();
  private long active=-1;
  private String tab="Projects";
  private LinearLayout body, root;
  private EditText projectTitle, projectDna;
  private File lastVideo;
  private volatile boolean exportRunning=false;
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    try { projects=new JSONArray(getPreferences(0).getString("projects","[]")); }catch(Exception e){projects=new JSONArray();}
    active=getPreferences(0).getLong("active",-1);
    File dir=getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
    lastVideo=new File(dir!=null?dir:getFilesDir(),"flow-studio-last.mp4");
    draw();
  }
  private void persist(){
    getPreferences(0).edit().putString("projects",projects.toString()).putLong("active",active).apply();
  }
  private JSONObject current(){
    for(int i=0;i<projects.length();i++){JSONObject p=projects.optJSONObject(i);if(p!=null && p.optLong("id")==active)return p;}return null;
  }
  private TextView text(String s,int size,int color){
    TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(dp(3),dp(5),dp(3),dp(5));return v;
  }
  private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  private void addGap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
  private void addTitle(LinearLayout l,String s){TextView t=text(s,24,INK);t.setTypeface(null,1);l.addView(t);}
  private Button button(String label,Runnable action){
    Button b=new Button(this);b.setText(label);b.setTextColor(INK);b.setAllCaps(false);b.setTextSize(13);b.setOnClickListener(v->action.run());return b;
  }
  private LinearLayout card(LinearLayout host){
    LinearLayout c=vertical();c.setPadding(dp(13),dp(10),dp(13),dp(12));
    android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
    bg.setColor(PANEL);bg.setCornerRadius(dp(16));c.setBackground(bg);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);host.addView(c,lp);return c;
  }
  private EditText edit(String hint,String value, boolean tall){
    EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(INK);e.setText(value);
    e.setSingleLine(!tall);if(tall){e.setMinLines(3);e.setGravity(Gravity.TOP);}
    e.setTextSize(15);return e;
  }
  private void draw(){
    root=vertical();root.setBackgroundColor(BG);root.setPadding(dp(12),dp(8),dp(12),dp(8));
    if(Build.VERSION.SDK_INT>=35){
      root.setOnApplyWindowInsetsListener((v,insets)->{
        android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());
        v.setPadding(dp(12),i.top+dp(8),dp(12),i.bottom+dp(8));return insets;
      });
    }
    setContentView(root);
    TextView header=text("AI TECH  /  FLOW STUDIO",17,INK);header.setTypeface(null,1);root.addView(header);
    root.addView(text("v0.1.4  •  OFFLINE VIDEO WORKBENCH",11,AQUA));
    LinearLayout nav=horizontal();root.addView(nav);
    String[] tabs={"Projects","Editor","Preview","About"};
    for(int i=0;i<tabs.length;i++){final String t=tabs[i];String[] shortNames={"HOME","EDIT","PLAY","INFO"};
      Button b=button(shortNames[i],()->{tab=t;draw();});b.setTextColor(tab.equals(t)?AQUA:MUTED);
      b.setTextSize(11);b.setSingleLine(true);b.setPadding(0,0,0,0);
      nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body=vertical();body.setPadding(dp(2),dp(12),dp(2),dp(30));scroll.addView(body);
    switch(tab){
      case "Projects": projectsPage();break;
      case "Editor": editorPage();break;
      case "Preview": previewPage();break;
      default: aboutPage();
    }
  }
  private void projectsPage(){
    addTitle(body,"My Projects");
    body.addView(text("Create and save projects on your phone. Import HyperFrames JSON to create editable scenes.",13,MUTED));
    body.addView(button("＋ New project",()->showNewProject()));
    body.addView(button("⇩ Import JSON from phone",()->startPicker(IMPORT)));
    addGap(body,12);
    for(int i=0;i<projects.length();i++){
      JSONObject p=projects.optJSONObject(i);if(p==null)continue;
      LinearLayout c=card(body);c.addView(text(p.optString("name","Project"),20,INK));
      c.addView(text(p.optJSONArray("scenes")==null?"0 scenes":p.optJSONArray("scenes").length()+" scenes",12,AQUA));
      LinearLayout r=horizontal();c.addView(r);
      long id=p.optLong("id");
      r.addView(button("Open Editor",()->{active=id;persist();tab="Editor";draw();}));
      r.addView(button("Delete",()->new AlertDialog.Builder(this).setTitle("Delete project?")
        .setMessage("This local project will be removed.")
        .setPositiveButton("Delete",(d,w)->{for(int j=0;j<projects.length();j++)if(projects.optJSONObject(j).optLong("id")==id){projects.remove(j);break;}if(active==id)active=-1;persist();draw();})
        .setNegativeButton("Cancel",null).show()));
    }
    if(projects.length()==0)card(body).addView(text("No projects yet. Start with New project or Import JSON.",14,MUTED));
  }
  private void showNewProject(){
    EditText n=edit("Project title","Untitled Project",false);
    new AlertDialog.Builder(this).setTitle("New project").setView(n)
      .setPositiveButton("Create",(d,w)->{JSONObject p=new JSONObject();try{
          long id=System.currentTimeMillis();
          p.put("id",id);p.put("name",n.getText().toString().trim());
          p.put("dna","");p.put("scenes",new JSONArray());projects.put(p);active=id;persist();tab="Editor";draw();
        }catch(Exception e){toast("Create failed: "+e.getMessage());}})
      .setNegativeButton("Cancel",null).show();
  }
  private void editorPage(){
    JSONObject p=current();
    if(p==null){card(body).addView(text("Create or open a project first.",16,MUTED));body.addView(button("Go to Projects",()->{tab="Projects";draw();}));return;}
    addTitle(body,"Scene Editor");
    LinearLayout info=card(body);
    projectTitle=edit("Project title",p.optString("name",""),false);
    projectDna=edit("Global prompt DNA",p.optString("dna",""),true);
    info.addView(text("Project name",13,AQUA));info.addView(projectTitle);
    info.addView(text("Prompt DNA / Story Script",13,AQUA));
    projectDna.setMinLines(3);projectDna.setMaxLines(6);projectDna.setVerticalScrollBarEnabled(true);info.addView(projectDna);
    info.addView(button("Expand prompt editor",()->expandDna(p)));
    info.addView(button("Save project changes",()->{
      try{p.put("name",projectTitle.getText().toString());p.put("dna",projectDna.getText().toString());persist();toast("Saved locally");draw();}
      catch(Exception e){toast("Save error");}
    }));
    info.addView(button("Build story scenes from prompt (local)",()->buildScenesFromPrompt(p)));
    addGap(body,12);addTitle(body,"Scenes / Layers");
    body.addView(text("These are local text-animation scenes, not AI videos.",12,MUTED));
    body.addView(button("＋ Add text scene",()->showSceneEditor(p,-1)));
    JSONArray layers=p.optJSONArray("scenes");
    if(layers==null){layers=new JSONArray();try{p.put("scenes",layers);}catch(Exception ignored){}}
    for(int i=0;i<layers.length();i++){
      JSONObject item=layers.optJSONObject(i);if(item==null)continue;final int idx=i;
      LinearLayout c=card(body);
      c.addView(text((i+1)+". "+item.optString("name","Layer"),17,INK));
      c.addView(text(item.optString("type","text")+"  •  "+item.optString("text",""),12,MUTED));
      LinearLayout controls=horizontal();c.addView(controls);
      controls.addView(button("Edit",()->showSceneEditor(p,idx)));
      controls.addView(button("Remove",()->{JSONArray a=p.optJSONArray("scenes");a.remove(idx);persist();draw();}));
    }
    body.addView(button("⇩ Import JSON",()->startPicker(IMPORT)));
    body.addView(button("⇧ Export JSON",()->startPicker(EXPORT)));
  }
  private void showSceneEditor(JSONObject p,int index){
    JSONArray scenes=p.optJSONArray("scenes");if(scenes==null)scenes=new JSONArray();
    JSONObject old=index>=0?scenes.optJSONObject(index):null;
    LinearLayout f=vertical();f.setPadding(dp(16),dp(8),dp(16),0);
    EditText name=edit("Layer name",old==null?"Title":old.optString("name"),false);
    EditText content=edit("Text / prompt",old==null?"ఈశ్వరరావు":old.optString("text"),true);
    f.addView(name);f.addView(content);
    final JSONArray target=scenes;
    new AlertDialog.Builder(this).setTitle(index>=0?"Edit scene":"New text scene").setView(f)
      .setPositiveButton("Save",(d,w)->{
        JSONObject obj=old==null?new JSONObject():old;
        try{obj.put("name",name.getText().toString());obj.put("text",content.getText().toString());if(old==null){obj.put("type","text");obj.put("start",0);obj.put("end",8);target.put(obj);}p.put("scenes",target);persist();draw();}
        catch(Exception e){toast("Could not save scene");}
      }).setNegativeButton("Cancel",null).show();
  }
  private void previewPage(){
    JSONObject p=current();
    if(p==null){addTitle(body,"Open a project first");body.addView(button("Projects",()->{tab="Projects";draw();}));return;}
    addTitle(body,"Animated Preview");
    body.addView(text("Real-time local animation. Use Export MP4 for an actual phone video.",12,MUTED));
    JSONArray layers=p.optJSONArray("scenes");int count=layers==null?0:layers.length();
    body.addView(new PreviewCanvas(this,p),new LinearLayout.LayoutParams(-1,dp(228)));
    body.addView(text("Project: "+p.optString("name")+"   •   "+count+" scenes",14,AQUA));
    if(count>8)body.addView(text("Local MP4 test exports only the first 8 scenes.",12,MUTED));
    body.addView(button("▶ CREATE REAL MP4 (OFFLINE)",()->startVideoExport(p)));
    if(lastVideo!=null&&lastVideo.exists()&&lastVideo.length()>0){
      LinearLayout player=card(body);
      player.addView(text("Latest generated MP4 ("+Math.max(1,lastVideo.length()/1024)+" KB)",16,AQUA));
      VideoView vv=new VideoView(this);
      vv.setVideoPath(lastVideo.getAbsolutePath());
      MediaController mc=new MediaController(this);
      mc.setAnchorView(vv);vv.setMediaController(mc);
      player.addView(vv,new LinearLayout.LayoutParams(-1,dp(235)));
      player.addView(button("▶ Play MP4",()->vv.start()));
      player.addView(button("↓ SAVE MP4 TO PHONE",()->startPicker(SAVE_VIDEO)));
    }
    for(int i=0;i<count;i++){JSONObject x=layers.optJSONObject(i);if(x!=null){
      card(body).addView(text((i+1)+". "+x.optString("name")+"  ["+x.optString("type")+"]",15,INK));
    }}
    body.addView(button("Open Editor",()->{tab="Editor";draw();}));
  }
  private void startVideoExport(JSONObject p){
    JSONArray arr=p.optJSONArray("scenes");
    if(arr==null||arr.length()==0){toast("Add a scene before exporting");return;}
    if(exportRunning){toast("Video export is already running");return;}
    int max=Math.min(8,arr.length());
    new AlertDialog.Builder(this).setTitle("Render local MP4?")
      .setMessage(max+" scene(s), "+(max*4)+" seconds, 480×270 H.264, no audio. Uses your phone, NOT Google Flow AI.")
      .setPositiveButton("Render",(d,w)->renderNow(p))
      .setNegativeButton("Cancel",null).show();
  }
  private void renderNow(JSONObject p){
    if(exportRunning)return;
    exportRunning=true;
    ProgressDialog progress=new ProgressDialog(this);
    progress.setTitle("Rendering MP4");
    progress.setMessage("Creating video frames on your phone…");
    progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progress.setMax(100);progress.setCancelable(false);progress.show();
    JSONObject snapshot;
    try {snapshot=new JSONObject(p.toString());}catch(Exception e){exportRunning=false;progress.dismiss();toast("Project error");return;}
    new Thread(()->{
      try{
        File dir=lastVideo.getParentFile();if(!dir.exists())dir.mkdirs();
        File temp=new File(dir,"flow-studio-rendering.mp4");
        if(temp.exists())temp.delete();
        LocalVideoExporter.export(snapshot,temp,n->runOnUiThread(()->progress.setProgress(n)));
        if(lastVideo.exists()&&!lastVideo.delete())throw new IOException("Cannot replace old output");
        if(!temp.renameTo(lastVideo))throw new IOException("Cannot save completed MP4");
        runOnUiThread(()->{exportRunning=false;progress.dismiss();tab="Preview";draw();toast("MP4 ready — tap Play or Save MP4");});
      }catch(Exception e){
        runOnUiThread(()->{exportRunning=false;progress.dismiss();
          new AlertDialog.Builder(this).setTitle("Video export error")
            .setMessage(e.toString()+"\nThis phone's hardware encoder may not support YUV420 input.").setPositiveButton("OK",null).show();
        });
      }
    },"flow-video-render").start();
  }
  private void expandDna(JSONObject p){
    EditText full=edit("Paste story or prompt",projectDna.getText().toString(),true);
    full.setMinLines(8);full.setMaxLines(16);
    new AlertDialog.Builder(this).setTitle("Full Prompt DNA").setView(full)
      .setPositiveButton("Apply",(d,w)->{projectDna.setText(full.getText());try{p.put("dna",full.getText().toString());persist();}catch(Exception ignored){}draw();})
      .setNegativeButton("Cancel",null).show();
  }
  private void buildScenesFromPrompt(JSONObject p){
    String prompt=projectDna.getText().toString().trim();
    if(prompt.isEmpty()){toast("Paste a story first");return;}
    java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("\\[(\\d{1,2}:\\d{2})\\s*[-–]\\s*(\\d{1,2}:\\d{2})\\]");
    java.util.regex.Matcher matcher=pattern.matcher(prompt);
    java.util.ArrayList<String> sections=new java.util.ArrayList<>();
    java.util.ArrayList<String> spans=new java.util.ArrayList<>();
    while(matcher.find()&&sections.size()<8){
      spans.add(matcher.group(1)+"–"+matcher.group(2));
      int a=matcher.end(), b=matcher.find()?matcher.start():prompt.length();
      String content=prompt.substring(a,b).replaceAll("[\\[\\]]"," ").trim();
      if(content.length()>160)content=content.substring(0,160);
      sections.add(content.isEmpty()?"Scene "+sections.size():content);
      matcher.region(b,prompt.length());
    }
    if(sections.isEmpty()){
      String[] paras=prompt.split("\\n\\s*\\n");
      for(String item:paras){if(sections.size()>=8)break;
        String q=item.trim().replace('\n',' ');if(q.isEmpty())continue;
        sections.add(q.length()>160?q.substring(0,160):q);
        spans.add("Scene "+(sections.size()));
      }
    }
    if(sections.isEmpty()){toast("No scenes could be found");return;}
    final JSONArray scenes=new JSONArray();
    for(int i=0;i<sections.size();i++){
      JSONObject o=new JSONObject();
      try{o.put("name",spans.get(i));o.put("type","text");o.put("text",sections.get(i));o.put("start",i*4);o.put("end",(i+1)*4);scenes.put(o);}catch(Exception ignored){}
    }
    new AlertDialog.Builder(this).setTitle("Create "+scenes.length()+" scenes?")
      .setMessage("This converts the written prompt into a LOCAL animated storyboard. It does not generate AI footage or music. Existing scenes will be replaced.")
      .setPositiveButton("Create",(d,w)->{try{
        p.put("dna",prompt);p.put("scenes",scenes);persist();draw();toast("Storyboard created");
      }catch(Exception e){toast("Save error");}})
      .setNegativeButton("Cancel",null).show();
  }
  private void aboutPage(){
    addTitle(body,"Flow Studio • Test");
    LinearLayout c=card(body);
    c.addView(text("Build 0.1.4-test",18,AQUA));
    c.addView(text("Local projects, improved editor, prompt-to-storyboard, animated preview, real offline H.264 MP4 export and playback, JSON import/export.",14,INK));
    c.addView(text("Not AI generation: no Google Flow integration, music synthesis, narration, or imported video editing. Local MP4 export is 480×270, text-animation only and silent.",14,MUTED));
    c.addView(text("Standalone package: in.aitechapp.flowstudio",12,AQUA));
    c.addView(text("Testing build. No account or cloud access required.",12,MUTED));
  }
  private void startPicker(int action){
    Intent i=new Intent(action==IMPORT?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(action==SAVE_VIDEO?"video/mp4":"application/json");
    if(action==EXPORT)i.putExtra(Intent.EXTRA_TITLE,"flow-studio-project.json");
    if(action==SAVE_VIDEO)i.putExtra(Intent.EXTRA_TITLE,"ai-tech-flow-video.mp4");
    startActivityForResult(i,action);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(result!=RESULT_OK || data==null || data.getData()==null)return;
    Uri uri=data.getData();
    if(request==IMPORT){
      try(InputStream in=getContentResolver().openInputStream(uri)){
        if(in==null)throw new IOException("No input");
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;
        while((n=in.read(chunk))!=-1){b.write(chunk,0,n);if(b.size()>2000000)throw new IOException("JSON too large");}
        JSONObject input=new JSONObject(b.toString("UTF-8"));
        JSONObject p=new JSONObject();long id=System.currentTimeMillis();p.put("id",id);
        JSONObject meta=input.optJSONObject("meta");
        p.put("name",input.optString("name",meta!=null?meta.optString("name","Imported JSON"):"Imported JSON"));
        p.put("dna",input.optString("dna",input.optString("global_dna","")));
        JSONArray sceneSource=input.optJSONArray("scenes");if(sceneSource==null)sceneSource=input.optJSONArray("layers");
        JSONArray resultScenes=new JSONArray();
        if(sceneSource!=null)for(int j=0;j<sceneSource.length();j++){
          JSONObject obj=sceneSource.optJSONObject(j);if(obj==null)continue;
          JSONObject s=new JSONObject(obj.toString());
          if(!s.has("name"))s.put("name","Scene "+(j+1));
          if(!s.has("text"))s.put("text",s.optString("prompt",""));
          resultScenes.put(s);
        }
        p.put("scenes",resultScenes);projects.put(p);active=id;persist();tab="Editor";draw();
        toast("Imported "+resultScenes.length()+" scenes");
      }catch(Exception e){toast("Import failed: "+e.getMessage());}
    } else if(request==SAVE_VIDEO){
      if(lastVideo==null||!lastVideo.exists()){toast("Render video first");return;}
      try(InputStream in=new FileInputStream(lastVideo);OutputStream out=getContentResolver().openOutputStream(uri)){
        if(out==null)throw new IOException("Cannot open destination");byte[] chunk=new byte[16384];int n;
        while((n=in.read(chunk))!=-1)out.write(chunk,0,n);out.flush();toast("MP4 saved to phone");
      }catch(Exception e){toast("MP4 save error: "+e.getMessage());}
    } else if(request==EXPORT){
      JSONObject p=current();if(p==null){toast("Choose project");return;}
      try(OutputStream out=getContentResolver().openOutputStream(uri)){
        if(out==null)throw new IOException("No output");
        out.write(p.toString(2).getBytes(StandardCharsets.UTF_8));out.flush();toast("JSON exported");
      }catch(Exception e){toast("Export failed: "+e.getMessage());}
    }
  }
  private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
  private static class PreviewCanvas extends View {
    final JSONObject project;long started=android.os.SystemClock.uptimeMillis();
    PreviewCanvas(Context c,JSONObject p){super(c);project=p;}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);
      JSONArray scenes=project.optJSONArray("scenes");
      int count=scenes==null?0:scenes.length();
      int playCount=Math.max(1,Math.min(8,count));
      long elapsed=(android.os.SystemClock.uptimeMillis()-started);
      int current=(int)((elapsed/4000)%playCount);
      float phase=(elapsed%4000)/4000f;
      int save=c.save();
      c.scale(getWidth()/(float)LocalVideoExporter.W,getHeight()/(float)LocalVideoExporter.H);
      LocalVideoExporter.render(c,project,count>0?scenes.optJSONObject(current):null,current,playCount,phase);
      c.restoreToCount(save);
      if(isAttachedToWindow())postInvalidateDelayed(80);
    }
  }
}
