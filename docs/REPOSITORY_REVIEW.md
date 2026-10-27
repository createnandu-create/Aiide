# Voice repository review · 27 October 2026

The three user links resolved to:

1. https://github.com/jamiepine/voicebox — MIT desktop Tauri app with Python/FastAPI backends and multiple neural TTS models. The currently documented engine language sets do not establish Telugu support. It is not a ready Android SDK. No Voicebox code or models are bundled in this APK.
2. https://github.com/samnaveenkumaroff/Indic-F5 — an IndicF5 fork. The upstream https://github.com/AI4Bharat/IndicF5 documents Telugu among 11 supported languages. Installation uses Python, model downloads and a reference speech prompt; it is not an Android library. Licensing of the full model stack must be checked before redistribution. Not embedded here.
3. https://github.com/praxelhq/praxy — code MIT; released R6 LoRA weights Apache-2.0; documents Telugu, Tamil, Hindi and English. Its Python serving/inference and base-model runtime need a separate model server or a real mobile inference port. Public source hosting does not supply GPU inference or guarantee free unlimited compute. Not embedded here.

This testing APK instead uses the Android platform TextToSpeech service directly, avoiding the browser SpeechSynthesis lifecycle that failed on the user's phone. It speaks original Telugu input, not English phonetic letters. A native UtteranceProgressListener drives state and highlighting; engines without range callbacks receive sentence highlighting. Voice data must be installed in a Telugu-capable engine. The quality is the engine's quality, not a claim of neural-model inference.

A future optional neural backend can use Praxy or IndicF5, but no server URL, cloud credential, voice clone, pretrained weights or inference service has been silently added. The provided APK is usable without any server account. No celebrity/person's voice is cloned.

Android API references:
- https://developer.android.com/reference/android/speech/tts/TextToSpeech
- https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener
