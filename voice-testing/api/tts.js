// Google Telugu narration adapter. Runs as a Node.js serverless function.
// The Google key must be a SERVER-SIDE environment variable, never in HTML/GitHub.
// This endpoint is for prebuilt TTS voices, NOT voice cloning.
const MAX_CHARS = 600;
const ALLOWED_VOICES = new Set(["Kore","Puck","Aoede","Charon","Leda","Orus"]);
const MODEL = "gemini-2.5-flash-preview-tts";

function makeWav(raw) {
  const wav = Buffer.alloc(44 + raw.length);
  wav.write("RIFF", 0);
  wav.writeUInt32LE(36 + raw.length, 4);
  wav.write("WAVEfmt ", 8);
  wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(24000, 24);
  wav.writeUInt32LE(48000, 28);
  wav.writeUInt16LE(2, 32);
  wav.writeUInt16LE(16, 34);
  wav.write("data", 36);
  wav.writeUInt32LE(raw.length, 40);
  raw.copy(wav, 44);
  return wav;
}
module.exports = async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");
  res.setHeader("X-Content-Type-Options", "nosniff");
  if (req.method === "GET") {
    res.setHeader("Content-Type", "application/json; charset=utf-8");
    return res.status(200).json({ configured: !!process.env.GEMINI_API_KEY, model: MODEL,
      note: "This checks configuration only, not generation. Telugu prebuilt TTS; no voice cloning." });
  }
  if (req.method !== "POST") return res.status(405).json({error:"Method not allowed"});
  if (!process.env.GEMINI_API_KEY) return res.status(503).json({error:"Google TTS backend is not configured by the site owner."});
  // Fail closed when an origin is present and differs from this deployment host.
  const origin = req.headers.origin;
  if (origin) {
    try {
      const host = new URL(origin).host;
      if (host !== req.headers.host) return res.status(403).json({error:"Cross-site use is disabled."});
    } catch { return res.status(403).json({error:"Invalid origin."}); }
  }
  let body = req.body;
  try { if (typeof body === "string") body = JSON.parse(body); }
  catch { return res.status(400).json({error:"Invalid JSON."}); }
  const t = typeof body?.text === "string" ? body.text.trim() : "";
  const voice = ALLOWED_VOICES.has(body?.voice) ? body.voice : "Kore";
  if (!t || t.length > MAX_CHARS) return res.status(400).json({error:"Send 1 to 600 characters per request."});
  if (!/[\u0C00-\u0C7F]/u.test(t)) return res.status(400).json({error:"Enter Telugu script for this test endpoint."});
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 50000);
    let response;
    try {
      response = await fetch("https://generativelanguage.googleapis.com/v1beta/models/"+MODEL+":generateContent",{
        method:"POST", signal:controller.signal,
        headers:{"Content-Type":"application/json", "x-goog-api-key":process.env.GEMINI_API_KEY},
        body:JSON.stringify({
          contents:[{parts:[{text:"Read the following Telugu text faithfully and naturally. Do not add any extra words:\n"+t}]}],
          generationConfig:{responseModalities:["AUDIO"], speechConfig:{
            voiceConfig:{prebuiltVoiceConfig:{voiceName:voice}}
          }}
        })
      });
    } finally { clearTimeout(timer); }
    if (!response.ok) {
      // Do not reflect Google error bodies or credentials to public visitors.
      const status = response.status === 429 ? 429 : 502;
      return res.status(status).json({error: response.status===429 ?
        "Google free-tier quota has been reached. Try later." :
        "Google speech service is unavailable ("+response.status+")."});
    }
    const json = await response.json();
    const part = json?.candidates?.[0]?.content?.parts?.find(p => p.inlineData?.data || p.inline_data?.data);
    const inline = part?.inlineData || part?.inline_data;
    if (!inline?.data) return res.status(502).json({error:"Google did not return audio."});
    const bytes = Buffer.from(inline.data,"base64");
    if (bytes.length < 2 || bytes.length > 20*1024*1024) return res.status(502).json({error:"Unexpected audio size."});
    const mime = (inline.mimeType || inline.mime_type || "").toLowerCase();
    const wav = mime.includes("wav") || bytes.subarray(0,4).toString("ascii")==="RIFF" ? bytes : makeWav(bytes);
    res.setHeader("Content-Type","audio/wav");
    res.setHeader("Content-Length",wav.length);
    return res.status(200).end(wav);
  } catch (err) {
    return res.status(504).json({error:"Google TTS request failed or timed out. Please retry later."});
  }
};