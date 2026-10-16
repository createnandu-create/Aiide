# AI Tech Flow Studio — Android install test 0.1.3
Native Android project; no API keys, no Google Flow automation, no video generator.
TEST FEATURES: Create, open, delete projects; save DNA; create/edit/remove scenes; import HyperFrames JSON with "layers" or simple "scenes"; export JSON; static preview. All data local.
LIMITATIONS: No MP4 renderer or advanced animation in this lightweight installation smoke-test. APK must be tested on a real device.
CI: .github/workflows/flow-studio-test.yml runs on branch push, builds :app:assembleDebug, checks alignment and signature, and uploads the installable debug APK as an artifact.
Different GitHub runner debug keys will not support in-place app updates. For early testing, uninstall previous debug builds if Android rejects signatures; future releases require consistent signing.
Privacy: No INTERNET permission; documents are imported and exported through Android system picker.
