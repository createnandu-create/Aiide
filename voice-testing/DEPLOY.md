# Telugu Voice Studio — HTML-first test

This is the TEST branch only. Keep \`main\` unchanged until audible generation has passed acceptance checks.

## What works and what does not

- The static HTML supports recording, storing your own reference audio in the browser, Web Speech device reading when an installed voice exists, and integration adapters for public Hugging Face demos.
- The public IndicF5/Maya demo API endpoints are quota-limited and have returned generation errors. A green "server reachable" response is not proof of successful cloning.
- **Google TTS** is a separate **prebuilt narration** mode. It is NOT the owner's cloned voice.
- A browser-hosted HTML file cannot hide a Google API key. The optional Node serverless endpoint at \`api/tts.js\` accepts short Telugu text and returns WAV; it reads \`GEMINI_API_KEY\` only from server environment variables.
- Google quota/rate limits apply. Long scripts send multiple requests and can stop partway if the quota expires. This is not an unlimited public GPU voice-cloning service.
- Google Gemini 3.8 supports consent-verified voice replication, but Google states that **AI Studio voice replication is unavailable in India** as of the cited 2026 launch note. We do not include a misleading replication button here.

## Deploy a preview on Vercel (NOT production/main)

1. Revoke/rotate any Gemini API key posted in chat or screenshots. NEVER commit or paste a key into HTML or GitHub.
2. Create a Vercel project connected to \`createnandu-create/Aiide\` with root directory \`voice-testing\`. For a non-main preview, configure the Git integration so that \`telugu-voice-testing\` deploys as a PREVIEW, not production. Alternatively import the test folder into a separate preview-only project.
3. In project Settings → Environment Variables, add \`GEMINI_API_KEY\` as a secret for the preview deployment. Your visitors never enter a key. This setup requires the site owner to provide a new key.
4. The site root should serve \`index.html\`, and \`/api/tts\` should return a JSON status for GET. An unset key should return \`configured:false\`.
5. Test a short Telugu sentence using the "Google TTS" tab. This does not prove voice cloning. Run a 10-30 minute test only after short audio generation succeeds.

## Security / public launch gates

- Introduce real server-side per-user quotas and abuse controls (ideally durable counters and a bot challenge), visibility of costs, and request limits BEFORE enabling anonymous public traffic. A client-side limit does not protect the Google account.
- Use only consented voices; model providers may have their own usage, licensing and privacy terms.
- For a real unlimited-capacity claim, supply dedicated inference infrastructure and budget. Vercel Hobby and community Hugging Face GPUs do not supply it.
- Browser reading uses available system voices and doesn't export generated device audio. The Google TTS mode downloads generated WAV/MP3 when server requests succeed.

## References

- https://ai.google.dev/gemini-api/docs/speech-generation
- https://ai.google.dev/gemini-api/docs/rate-limits
- https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-3-8-text-to-speech/
- https://huggingface.co/docs/hub/spaces-zerogpu
