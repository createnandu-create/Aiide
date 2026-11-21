package in.aitechapp.teluguoffline;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Additional experimental model pathways. Important:
 * - Pravega runs ONLY through a separately installed offline Pocket TTS engine
 *   with a user-imported Telugu ONNX language pack. Android does not ship it.
 * - Lyric Voice has no verified native Android runtime here; it is deliberately
 *   never mislabeled as working or routed to an unrelated system voice.
 * - This class DOES NOT bundle or redistribute restricted model weights.
 */
public class ExperimentalModelActivity extends Activity implements TextToSpeech.OnInitListener {
  private static final String POCKET_PACKAGE = "org.pockettts.android.engine";
  private static final String ENGINE_RELEASES = "https://github.com/The-unknown-Shadowman/PocketTTS-Android-Engine/releases";
  private static final String PRAVEGA = "https://huggingface.co/dheeyantra/pravega-pocket-tts";
  private static final String LYRIC = "https://huggingface.co/10X-Technologies/lyric-voice-tts-te";
  private static final Locale TE = new Locale("te", "IN");
  private static final int PRAVEGA_MODE = 3;
  private static final int LYRIC_MODE = 2;

  private int mode = LYRIC_MODE;
  private TextToSpeech tts;
  private Voice selectedVoice;
  private boolean ready = false, exporting = false;
  private File exportPart;
  private TextView status, modelInfo;
  private EditText editor;
  private Button play, export, openEngine;

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(28, 26, 28, 48);
    root.setBackgroundColor(Color.rgb(252, 250, 245));
    scroll.addView(root); setContentView(scroll);

    TextView title = line(root, "TE L U G U  •  Voice Studio", 25, true, Color.rgb(29, 31, 31));
    title.setPadding(0, 0, 0, 18);
    line(root, "Experimental offline model lab", 17, true, Color.rgb(95, 80, 43));

    Button lyric = button(root, "2. Lyric Voice — Ahana (status)");
    Button pocket = button(root, "3. Pravega Pocket TTS — Research");
    modelInfo = line(root, "", 15, false, Color.rgb(60, 61, 61));
    modelInfo.setPadding(0, 15, 0, 18);

    editor = new EditText(this);
    editor.setTextSize(18);
    editor.setMinLines(5);
    editor.setTextColor(Color.rgb(33, 36, 36));
    editor.setHint("తెలుగులో నరేషన్ రాయండి");
    editor.setText("నమస్కారం! ఇది తెలుగు ఆఫ్‌లైన్ వాయిస్ నరేషన్ పరీక్ష. మీ అందరికీ స్వాగతం!");
    root.addView(editor, new LinearLayout.LayoutParams(-1, -2));

    play = button(root, "▶ Offline preview (Pravega bridge)");
    export = button(root, "Save WAV to Music (Android 10+)");
    openEngine = button(root, "Open installed Pocket TTS Engine");
    Button engineRelease = button(root, "Get Pocket TTS Android Engine");
    Button modelPage = button(root, "Official Pravega model and licence");
    Button lyricPage = button(root, "Official Lyric Voice model and licence");
    status = line(root, "", 15, false, Color.rgb(27, 94, 73));
    status.setPadding(0, 18, 0, 0);
    line(root,
      "No model weights are built into this APK. Pravega is research-only; commercial/monetized use needs permission. Lyric Voice has a separate community licence. Voice cloning requires the speaker's consent.",
      14, false, Color.rgb(93, 65, 49));

