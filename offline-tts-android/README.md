# Telugu Offline Voice — Android debug proof-of-concept

This is *ordinary offline Android Telugu TextToSpeech*, **NOT voice cloning**. It does not install a neural model by itself. Users must install Telugu offline voice data once using the Android TTS engine's supported install screen. The APK requests NO INTERNET permission. It chooses only voices reporting `isNetworkConnectionRequired() == false`.

Features: paste Telugu text; inspect local voices; open Android install-voice-data; speak long text in 180-character chunks; export merged WAV via Android's MediaStore to `Music/TeluguVoiceStudio`. Chunk export assumes the TTS engine supports PCM WAV from synthesizeToFile. Test on real 4 GB devices; support is *not guaranteed*. For real Piper neural Telugu TTS, use the separate experimental offline PWA in `voice-testing/offline.html`.

Build with the standalone workflow `.github/workflows/offline-tts-debug.yml` on branch `telugu-voice-testing`. Android Studio not needed on the phone. Never merge into main until tested.


## Experimental models #2 and #3 (2026-11-21, research build)

The main application retains the original Android offline TTS mode. Use
**5. New Voice Models (Lyric / Pravega research)** to open the extra screen.

### #2 Lyric Voice Telugu (Ahana)

Source: https://huggingface.co/10X-Technologies/lyric-voice-tts-te

**NOT yet a working Android synthesis adapter:** upstream provides a
Python/PyTorch inference example, but this project has NOT ported it to Android
native inference. The screen provides model/licence information and deliberately
disables speech and WAV for this selection. No fake fallback to system voice.
A future port would need Telugu-script-only input and spelled-out numbers.
Review the Lyric Community License's attribution and commercial terms first.

### #3 Pravega Pocket TTS (offline external-engine adapter, research only)

Source: https://huggingface.co/dheeyantra/pravega-pocket-tts

The Android screen talks exclusively to an INSTALLED Pocket TTS Android Engine
(package: org.pockettts.android.engine) using Android's standard TextToSpeech API.
This is a **bridge**, not embedded JNI inference in our APK.
It checks that the selected engine is specifically Pocket TTS and exposes
an offline Telugu voice, refusing fallback and network voices. Android TTS
does not expose the actual model weights to client apps, so the user must verify
that the imported Telugu voice truly comes from Pravega using an audible test.
A short text preview and experimental WAV export (Android 10+) are provided.

The upstream independent Android engine is here:
https://github.com/The-unknown-Shadowman/PocketTTS-Android-Engine

#### Prepare your personal research model pack (on a computer)

1. Review the original Dhee Research-Only License v1.0.
2. Download the upstream ONNX files and required notices using Hugging Face CLI:

       hf download dheeyantra/pravega-pocket-tts --include "onnx/*" "LICENSE" "NOTICE" --local-dir ./pravega

3. Produce an importable local pack:

       python3 offline-tts-android/tools/build_pravega_research_pack.py --source ./pravega --out ./pravega-te-research.zip --accept-research-license

4. Install Pocket TTS Android Engine; import the research ZIP and your consented
   speaker-reference WAV, choose that voice, and select Pocket TTS as Android's
   preferred engine under Text-to-speech settings.
5. Reopen our app, select Pravega and test a short Telugu sentence offline.

**Licensing:** Pravega is research-only. Commercial or monetized YouTube speech,
or a public commercial service, requires separate permission/licensing from
Dheeyantra. Never clone a voice without consent.

**Technical limitations:** This app does not bundle model weights or the native
ONNX runtime. The model packer has not been verified against a complete
research-model download and a physical Android device in this session.
If the independent engine does not expose a local Telugu voice, preview/export
are disabled. Neither model is claimed to be a working embedded APK engine.
