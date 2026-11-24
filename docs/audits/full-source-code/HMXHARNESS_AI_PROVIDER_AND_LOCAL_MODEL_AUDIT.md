# HMXHARNESS — AI Provider and Local Model Audit

**Audit branch:** `audit/hmxharness-full-source-audit`
**Method:** static source inspection only; no files modified, no tests run.

## 1. Verified findings

| Claim | Verdict | Evidence |
| --- | --- | --- |
| Main OkHttp client: connect 30s, write 60s, read 0 (SSE), no `callTimeout` | Confirmed | `llm/Llm.kt:211-219` |
| SSE via `enqueue` + raw `BufferedSource`, `data:` parsing, `[DONE]`, cancel on collector close | Confirmed | `Llm.kt:245-305`, `readSseLine:318-329` |
| Provider fallback runtime-wired | Confirmed | `HarnessApp.kt:379-403,405-406`; consent `RunManager.kt:623-625,1617-1619`, `ChatViewModel.kt:2298-2300` |
| Key rotation runtime-wired | Confirmed | `HarnessApp.kt:189`; `ProviderRepository.kt:287-294`; `KeyStoreManager.kt:59-90` |
| Built-in "Harness" provider is keyless and **not HMX-hosted** | Confirmed | `HarnessProvider.kt:23-29,43-46` |
| Local CPU GGUF on 64-bit ABIs; GPU gated by `localModelExperimental` AND `LocalAccelerationPolicy.useGpu` | Confirmed | `SettingsRepository.kt:234` (default false), `LocalModelCatalog.kt:79`, `LocalAccelerationPolicy.kt:11-12`, `LocalModelManager.kt:499-501,689-692` |
| 32-bit builds use a 1-line stub, no llama.cpp fetched | Confirmed | `CMakeLists.txt:21-36` (`return()` at 35), `local_unavailable.cpp:1`, `gpu_unavailable.cpp:1-3` |
| llama.cpp pinned to `19cce691…` with SHA256; Vulkan/SPIRV headers pinned | Confirmed | `CMakeLists.txt:84-90,64-83` |
| Model downloads verify size + GGUF magic + SHA-256 before rename, stored in `noBackupFilesDir/local-models` | Confirmed | `LocalModelManager.kt:84`, `LocalModelStore.kt:88,131,145-173` |
| No model training code exists | Confirmed | source-wide search negative; `safetensors_import.cpp:65-291` is format conversion only |

## 2. HTTP and SSE layer — `llm/Llm.kt`

`ProviderType` four wire types with default bases `Llm.kt:24-30` (OpenAI compat/Responses
`https://api.openai.com/v1`, Anthropic `https://api.anthropic.com`, Gemini
`https://generativelanguage.googleapis.com/v1beta`); endpoint suffixes `:41-48`;
`ProviderConfig` `:50-63`; `RequestOptions` `:79-120`; `StreamEvent` sealed interface
`:122-166`; `ApiException` `:168-176`.

OkHttp client `:211-219`: `connectTimeout(30s)` `:216`, `writeTimeout(60s)` `:217`,
`readTimeout(0)` `:218`. **`callTimeout` is never set** (OkHttp default 0 = no whole-call
limit); stall protection is agent-layer `stallGuard` (`FallbackProvider.kt:123`) and
`StreamRetrier`. Other clients have tighter bounds: Harness probe 30/60/90s
(`HarnessProvider.kt:108-118`), key check 15/15/45s (`ProviderKeyCheck.kt:40-44`),
ChatGPT `callTimeout(10, MINUTES)` (`chatgpt/ChatGptProvider.kt:17-18`).

SSE `sseJson()` `:245-305`: `call.enqueue` bridged via `suspendCancellableCoroutine`
`:251-262`; non-2xx reads up to 2,000 chars and throws `ApiException` with `Retry-After`
`:264-267`; `readSseLine()` `:318-329` strips only one preceding `\r` preserving interior
CR bytes; malformed JSON skipped `:286-288`; `awaitClose { call.cancel(); reader.cancel() }`
`:301-304`. Factory dispatch `create(config)` `:224-231` (ChatGPT, local, Harness,
RotatingKeyProvider-wrapped) and `create(type)` `:233-238` (four wire providers).