    lyric.setOnClickListener(v -> selectMode(LYRIC_MODE));
    pocket.setOnClickListener(v -> selectMode(PRAVEGA_MODE));
    engineRelease.setOnClickListener(v -> browse(ENGINE_RELEASES));
    modelPage.setOnClickListener(v -> browse(PRAVEGA));
    lyricPage.setOnClickListener(v -> browse(LYRIC));
    openEngine.setOnClickListener(v -> {
      try {
        Intent i = getPackageManager().getLaunchIntentForPackage(POCKET_PACKAGE);
        if (i != null) startActivity(i);
        else showStatus("Pocket TTS Android Engine is not installed. Use the official release link and import a compatible Telugu research pack.");
      } catch (Exception e) { showStatus("Unable to launch engine: " + e.getMessage()); }
    });
    play.setOnClickListener(v -> speakNow());
    export.setOnClickListener(v -> exportNow());
    selectMode(LYRIC_MODE);
  }

  private TextView line(LinearLayout root, String text, int sp, boolean bold, int color) {
    TextView t = new TextView(this);
    t.setText(text); t.setTextSize(sp); t.setTextColor(color);
    t.setPadding(0, 8, 0, 8);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    root.addView(t); return t;
  }
  private Button button(LinearLayout root, String label) {
    Button b = new Button(this); b.setAllCaps(false); b.setText(label);
    root.addView(b, new LinearLayout.LayoutParams(-1, -2)); return b;
  }
  private void browse(String uri) {
    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri))); }
    catch(Exception e) { showStatus("No browser available: " + uri); }
  }
  private void showStatus(String text) { runOnUiThread(() -> { if (status != null) status.setText(text); }); }

  private void selectMode(int next) {
    mode = next; ready = false; selectedVoice = null; exporting = false;
    if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
    play.setEnabled(false); export.setEnabled(false);
    openEngine.setEnabled(mode == PRAVEGA_MODE);
    if (mode == LYRIC_MODE) {
      modelInfo.setText("Lyric Voice — Telugu Ahana (336M). The upstream model runs with Python/PyTorch. No verified compatible Android inference runtime is integrated. Speech and WAV are intentionally disabled — no fake or system-voice fallback. Latin English and digits must be written as Telugu words.");
      showStatus("Lyric Voice: model documented; ON-DEVICE INFERENCE NOT INTEGRATED.");
      return;
    }
    modelInfo.setText("Pravega Pocket TTS — Telugu research adapter. Install the independent ARM64 Pocket TTS Android Engine, then import a compatible Pravega INT8 Telugu pack and configure your own consented reference voice. The app requests ONLY that engine and refuses any default-engine fallback. Actual imported pack identity cannot be verified by Android TTS.");
    showStatus("Checking installed Pocket TTS engine and local Telugu voice...");
    // Android TextToSpeech constructor may fall back to the default engine.
    // onInit explicitly refuses fallback using getCurrentEngine().
    tts = new TextToSpeech(this, this, POCKET_PACKAGE);
  }

  @Override public void onInit(int outcome) {
    if (mode != PRAVEGA_MODE) return;
    if (outcome != TextToSpeech.SUCCESS || tts == null) {
      showStatus("Pocket TTS Android Engine unavailable. Install it and a compatible Telugu model pack first.");
      return;
    }
    if (!POCKET_PACKAGE.equals(tts.getCurrentEngine())) {
      showStatus("BLOCKED: Android selected a different TTS engine. No fallback permitted.");
      return;
    }
    Set<Voice> voices = tts.getVoices();
    if (voices != null) {
      for (Voice v : voices) {
        if (v.getLocale() != null &&
            "te".equals(v.getLocale().getLanguage()) &&
            !v.isNetworkConnectionRequired()) {
          selectedVoice = v; break;
        }
      }
    }
    if (selectedVoice == null) {
      showStatus("No offline Telugu voice exposed by Pocket TTS Engine. Import a compatible Telugu model pack in its settings, and reopen this screen.");
      return;
    }
    // Set language first, then explicit offline voice to avoid resetting selection.
    tts.setLanguage(TE);
    int result = tts.setVoice(selectedVoice);
    ready = result == TextToSpeech.SUCCESS;
    play.setEnabled(ready); export.setEnabled(ready && Build.VERSION.SDK_INT >= 29);
    if (ready) {
      showStatus("Pocket TTS Telugu voice detected: " + selectedVoice.getName() +
        ". This verifies a LOCAL Telugu engine voice, NOT the underlying model weights. Play a test and verify it is your imported Pravega research pack.");
    } else showStatus("Could not activate the offline Pocket TTS Telugu voice.");
  }

  private String validatedText() {
    return ExperimentalText.pravegaTelugu(editor.getText().toString());
  }
  private void speakNow() {
    if (mode != PRAVEGA_MODE || !ready || tts == null) {
      showStatus("This voice is not ready. No substitute model will speak.");
      return;
    }
    try {
      String text = validatedText();
      tts.stop(); exporting = false;
      tts.setSpeechRate(1.0f);
      int rc = tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), "PRAVEGA-PREVIEW");
      if (rc != TextToSpeech.SUCCESS) showStatus("Offline engine rejected preview.");
      else showStatus("Requested local Telugu preview. Listen and verify voice identity/quality.");
    } catch (IllegalArgumentException e) { showStatus(e.getMessage()); }
  }

  private void exportNow() {
    if (Build.VERSION.SDK_INT < 29) { showStatus("This experimental WAV save requires Android 10+."); return; }
    if (mode != PRAVEGA_MODE || !ready || tts == null || exporting) {
      showStatus("No verified local engine voice is ready for WAV synthesis."); return;
    }
    try {
      String text = validatedText();
      exporting = true; exportPart = new File(getCacheDir(), "pravega-preview.wav");
      if (exportPart.exists()) exportPart.delete();
      tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
        @Override public void onStart(String id) { }
        @Override public void onDone(String id) {
          if (!"PRAVEGA-WAV".equals(id) || !exporting) return;
          exporting = false;
          new Thread(() -> saveWav()).start();
        }
        @Override public void onError(String id) {
          exporting = false; showStatus("Pocket TTS engine could not synthesize WAV.");
        }
        @Override public void onError(String id, int code) { onError(id); }
      });
      int rc = tts.synthesizeToFile(text, new Bundle(), exportPart, "PRAVEGA-WAV");
      if (rc != TextToSpeech.SUCCESS) { exporting = false; showStatus("Engine refused WAV file export."); }
      else showStatus("Rendering offline WAV in selected external engine...");
    } catch (IllegalArgumentException e) { exporting = false; showStatus(e.getMessage()); }
  }

  private void saveWav() {
    Uri uri = null;
    try {
      if (exportPart == null || !exportPart.exists() || exportPart.length() < 44)
        throw new IOException("Engine returned no usable audio.");
      try (FileInputStream verify = new FileInputStream(exportPart)) {
        byte[] head = new byte[12];
        if (verify.read(head) < 12 || head[0] != 'R' || head[1] != 'I' ||
            head[2] != 'F' || head[3] != 'F' ||
            head[8] != 'W' || head[9] != 'A' || head[10] != 'V' || head[11] != 'E')
          throw new IOException("Engine returned unsupported non-WAV data.");
      }
      ContentValues values = new ContentValues();
      values.put(MediaStore.Audio.Media.DISPLAY_NAME, "pocket-telugu-" + System.currentTimeMillis() + ".wav");
      values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav");
      values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/TeluguVoiceStudio");
      values.put(MediaStore.Audio.Media.IS_PENDING, 1);
      uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
      if (uri == null) throw new IOException("Could not create audio in Music.");
      try (FileInputStream in = new FileInputStream(exportPart);
           OutputStream out = getContentResolver().openOutputStream(uri)) {
        if (out == null) throw new IOException("Music output is unavailable.");
        byte[] buf = new byte[16384]; int read;
        while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
      }
      ContentValues done = new ContentValues();
      done.put(MediaStore.Audio.Media.IS_PENDING, 0);
      getContentResolver().update(uri, done, null, null);
      showStatus("Saved actual engine WAV under Music/TeluguVoiceStudio.");
    } catch (Exception e) {
      if (uri != null) try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
      showStatus("WAV save failed: " + e.getMessage());
    } finally { if (exportPart != null) exportPart.delete(); }
  }
  @Override protected void onDestroy() {
    exporting = false;
    if (tts != null) { tts.stop(); tts.shutdown(); }
    super.onDestroy();
  }
}