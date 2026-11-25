/* HMXHARNESS Project Expo — shared slide specification.
 * One source of truth rendered into PPTX (editable), PDF and PNG previews.
 * Units: inches on a 13.333 x 7.5 slide.
 */
const path = require('path');
const { createCanvas, GlobalFonts } = require('@napi-rs/canvas');

const ROOT = path.resolve(__dirname, '..');
const ASSETS = path.join(ROOT, 'assets');
const GENERATED = path.join(ASSETS, 'generated');
const SCREENS = path.join(ASSETS, 'screens');
const FONT_DIR = path.join(ASSETS, 'fonts');
GlobalFonts.registerFromPath(path.join(FONT_DIR, 'Inter-Regular.ttf'), 'Inter');
GlobalFonts.registerFromPath(path.join(FONT_DIR, 'SpaceGrotesk.ttf'), 'Space');

const C = {
  bg: '#080910', navy: '#111629', purple: '#8B5CF6', violet: '#A78BFA',
  cyan: '#38BDF8', white: '#F8FAFC', grey: '#A1A1AA', line: '#2A2F4A',
};
const SW = 13.333, SH = 7.5;
const measure = createCanvas(10, 10).getContext('2d');

function wrap(text, maxWidthPx, sizePt, family) {
  measure.font = `${sizePt * 1.333}px ${family}`;
  const out = [];
  for (const raw of String(text).split('\n')) {
    const words = raw.split(' ');
    let line = '';
    for (const w of words) {
      const test = line ? line + ' ' + w : w;
      if (measure.measureText(test).width <= maxWidthPx || !line) line = test;
      else { out.push(line); line = w; }
    }
    out.push(line);
  }
  return out;
}

const op = {
  rect: (x, y, w, h, o = {}) => ({ t: 'rect', x, y, w, h, ...o }),
  text: (x, y, w, text, size, o = {}) => {
    const family = o.font === 'space' ? 'Space' : 'Inter';
    const px = w * 96;
    const lines = wrap(text, px, size, family);
    return { t: 'text', x, y, w, size, family, color: o.color || C.white,
      lines, align: o.align || 'left', spacing: o.spacing || 1.22,
      italic: !!o.italic, tracking: o.tracking || 0, breakLines: !!o.breakLines };
  },
  line: (x1, y1, x2, y2, o = {}) => ({ t: 'line', x1, y1, x2, y2, ...o }),
  circle: (x, y, r, o = {}) => ({ t: 'circle', x, y, r, ...o }),
  image: (p, x, y, w, h, o = {}) => ({ t: 'image', path: p, x, y, w, h, ...o }),
};

function header(section, title, no) {
  const ops = [];
  ops.push(op.rect(0, 0, SW, SH, { fill: C.bg }));
  ops.push(op.rect(0.62, 0.48, 0.07, 0.28, { fill: C.cyan }));
  ops.push(op.text(0.82, 0.48, 8, section.toUpperCase(), 11, { font: 'space', color: C.cyan, tracking: 2 }));
  ops.push(op.text(0.62, 0.92, 11.6, title, 31, { font: 'space' }));
  ops.push(op.line(0.62, 1.62, 3.3, 1.62, { color: C.purple, width: 2.4 }));
  ops.push(op.line(0.62, 7.02, 12.71, 7.02, { color: C.line, width: 1 }));
  ops.push(op.text(0.62, 7.10, 5, 'HMXHARNESS  ·  PROJECT EXPO 2026', 9, { color: C.grey }));
  ops.push(op.text(11.2, 7.10, 1.5, `${String(no).padStart(2, '0')} / 10`, 9, { color: C.grey, align: 'right' }));
  return ops;
}

function glass(x, y, w, h, accent = C.purple) {
  return op.rect(x, y, w, h, { fill: C.navy, alpha: 0.82, radius: 0.14, stroke: accent, strokeAlpha: 0.55, sw: 1.2 });
}

function notesFor(i) { return NOTES[i - 1] || ''; }

