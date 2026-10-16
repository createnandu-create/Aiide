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
  private static final int IMPORT=31, EXPORT=32;
  private static final int BG=0xff090f1e, PANEL=0xff19243a, INK=0xfff3f5ff, MUTED=0xffa9b8d8, ACCENT=0xffbba7ff, AQUA=0xff86e4e3;
  private JSONArray projects=new JSONArray();
  private long active=-1;
  private String tab="Projects";
  private LinearLayout body, root;
  private EditText projectTitle, projectDna;
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    try { projects=new JSONArray(getPreferences(0).getString("projects","[]")); }catch(Exception e){projects=new JSONArray();}
    active=getPreferences(0).getLong("active",-1);
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
    Button b=new Button(this);b.setText(label);b.setTextColor(INK);b.setTextAllCaps(false);b.setTextSize(13);b.setOnClickListener(v->action.run());return b;
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
    root.addView(text("ANDROID TEST 0.1.3   •   LOCAL PROJECTS",11,AQUA));
    LinearLayout nav=horizontal();root.addView(nav);
    String[] tabs={"Projects","Editor","Preview","About"};
    for(String t:tabs){Button b=button(t,()->{tab=t;draw();});b.setTextColor(tab.equals(t)?AQUA:MUTED);
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
    info.addView(text("Prompt DNA",13,AQUA));info.addView(projectDna);
    info.addView(button("Save project changes",()->{
      try{p.put("name",projectTitle.getText().toString());p.put("dna",projectDna.getText().toString());persist();toast("Saved locally");draw();}
      catch(Exception e){toast("Save error");}
    }));
    addGap(body,12);addTitle(body,"Scenes / Layers");
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
    JSONObject p=current();if(p==null){addTitle(body,"Select a project first");return;}
    addTitle(body,"Scene Preview");
    body.addView(text("Local composition preview (not Google Flow video generation).",12,MUTED));
    JSONArray layers=p.optJSONArray("scenes");int count=layers==null?0:layers.length();
    body.addView(new PreviewCanvas(this,p),new LinearLayout.LayoutParams(-1,dp(250)));
    body.addView(text("Project: "+p.optString("name")+" • "+count+" layers",14,AQUA));
    for(int i=0;i<count;i++){JSONObject x=layers.optJSONObject(i);if(x!=null){
      card(body).addView(text((i+1)+". "+x.optString("name")+"  ["+x.optString("type")+"]",15,INK));
    }}
    body.addView(button("Open Editor",()->{tab="Editor";draw();}));
  }
  private void aboutPage(){
    addTitle(body,"Flow Studio • Test");
    LinearLayout c=card(body);
    c.addView(text("Build 0.1.3-test",18,AQUA));
    c.addView(text("Working test features: create/open/delete projects, persistent local save, global prompt DNA, scene editor, HyperFrames JSON import, JSON export, and preview.",14,INK));
    c.addView(text("Not yet included: Google Flow automation, online AI video generation, speech, and MP4 rendering.",14,MUTED));
    c.addView(text("Standalone package: in.aitechapp.flowstudio",12,AQUA));
    c.addView(text("Testing build. No account or cloud access required.",12,MUTED));
  }
  private void startPicker(int action){
    Intent i=new Intent(action==IMPORT?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");
    if(action==EXPORT)i.putExtra(Intent.EXTRA_TITLE,"flow-studio-project.json");
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
    final JSONObject project;Paint paint=new Paint(3);
    PreviewCanvas(Context c,JSONObject p){super(c);project=p;setBackgroundColor(0xff111a30);}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);float w=getWidth(),h=getHeight();
      paint.setColor(0xff263751);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
      for(int r=1;r<=3;r++)c.drawCircle(w/2,h/2,r*Math.min(w,h)/8,paint);
      paint.setStyle(Paint.Style.FILL);paint.setColor(0xff82dbdc);
      paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(w/17,36));
      JSONArray arr=project.optJSONArray("scenes");
      String title=arr!=null&&arr.length()>0?arr.optJSONObject(0).optString("text","Scene 1"):"Add a scene to preview";
      if(title.length()>55)title=title.substring(0,55)+"…";
      c.drawText(title,w/2,h/2,paint);
      paint.setColor(0xffb6a5ff);paint.setTextSize(25);
      c.drawText(project.optString("name","Untitled"),w/2,h-28,paint);
    }
  }
}