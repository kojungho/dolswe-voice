# Third-party notices

- sherpa-onnx 1.13.8: https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8 (Apache-2.0; see licenses/sherpa-onnx-LICENSE.txt).
- SenseVoice model: https://github.com/k2-fsa/sherpa-onnx/releases/tag/asr-models, archive `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17.tar.bz2`. The archive's original model notice is in licenses/model-NOTICE.txt; it refers to FunASR licensing. Code license and model terms are separate; preserve the original notice when redistributing weights. Upstream: https://github.com/FunAudioLLM/SenseVoice and https://github.com/modelscope/FunASR#license.
- Silero VAD: https://github.com/snakers4/silero-vad (see licenses/silero-LICENSE.txt).
- Kotlin standard library 2.2.20: https://kotlinlang.org/ (see licenses/kotlin-LICENSE.txt).

The release APK contains model and runtime assets. The source ZIP does not contain these large third-party binaries; prepare-deps.py downloads the pinned versions and verifies their checksums.

- Leaflet 1.9.4: https://leafletjs.com/ (BSD-2-Clause; see licenses/Leaflet-LICENSE.txt). Map tiles © OpenStreetMap contributors, https://www.openstreetmap.org/copyright. Tiles are requested only while viewing the map; no bulk or offline download.