const NOTES = [
`[0:00-1:00] Good morning. HMXHARNESS is a native Android application that runs an AI coding agent, chat, a real tiered shell and optional local models directly on a phone. There is no project-owned server. We will show the architecture, then a live demonstration where the agent builds an attendance website from a phone.`,
`[1:00-2:00] The problem is simple: most development tools assume a laptop. A student with only a phone cannot easily work on real files, run commands or use Git. Basic AI chat can answer questions but cannot independently edit a project. We asked whether a phone could be the development environment itself.`,
`[2:00-3:00] Our solution integrates the whole stack: an agent loop, tools, a file editor, terminal, browser preview, GitHub operations, configurable model providers and optional local GGUF inference. We are careful not to claim that every feature was independently verified on hardware; the architecture and code are real, and the demo will show what works.`,
`[3:00-4:00] This is the actual architecture from our source audit. The UI sends a message through ChatViewModel to RunManager, which starts AgentEngine. The engine asks a model for tool calls, executes them through the Tool Registry, and persists results through Session Repository into Room. There is no central cloud server: providers are endpoints the user configures.`,
`[4:00-5:00] The agent runs a loop: it builds context, asks the model, parses tool calls, executes tools, returns results, and repeats until finished. Our audit counted 82 tools registered at startup. Chat mode sees a fixed allowlist of 25 tools, and Plan mode restricts schemas to read-only tools.`,
`[5:00-7:00] This is the most important part: the developer-supplied screenshots of a Student Attendance Management website generated and previewed on a physical device. You can see the dashboard with five students, four present and one absent, the roster with working Present and Absent controls, and the add-student form. The agent banner says Completed, Open page. We label this as developer-supplied device evidence, not something the audit reproduced.`,
`[7:00-8:00] The technology stack is modern Android: Kotlin, Compose, Room, DataStore, Coroutines and OkHttp with server-sent events. Credentials use Android Keystore. Native C++ and CMake compile llama.cpp for local GGUF inference. CPU inference is the shipped path; GPU is experimental and opt-in. We do not train our own model and we do not claim every provider was live-tested.`,
`[8:00-9:00] GitHub integration uses Device Flow with only a public client ID, no client secret. The cloud build runs unit tests, debug lint, and assembles debug and release APKs, then uploads an artifact. We verified a successful run and a 110-megabyte artifact through the public GitHub API.`,
`[9:00-9:40] We want to be transparent. HMXHARNESS is an MIT derivative of AndroidHarness, and we keep that attribution. The HMX verification objects are test-only, not an enforced runtime gate. The release APK is debug-signed. Four high-severity security findings were identified. GPU is experimental, and instrumented tests have not been run by CI. Our roadmap addresses these directly.`,
`[9:40-10:00] To close: HMXHARNESS explores a future where capable AI-assisted development is accessible directly from an Android device. We showed a real architecture, a real cloud build and a real on-device demonstration. Thank you.`
];

const slides = [];

// 01 — Product introduction
{
  const o = [];
  o.push(op.image(path.join(GENERATED, 'hero_phone.png'), 0, 0, SW, SH, { mode: 'cover' }));
  o.push(op.rect(0, 0, 8.1, SH, { fill: C.bg, alpha: 0.80 }));
  o.push(op.rect(0.68, 1.28, 0.08, 0.62, { fill: C.cyan }));
  o.push(op.text(0.92, 1.22, 7.2, 'HMXHARNESS', 54, { font: 'space' }));
  o.push(op.text(0.92, 2.16, 7.0, 'CODE BEYOND LIMITS.', 27, { font: 'space', color: C.cyan, tracking: 2 }));
  o.push(op.text(0.92, 2.86, 6.6, 'AI-Powered Autonomous Coding Agent for Android', 17, { color: C.white }));
  o.push(glass(0.92, 3.62, 5.5, 0.82));
  o.push(op.text(1.12, 3.83, 5.1, 'com.hmx.harness   ·   Version 4.0   ·   Android 8.0+', 12.5, { color: C.grey }));
  o.push(op.text(0.92, 5.30, 6.4, '[ COLLEGE NAME ]', 12, { color: C.violet, font: 'space' }));
  o.push(op.text(0.92, 5.68, 6.4, '[ TEAM MEMBER 1 ]   [ TEAM MEMBER 2 ]', 12, { color: C.grey }));
  o.push(op.text(0.92, 6.04, 6.4, '[ TEAM MEMBER 3 ]   [ TEAM MEMBER 4 ]', 12, { color: C.grey }));
  o.push(op.text(0.92, 7.10, 8, 'github.com/HMXPANEL/HMXHARNESS', 10, { color: C.grey }));
  o.push(op.text(11.2, 7.10, 1.5, '01 / 10', 9, { color: C.grey, align: 'right' }));
  slides.push({ ops: o, notes: notesFor(1) });
}