## 3. Provider reference

| Provider | Endpoint | Key supply | Streaming / tools | Tests |
| --- | --- | --- | --- | --- |
| `OpenAiCompatProvider` | `{base}/chat/completions` `:448` | `Authorization: Bearer` `:452`, omitted for blank/`LOCAL_KEY` `:449-453`; OpenRouter headers `:458-461`; custom headers `:462` | SSE `sseJson` `:86`, `parseChunk` `:254-414`, id-keyed tool accumulation `:354-402`, leftover flush `:114-118`; `max_tokens` vs `max_completion_tokens` `:131-136,590-595`; local-host detection `:580-588` | `OpenAiCompatStreamTest` (233), `OpenAiCompatToolCallTest` (122), parsing/hoist/history tests, `LiveToolStreamTest`, `SseStreamTest`, `SseLineReaderTest` |
| `OpenAiResponsesProvider` | `{base}/responses` `:96` | Bearer `:97`; OpenCode session `:98-100`; custom `:101` | Responses events `:237-332`, flattened function tools `:133-145`, `max_output_tokens` `:116`, `store:false` `:115`, reasoning `:123-129` | `OpenAiResponsesProviderTest` (179) |
| `AnthropicProvider` | `{base}/v1/messages` `:458` | `x-api-key` `:459`, `anthropic-version: 2023-06-01` `:460` | SSE events `:121-265`, tool_use/tool_result `:186-233`, 4 prompt-cache breakpoints `:87-107,271-331`, usage normalization `:134-150` | `AnthropicCacheBreakpointsTest`, `AnthropicUsageTest`, `CacheReportingTest` |
| `GeminiProvider` | `{base}/models/{model}:streamGenerateContent?alt=sse` `:43-44` | `x-goog-api-key` `:47` | inline parse `:86-122`, `parametersJsonSchema` `:146-161`, replay `:179-274`, thinkingConfig `:163-173` | `GeminiToolSchemaTest` (109); no dedicated device test |
| `HarnessProvider` (keyless) | `opencode.ai/zen/v1` `:24`, `api.kilo.ai/api/gateway` `:25`, Pollinations `:44` | sentinel `KEYLESS` `:104-106`; `withSession` `:171-177`; `anonymous()` strips auth `:179-187` | delegates to wire providers `:213-258`; probes wire `:95-99,127-137`; pooled models `:222-237` | `HarnessProviderTest` (141), `HarnessCatalogTest` (115), `HarnessFreeLimitTest`; device `LiveKiloWorkflowDeviceTest` (208) |
| `FallbackProvider`/`FallbackPolicy` | wraps all providers | per-attempt fresh credentials `:159-184` | classification `:20-32`, stall guard 600s local `:113`, retries `MAX_TARGETS=4` `:23`, default disabled `:13`, retries=3 `:15`; emitted `ProviderChanged` `:183` | `FallbackProviderTest` (202); device `FallbackRunDeviceTest` (102) |
| `RotatingKeyProvider` | wraps editable providers only `:83-85` | pools via `HarnessApp.kt:189`, encrypted keys | atomic cursor `:30`, rotate on 401/402/403/429 `:62-63`, redacts keys `:65-68` | `RotatingKeyProviderTest` (97) |
| `ProviderKeyCheck` | per-type smallest request | 15/15/45s client `:40-44` | `check()` `:47-72`, URL/auth builders `:74-122` | `ProviderKeyCheckTest` (97) |
| `chatgpt/` | OAuth `auth.openai.com` `:24`, resource `api.openai.com/v1` `:25` | PKCE S256 `:40-58`, RS256/JWKS ID token validation `:76-109`, loopback callback `:177-212` | Responses via `sseJson` `:79`, tools namespaced `:50-56`, one 401 retry `:151` | `ChatGptProtocolTest` (221), `ChatGptProviderTest` (188), device `ChatGptDeviceTest` (331) |
| `ModelCatalog`/`ModelPrices`/`ModelsDev` | `models.dev/api.json` `:41` | — | capability heuristics `:44-71`, list `:89-117`, pricing `:58-105`, daily cache `ModelsDev.kt:42-44` | `ModelCatalogTest` (128), `ModelsDevTest` (237), price tests |
| 9Router | `127.0.0.1:20128/v1` `NineRouterManager.kt:132-135,254-256` | integration key `:211` | npm `9router@0.5.112` `:253`, installed `filesDir/9router` `:40-42` | `NineRouterReleaseTest`, `NineRouterNodeRuntimeTest`, device runtime tests |

