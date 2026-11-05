# Telugu Offline Voice — Android debug proof-of-concept

This is *ordinary offline Android Telugu TextToSpeech*, **NOT voice cloning**. It does not install a neural model by itself. Users must install Telugu offline voice data once using the Android TTS engine's supported install screen. The APK requests NO INTERNET permission. It chooses only voices reporting `isNetworkConnectionRequired() == false`.

Features: paste Telugu text; inspect local voices; open Android install-voice-data; speak long text in 180-character chunks; export merged WAV via Android's MediaStore to `Music/TeluguVoiceStudio`. Chunk export assumes the TTS engine supports PCM WAV from synthesizeToFile. Test on real 4 GB devices; support is *not guaranteed*. For real Piper neural Telugu TTS, use the separate experimental offline PWA in `voice-testing/offline.html`.

Build with the standalone workflow `.github/workflows/offline-tts-debug.yml` on branch `telugu-voice-testing`. Android Studio not needed on the phone. Never merge into main until tested.