// 02 — Problem
{
  const o = header('The problem', 'What if your phone could be your development environment?', 2);
  o.push(op.image(path.join(GENERATED, 'problem_split.png'), 0, 1.78, SW, 5.18, { mode: 'cover', alpha: 0.92 }));
  o.push(op.rect(0, 1.78, 7.6, 5.18, { fill: C.bg, alpha: 0.78 }));
  const items = [
    ['01', 'Development tools usually assume a laptop or desktop.'],
    ['02', 'Mobile-only developers still need files, terminal and Git.'],
    ['03', 'Chat assistants alone cannot act on a real project workspace.'],
  ];
  items.forEach(([n, t], i) => {
    const y = 2.10 + i * 1.28;
    o.push(glass(0.72, y, 6.55, 1.04, i === 0 ? C.cyan : C.purple));
    o.push(op.text(0.96, y + 0.27, 0.6, n, 22, { font: 'space', color: i === 0 ? C.cyan : C.violet }));
    o.push(op.text(1.66, y + 0.30, 5.35, t, 15.2, { color: C.white }));
  });
  o.push(op.text(0.74, 6.28, 6.6, 'The gap: capable AI tools stay on desktops; phones get only conversation.', 12.5, { color: C.grey }));
  slides.push({ ops: o, notes: notesFor(2) });
}

// 03 — Solution
{
  const o = header('Our solution', 'Meet HMXHARNESS', 3);
  o.push(op.image(path.join(GENERATED, 'solution_exploded.png'), 0, 1.78, SW, 5.18, { mode: 'cover', alpha: 0.95 }));
  o.push(op.rect(0, 1.78, 6.5, 5.18, { fill: C.bg, alpha: 0.80 }));
  o.push(op.text(0.76, 2.02, 5.7, 'An integrated AI coding-agent experience on Android — agent, tools and data on the device.', 16, { color: C.white }));
  const chips = [
    ['AI coding agent', C.purple], ['File editing', C.cyan], ['Terminal', C.violet],
    ['Browser preview', C.cyan], ['GitHub integration', C.purple],
    ['Configurable providers', C.violet], ['Optional local GGUF', C.cyan],
  ];
  chips.forEach(([t, col], i) => {
    const x = 0.76 + (i % 2) * 2.86, y = 2.98 + Math.floor(i / 2) * 0.74;
    o.push(op.rect(x, y, 2.62, 0.58, { fill: C.navy, alpha: 0.9, radius: 0.12, stroke: col, strokeAlpha: 0.7, sw: 1.2 }));
    o.push(op.text(x + 0.18, y + 0.17, 2.3, t, 11.5, { color: C.white }));
  });
  o.push(op.text(0.76, 6.20, 5.8, 'Feature availability depends on settings, device capabilities and permissions.', 11.5, { color: C.grey }));
  slides.push({ ops: o, notes: notesFor(3) });
}

// 04 — Architecture (editable shapes)
{
  const o = header('System architecture', 'How HMXHARNESS works', 4);
  const box = (x, y, w, h, label, sub, accent) => {
    o.push(glass(x, y, w, h, accent));
    o.push(op.text(x + 0.16, y + (sub ? 0.12 : 0.20), w - 0.3, label, 13.5, { font: 'space', color: C.white }));
    if (sub) o.push(op.text(x + 0.16, y + 0.50, w - 0.3, sub, 9.5, { color: C.grey }));
  };
  box(4.55, 1.78, 4.25, 0.62, 'AppContainer  ·  manual composition root (no Hilt/Dagger)', null, C.violet);
  box(0.72, 2.62, 2.55, 0.86, 'User Interface', 'Compose · AppNav', C.cyan);
  box(3.55, 2.62, 2.55, 0.86, 'ChatViewModel', 'send() · state', C.violet);
  box(6.38, 2.62, 2.55, 0.86, 'RunManager', 'startRun() · events', C.purple);
  box(9.2, 2.62, 3.25, 0.86, 'AgentEngine', 'run() · context · tools', C.cyan);
  [ [3.27, 3.05, 3.55, 3.05], [6.10, 3.05, 6.38, 3.05], [8.93, 3.05, 9.2, 3.05] ].forEach(([a,b,c,d]) =>
    o.push(op.line(a, b, c, d, { color: C.violet, width: 2 })));
  box(0.72, 3.94, 2.55, 0.86, 'UI updates', 'flows · cards', C.cyan);
  box(3.55, 3.94, 2.55, 0.86, 'ToolRegistry', '82 tools at startup', C.purple);
  box(6.38, 3.94, 2.55, 0.86, 'Tool execution', 'workspace · shell · git · browser · MCP', C.violet);
  box(9.2, 3.94, 3.25, 0.86, 'Model providers', '4 wire protocols · local GGUF', C.cyan);
  o.push(op.line(9.82, 3.48, 9.82, 3.94, { color: C.cyan, width: 2 }));
  o.push(op.line(6.1, 4.37, 6.38, 4.37, { color: C.violet, width: 2 }));
  box(3.55, 5.24, 5.2, 0.86, 'SessionRepository  →  Room (harness.db, v27)', null, C.purple);
  o.push(op.line(6.1, 4.80, 6.1, 5.24, { color: C.purple, width: 2 }));
  box(9.2, 5.24, 3.25, 0.86, 'Keystore · DataStore · Linux env', null, C.violet);
  box(0.72, 5.24, 2.55, 0.86, 'App data stays on device', null, C.cyan);
  o.push(op.text(0.74, 6.42, 11.6, 'No project-owned cloud backend: providers and GitHub are endpoints configured by the user.', 12, { color: C.grey }));
  slides.push({ ops: o, notes: notesFor(4) });
}