## 4. Local inference — CPU vs GPU

- **Gating:** `localModelExperimental` default `false` (`SettingsRepository.kt:234,596`; reset in backup `:80,88`); `LocalBackend {AUTO,CPU,GPU}` (`LocalAccelerationPolicy.kt:6`), default `AUTO` (`LocalModelCatalog.kt:79`); `useGpu(experimental, preference, available) = experimental && preference != CPU && available` (`:11-12`); call sites pass `available = experimental && backend != CPU && gpuSupport.device().isNotBlank()` (`LocalModelManager.kt:500-501,691-692`). GPU load/generate fall back to CPU before output (`:507-516,730-737`).
- **JNI/32-bit:** `LocalNative.kt:5-10` loads `harness_local`, `libggml-cpu.so` on 64-bit only; 32-bit `CMakeLists.txt:21-36` builds only stubs with `return()` at `:35`; `local_unavailable.cpp:1` has no JNI symbols so calls throw `UnsatisfiedLinkError`, caught at `LocalModelProvider.kt:46-48`. `gpu_unavailable.cpp:1-3` exports `ah_gpu_runtime_available()=0`.
- **Native CPU/GPU:** `local_model.cpp:60` backend init, `:73-74` load, `n_gpu_layers=0` `:68`, greedy sampler `:127`; `local_chat.cpp:62-74` Vulkan device selection with `n_gpu_layers=999` when GPU, `:381-403` tool-capable sampler, `:453-459` GPU enumeration.
- **CMake pinning:** `LLAMA_REVISION 19cce6919992fdffd92e0ac152caaf1e2b8f254e` `:84`; `FetchContent_Declare` with SHA256 `:85-90`; Vulkan headers 1.4.363 `:65-67`, SPIRV-Headers `:68-70`; `GGML_VULKAN ON` `:82`, `GGML_BACKEND_DL ON` `:57`, KleidiAI on arm64 `:51-55`.
- **Downloads/storage:** root `noBackupFilesDir/local-models` (`LocalModelManager.kt:84`); `.gguf`/`mmproj`/`.part` naming (`LocalModelStore.kt:22-26`); size check `:163`, HTTP Content-Length `:247`, SHA-256 `:88,91-108`, GGUF magic `:165-166`, pinned hash compare `:168`, atomic rename `:170`; 2 MB header probe (`GgufMetadata.kt:8`), split GGUF rejected `:58`. Pinned public HF models (`LocalModelRecommendations.kt`), GPU runtime from `Sanuu7/AH-GPU-Runtime` (`GpuDownloadWorker.kt:65`) with size+checksum `:50-56`.
- **No training:** source-wide search for training/finetune/optimizer APIs returned no matches; `safetensors_import.cpp` only reads/converts/quantizes (`:174-291`).

## 5. Coverage and gaps

Extensive JVM tests for SSE, all four wire protocols, ChatGPT, fallback, rotation, catalog/pricing and local policy; instrumented tests for keyless live workflow, fallback, ChatGPT, local GGUF CPU/GPU and 9Router. **No dedicated tests** for `ProviderFactory.create` dispatch itself, the exact OkHttp timeout values, numeric price table, `ProvidersScreen` Compose screen, `GpuDownloadWorker`/`GpuDeliveryFactory`, or native C++ conversion (device tests only). Instrumented tests exist but are not run by CI.

## 6. Caveats

Keyless "Harness" is a relay over third-party free endpoints, not HMX infrastructure. GPU runtime and model weights come from third-party hosts (pinned by revision/checksum). All "works" claims here mean source implementation plus test files; no live provider call or GGUF inference was executed in this audit.
