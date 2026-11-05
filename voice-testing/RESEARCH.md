# Telugu Voice Studio — testing branch

Checked 2026-11-05. Main remains untouched. This is a browser client, not a GPU inference server. It sends reference audio and text only to the chosen service when Generate is clicked. Recordings are kept in the browser's IndexedDB and never committed to this repository. Browser data is origin-scoped: samples from an earlier downloaded HTML file will not automatically appear on this preview URL.

## Run and test

Open `index.html` over HTTPS. Choose My voice → model → New voice. Record a clean 8–10-second sample, enter the exact transcript and save. Enter one short Telugu sentence and generate. Download successful audio for offline playback. Start with one short sentence, not a long script.

Server Check verifies configuration and the API name only. It does not claim model inference success. If a shared demo requires login or has exhausted anonymous GPU quota, open its original demo. A Hugging Face token can be entered for the selected HF service in Advanced, but is not saved or published and does not guarantee quota. Do not place keys in source code.

Test link is a third-party raw.githack HTML preview of the GitHub testing branch, not GitHub Pages. It may show a confirmation before opening. No uptime guarantee. GitHub stores code; inference is performed by external Hugging Face demo services.

## Candidates and evidence

| Candidate | Purpose and evidence | Decision |
|---|---|---|
| [IndicF5](https://github.com/AI4Bharat/IndicF5) | Official model supports Telugu and reference audio + exact transcript. | Implemented official GPU and community CPU adapters. |
| [Maya 2 Native](https://huggingface.co/maya-research/maya2-native) | Telugu, zero-shot cloning, streaming; 2.5B unique parameters / about 5 GB BF16 weights. Apache-2.0 core, upstream component terms apply. | Implemented clone_speak adapter from the live API schema and source. |
| [Praxy](https://github.com/praxelhq/praxy) | Telugu/code-mix cloning. Its current demo source uses Modal GPU handles; code-mix processing calls Anthropic. Source: https://huggingface.co/spaces/Srikesh/praxy-voice-demo/blob/main/app.py | Not represented as a free independent working backend. Demo probe returned HTTP 503. |
| [Indic-Mio](https://huggingface.co/SPRINGLab/Indic-Mio) | 0.6B, Indic-language coverage, codec speaker-embedding cloning. Model card says Apache-2.0. | Local evaluation candidate; not integrated or quality-tested. Model API page is not a hosted inference endpoint. |
| [OmniVoice](https://huggingface.co/k2-fsa/OmniVoice) | Multilingual reference cloning. | Broad-language candidate; no Telugu quality result from this session. Component licences require review. Not integrated. |
| [Indic Parler](https://huggingface.co/ai4bharat/indic-parler-tts) | Telugu narration and style control; Apache-2.0 model card. | Existing Studio mode retained; not sample-based cloning. |
| [Indic-Speak](https://huggingface.co/bodhan-ai/indic-speak) | Telugu/code-mixed narration, fixed voices. Custom licence includes permission before hosting for others. | Not treated as unrestricted open source or integrated clone backend. |

These are a focused shortlist, not a claim to have found every model or ranked audible quality. No side-by-side human listening benchmark was possible because no new audio was produced in the live inference probes.

## Google

[Google AI Studio speech](https://aistudio.google.com/generate-speech) supports Telugu according to its [speech guide](https://ai.google.dev/gemini-api/docs/speech-generation). It is a hosted service, not an offline open-source engine. Free availability is model/account/quota dependent; [pricing](https://ai.google.dev/gemini-api/docs/pricing) does not offer unlimited free compute. Link provided separately, no Google key required by this app and no paid API silently called.

## Live verification

- Official IndicF5: `/config` reachable, `synthesize_speech` input order verified. Example loader succeeded. Generation with example reference and a short Telugu sentence returned `event: error`, `data: null`.
- Community heyviru IndicF5: config and example loader reachable; same generation test returned `event: error`, `data: null`.
- Maya official Studio: config and `/gradio_api/info` reachable. `clone_speak` parameters and returned `audio_url` verified against demo source. Short Telugu built-in-voice smoke test returned `ZeroGPU quota exceeded`. Clone inference not proven.
- Praxy demo probe: HTTP 503. No generation attempted.
- Therefore a reachable page or endpoint must not be called a successful voice clone. Results can differ for the user's own session/quota, but success is not promised.

## Client changes

Original visual design retained. Explicit model selection, check-server action, original demo links, input validation, local device speech, cancellation/timeouts and WAV export. Clone failure never falls back to an unrelated voice. Noise reduction is off by default to preserve reference timbre. Tokens are session-only and sent only to configured HF hosts. Returned audio URLs are resolved against the server origin. Sample-based Maya cloning uses the clip transcript's detected language and the requested text language.

Offline device reading uses installed system voices, not cloning. Model weights are not included. Full offline cloning needs a separate inference runtime and suitable hardware; this web preview does not provide it.

## Follow-up investigation: Google 3.8 and deployment (2026-11-05)

Google's September 2026 release includes **Gemini 3.8 Flash TTS / Flash-Lite TTS**, with consent-verified **Voice Replication** APIs and prebuilt narration voices. This is newer than earlier Gemini 2.5 narration-only examples. However **Google says AI Studio Voice Replication is not available in India**, among other regions. Do not imply that the user can clone voice with the pasted `gemini-flash-latest:generateContent` text example. API availability and account eligibility must be checked without bypassing geographic restrictions.

For dependable prebuilt Telugu narration, this branch adds a *separate* `voice-testing/api/tts.js` server-side Gemini 2.5 Flash Preview TTS adapter and a `Google TTS` HTML tab. It must be deployed to a compatible serverless host with a newly rotated secret `GEMINI_API_KEY` environment variable. The HTML has no API key. Until configured, this mode intentionally reports not configured; no Google generation success is claimed.

The original user-shared key appeared in a chat and must be revoked. This branch has NOT copied it into code, a commit, or a request. The connected Vercel account returned no team or Git-linked project, so an actual Vercel production deployment and live cloud inference test were not possible.

The Device voice mode now splits a long script into small chunks for mobile TTS reading when a suitable system voice is available. This is not downloadable cloned audio and foreground browser behavior can vary.

Hosting comparison: GitHub Pages/Cloudflare Pages can publish static HTML, but cannot run Python/CUDA GPU inference. Vercel Hobby permits serverless code and is subject to strict usage quotas/noncommercial limitations. WordPress.com Free restricts custom JavaScript and cannot host this GPU model. Dedicated clone inference still needs a running GPU instance, consent procedures, per-user rate limits, and quota/cost controls before open public launch.

Sources:
- https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-8-text-to-speech/
- https://ai.google.dev/gemini-api/docs/speech-generation
- https://ai.google.dev/gemini-api/docs/rate-limits
- https://huggingface.co/docs/hub/spaces-zerogpu
- https://vercel.com/docs/plans/hobby
- https://wordpress.com/support/code/