// 05 — Agent + tools
{
  const o = header('AI agent & tool ecosystem', 'From natural language to real development actions', 5);
  o.push(op.image(path.join(GENERATED, 'agent_brain.png'), 7.25, 1.78, 6.08, 5.18, { mode: 'cover', alpha: 0.96 }));
  const steps = [
    'User submits a task from the composer.',
    'Agent builds system prompt, environment, memory and repo map.',
    'The model replies with text or tool calls.',
    'Agent parses tool calls and checks permissions.',
    'Tools execute on files, shell, Git, browser or MCP.',
    'Results return as messages; the loop continues or finishes.',
  ];
  steps.forEach((s, i) => {
    const y = 1.92 + i * 0.72;
    o.push(op.circle(0.92, y + 0.17, 0.17, { fill: i === 5 ? C.cyan : C.purple, alpha: 0.95 }));
    o.push(op.text(0.72, y + 0.08, 0.34, String(i + 1), 11, { color: C.bg, align: 'center' }));
    o.push(op.text(1.28, y + 0.02, 5.65, s, 12.6, { color: C.white }));
  });
  o.push(op.rect(0.72, 6.30, 5.95, 0.48, { fill: C.navy, alpha: 0.9, radius: 0.1, stroke: C.cyan, strokeAlpha: 0.6 }));
  o.push(op.text(0.92, 6.42, 5.5, '82 tools at startup  ·  25-tool Chat allowlist  ·  Plan mode is read-only', 10.6, { color: C.cyan }));
  slides.push({ ops: o, notes: notesFor(5) });
}

// 06 — Live demo
{
  const o = header('Live application demonstration', 'Building a real application from a phone', 6);
  const flow = ['AI task', 'Website generation', 'Website preview', 'Attendance dashboard'];
  flow.forEach((t, i) => {
    const x = 0.76 + i * 3.10;
    o.push(op.rect(x, 1.82, 2.72, 0.46, { fill: C.navy, alpha: 0.9, radius: 0.1, stroke: i === 3 ? C.cyan : C.purple, strokeAlpha: 0.7 }));
    o.push(op.text(x + 0.12, 1.94, 2.5, t, 11.4, { color: i === 3 ? C.cyan : C.white, align: 'center' }));
    if (i < 3) o.push(op.line(x + 2.72, 2.05, x + 3.10, 2.05, { color: C.violet, width: 1.6 }));
  });
  const files = ['31-05', '33-92', '35-72'];
  files.forEach((f, i) => {
    const x = 0.92 + i * 3.18;
    const p = path.join(SCREENS, `Screenshot_2026-11-24-14-02-${f}_28c2859092379cd2f7b967065d899178.jpg`);
    o.push(op.rect(x - 0.07, 2.48, 2.92, 3.36, { fill: C.navy, radius: 0.16, stroke: i === 1 ? C.cyan : C.purple, strokeAlpha: 0.6, sw: 1.4 }));
    o.push(op.image(p, x, 2.56, 2.78, 3.20, { mode: 'cover', radius: 0.12 }));
  });
  ['CHAT', 'CODE EDITOR', 'TERMINAL', 'BROWSER PREVIEW'].forEach((label, i) => {
    const x = 0.92 + i * 3.18;
    o.push(op.rect(x, 5.98, 2.78, 0.48, { fill: C.navy, alpha: 0.5, radius: 0.09, stroke: C.line, strokeAlpha: 0.9, sw: 1 }));
    o.push(op.text(x + 0.12, 6.13, 2.5, `+  ${label} SCREENSHOT`, 9.6, { color: C.grey, align: 'center' }));
  });
  o.push(op.text(0.92, 6.60, 11.4, 'Developer-supplied physical-device demonstration — not reproduced by the audit.', 10.5, { color: C.grey }));
  slides.push({ ops: o, notes: notesFor(6) });
}

