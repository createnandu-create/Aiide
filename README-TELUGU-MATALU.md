# తెలుగు మాటలు · Native Android test 2.1.0

A real Android platform Activity built in Java. No WebView, remote page dependency, React Native, third-party runtime, AI model, account, ads or trackers. This work lives on the `telugu-matalu-native` branch so the Droidide Pro project on main is kept separate.

- Native Telugu to readable English letters conversion, with 183 curated casual spellings and custom word overrides.
- Four native workspaces: Convert, Listen, My words, Settings.
- Direct Android TextToSpeech with Telugu language selection, installed engine/voice picker, voice-data installation shortcut, native media audio focus, error codes, and a short Telugu voice test.
- Original Telugu speech with genuine onRangeStart-driven English-letter highlighting; sentence highlighting when a speech engine does not emit word ranges.
- Pause resumes the last reported word or sentence; speech stops on background and audio-focus loss.
- Voice input uses the installed Android recognizer UI. Conversion works offline; network speech services may require internet.
- Native copy, share, storage-access-framework text export; no broad storage or microphone permission.
- Light/dark/system themes, rounded cards, touch feedback, optional system tap sound and speech-active waveform.

## APK and source

`downloads/TeluguMatalu-2.1.0-test.apk` is the locally compiled, verified test APK distributed with this commit. Application ID `in.aitechapp.telugumatalu`, versionCode 210, Android 8.0+ (API 26), target/compile API 35. It contains no native ELF libraries and is architecture-independent. It uses a test signing identity, not a production release identity. CI rebuilds use a fresh runner-local test key and are verification artifacts, not an update-signing guarantee.

Build: install JDK 17, Android platform 35, build-tools 35.0.0; set ANDROID_SDK_ROOT; run `bash native/build.sh`. The script uses AAPT2, javac, D8, zipalign and apksigner. No Gradle downloads or Maven dependencies are required. Build scripts keep signing keys outside the repository.

Run engine checks:
```
javac -encoding UTF-8 -d /tmp/telugu-tests native/src/in/aitechapp/telugumatalu/Transliterator.java native/test/EngineTest.java
java -cp /tmp/telugu-tests EngineTest native/assets/words.tsv
```

Validated: complete Java compilation, 183 dictionary mappings and mixed-text/conjunct/number/custom-spelling/alignment tests, AAPT package inspection, DEX packaging, v2/v3 signature verification and 16KB-compatible ZIP alignment. No physical Android device or emulator was available in this build environment. Actual speech output and perceived voice quality need device testing; if the installed TTS engine itself cannot synthesize Telugu, the app exposes setup and error information rather than claiming successful audio.

`web/` is the browser version source snapshot; it retains browser-specific voice limitations. `docs/REPOSITORY_REVIEW.md` records the three requested repositories and why their Python neural runtimes are not claimed to run inside this APK.

Original project code is MIT licensed in `native/LICENSE`. There are no copied neural-model weights.
