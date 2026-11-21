#!/usr/bin/env python3
"""Locally pack the official Pravega INT8 ONNX research model for the
third-party Pocket TTS Android Engine. Model weights are NOT included in
this Git repository or the APK. Only use with the model's research-only licence.

Prerequisite (desktop / Python, with the author's licence reviewed):
  hf download dheeyantra/pravega-pocket-tts --include "onnx/*" "LICENSE" "NOTICE" --local-dir ./pravega
  python3 offline-tts-android/tools/build_pravega_research_pack.py \
      --source ./pravega --out ./pravega-te-research.zip --accept-research-license

Import the resulting zip in Pocket TTS Android Engine, import a consented WAV
speaker sample in that engine and select that voice in its settings.
"""
from __future__ import annotations
import argparse
import json
import shutil
import tempfile
import zipfile
from pathlib import Path

FILES = {
    "text_conditioner.onnx": "text_conditioner.onnx",
    "mimi_encoder.onnx": "mimi_encoder.onnx",
    "tokenizer.model": "tokenizer.model",
    "flow_lm_main_int8.onnx": "flow_lm_main_int8.onnx",
    "flow_lm_flow_int8.onnx": "flow_lm_flow_int8.onnx",
    "mimi_decoder_int8.onnx": "mimi_decoder_int8.onnx",
}
MANIFEST = {
    "format": 1, "id": "pravega-te-int8-research",
    "name": "Pravega Telugu (Research Only)",
    "languageTag": "te-IN", "precision": "int8",
    "int8Profile": "all", "temperature": 0.3,
    "lsdSteps": 1, "threads": 4,
    "voices": [],
    "source": "https://huggingface.co/dheeyantra/pravega-pocket-tts",
}

def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", type=Path, required=True)
    p.add_argument("--out", type=Path, required=True)
    p.add_argument("--accept-research-license", action="store_true")
    args = p.parse_args()
    if not args.accept_research_license:
        p.error("Read the official Dhee Research-Only Licence v1.0 first; "
                "then explicitly pass --accept-research-license.")
    source = args.source.resolve()
    lic = source / "LICENSE"
    if not lic.is_file():
        p.error("Official LICENSE must accompany model files.")
    missing = [f for f in FILES if not (source / "onnx" / f).is_file()]
    if missing:
        p.error("Missing official files: " + ", ".join(missing))
    output = args.out.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="pravega-pack-") as tmp:
        root = Path(tmp)
        (root / "models").mkdir()
        (root / "voices").mkdir()
        for origin, dest in FILES.items():
            shutil.copy2(source / "onnx" / origin, root / "models" / dest)
        shutil.copy2(lic, root / "MODEL_LICENSE.txt")
        if (source / "NOTICE").is_file():
            shutil.copy2(source / "NOTICE", root / "NOTICE")
        (root / "manifest.json").write_text(
            json.dumps(MANIFEST, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
        with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED,
                             compresslevel=3, allowZip64=True) as archive:
            for f in sorted(root.rglob("*")):
                if f.is_file():
                    archive.write(f, f.relative_to(root).as_posix())
    print("Pack written for personal non-commercial research:", output)
    print("Next: Import a consented speaker WAV in Pocket TTS Android Engine.")
    print("A successful import and speech test are still required.")

if __name__ == "__main__":
    main()