// 07 — Tech stack
{
  const o = header('Technology stack & AI models', 'Powered by a modern Android technology stack', 7);
  const cols = [
    { x: 0.72, title: 'LANGUAGE & BUILD', col: C.cyan, items: ['Kotlin 2.4.10', 'Gradle 9.7.0 · AGP 9.3.1', 'KSP 2.3.11', 'NDK 27.2.12479018', 'CMake 3.22.1'] },
    { x: 4.86, title: 'APP & DATA', col: C.purple, items: ['Jetpack Compose · Material 3', 'Room · DataStore', 'Coroutines · StateFlow', 'AndroidX Security · Keystore', 'Biometric · WorkManager'] },
    { x: 9.0, title: 'NETWORK & NATIVE', col: C.violet, items: ['OkHttp · SSE streaming', 'Native C++17 · llama.cpp', 'CPU GGUF inference', 'Shizuku · libsu · Dhizuku', 'GitHub Actions'] },
  ];
  cols.forEach(c => {
    o.push(glass(c.x, 1.88, 3.62, 2.72, c.col));
    o.push(op.text(c.x + 0.2, 2.06, 3.2, c.title, 12.5, { font: 'space', color: c.col }));
    c.items.forEach((it, i) => o.push(op.text(c.x + 0.22, 2.50 + i * 0.40, 3.2, '·  ' + it, 11.6, { color: C.white })));
  });
  o.push(glass(0.72, 4.86, 11.9, 1.48, C.cyan));
  o.push(op.text(0.98, 5.04, 11.4, 'AI PROVIDERS', 12.5, { font: 'space', color: C.cyan }));
  o.push(op.text(0.98, 5.40, 11.3, 'OpenAI-compatible  ·  OpenAI Responses  ·  Anthropic Messages  ·  Gemini  ·  ChatGPT account  ·  third-party relay', 12.4, { color: C.white }));
  o.push(op.text(0.98, 5.86, 11.2, 'CPU GGUF is shipped; GPU inference is experimental and opt-in. No model training code; no claim of live testing for every provider.', 11.2, { color: C.grey }));
  slides.push({ ops: o, notes: notesFor(7) });
}

// 08 — GitHub / CI
{
  const o = header('GitHub & cloud build', 'Develop, test and build with cloud infrastructure', 8);
  o.push(op.image(path.join(GENERATED, 'cloud_phone.png'), 8.35, 1.80, 4.98, 5.15, { mode: 'cover', alpha: 0.94 }));
  const lines = [
    ['GitHub Device Flow', 'Public client ID only — no client secret'],
    ['Repository operations', 'Reviewed commits · plain push · no force-push'],
    ['GitHub Actions', 'Unit tests · debug lint · debug + release APK'],
    ['Artifact upload', 'Verified, non-empty APK bundle'],
  ];
  lines.forEach(([a, b], i) => {
    const y = 1.92 + i * 0.92;
    o.push(glass(0.72, y, 7.2, 0.76, i % 2 ? C.violet : C.purple));
    o.push(op.text(0.94, y + 0.13, 2.55, a, 13, { font: 'space', color: i % 2 ? C.violet : C.purple }));
    o.push(op.text(3.48, y + 0.16, 4.2, b, 11.2, { color: C.white }));
  });
  o.push(op.rect(0.72, 5.86, 7.2, 0.94, { fill: C.navy, alpha: 0.95, radius: 0.14, stroke: C.cyan, strokeAlpha: 0.8, sw: 1.5 }));
  o.push(op.text(0.94, 6.02, 6.8, 'VERIFIED BUILD', 11, { font: 'space', color: C.cyan }));
  o.push(op.text(0.94, 6.32, 6.8, 'Run 45338701934  ·  Artifact 14898111209  ·  110,461,001 bytes', 11.4, { color: C.white }));
  slides.push({ ops: o, notes: notesFor(8) });
}

// 09 — Innovation / security / roadmap
{
  const o = header('Innovation, security & future scope', 'Built for the next generation of mobile development', 9);
  o.push(op.image(path.join(GENERATED, 'roadmap.png'), 0, 3.92, SW, 3.05, { mode: 'cover', alpha: 0.55 }));
  o.push(glass(0.72, 1.86, 5.8, 2.86, C.cyan));
  o.push(op.text(0.96, 2.04, 5.3, 'WHAT MAKES IT INTERESTING', 12, { font: 'space', color: C.cyan }));
  o.push(op.text(0.96, 2.42, 5.3, 'Native Android agent · tiered shell · local GGUF · browser automation · MCP · subagents · memory · cloud builds · adaptive glass.', 12, { color: C.white }));
  o.push(op.text(0.96, 3.38, 5.3, 'HMX additions: identity, CI, hmx/ objects, Connected Accounts card, glass tiers, documentation. Engine is inherited upstream.', 11, { color: C.grey }));
  o.push(glass(6.82, 1.86, 5.8, 2.86, C.purple));
  o.push(op.text(7.06, 2.04, 5.3, 'LIMITATIONS WE DISCLOSE', 12, { font: 'space', color: C.violet }));
  o.push(op.text(7.06, 2.42, 5.3, 'HMX gate is test/display-only · release is debug-signed · 4 high-severity security findings · GPU experimental · instrumented tests not run by CI.', 12, { color: C.white }));
  o.push(op.text(7.06, 3.38, 5.3, 'Upstream MIT attribution to AndroidHarness / Sanuu7 is preserved in LICENSE and README.', 11, { color: C.grey }));
  o.push(op.rect(0.72, 5.06, 11.9, 1.56, { fill: C.navy, alpha: 0.9, radius: 0.14, stroke: C.violet, strokeAlpha: 0.6 }));
  o.push(op.text(0.98, 5.24, 11.2, 'ROADMAP', 11.5, { font: 'space', color: C.violet }));
  o.push(op.text(0.98, 5.60, 11.2, 'Security remediation  →  runtime verification enforcement  →  process recovery  →  release signing  →  instrumented testing  →  visual validation', 12.2, { color: C.white }));
  slides.push({ ops: o, notes: notesFor(9) });
}

// 10 — Conclusion
{
  const o = [];
  o.push(op.image(path.join(GENERATED, 'closing.png'), 0, 0, SW, SH, { mode: 'cover' }));
  o.push(op.rect(0, 0, SW, SH, { fill: C.bg, alpha: 0.72 }));
  o.push(op.rect(0.9, 1.5, 0.08, 0.72, { fill: C.cyan }));
  o.push(op.text(1.16, 1.42, 10.6, 'CODE BEYOND LIMITS.', 52, { font: 'space', color: C.white }));
  o.push(op.text(1.16, 2.60, 9.8, 'HMXHARNESS explores a future where powerful AI-assisted software development is accessible directly from an Android device.', 18, { color: C.white }));
  o.push(glass(1.16, 3.66, 7.4, 0.86, C.purple));
  o.push(op.text(1.40, 3.92, 6.9, 'github.com/HMXPANEL/HMXHARNESS', 15, { color: C.cyan }));
  o.push(op.text(1.16, 5.06, 7.4, '[ COLLEGE NAME ]   ·   [ TEAM MEMBERS ]', 13, { color: C.grey }));
  o.push(op.text(1.16, 5.72, 7.4, 'Thank you.', 24, { font: 'space', color: C.violet }));
  o.push(op.text(1.16, 7.10, 8, 'HMXHARNESS  ·  MIT derivative of AndroidHarness  ·  Project Expo 2026', 10, { color: C.grey }));
  o.push(op.text(11.2, 7.10, 1.5, '10 / 10', 9, { color: C.grey, align: 'right' }));
  slides.push({ ops: o, notes: notesFor(10) });
}

module.exports = { slides, C, SW, SH, ROOT, ASSETS, GENERATED, SCREENS, op };
