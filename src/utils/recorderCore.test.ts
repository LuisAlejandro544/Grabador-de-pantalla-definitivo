import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateEstimatedFileSize,
  formatBytes,
  formatDuration,
  validateResolution,
  calculateEngineMetrics,
  RESOLUTION_PRESETS,
  validateAudioSource,
  mixPcm16Samples,
  getFfmpegRepairCommand,
  getFfmpegLosslessMergeCommand,
  getGalleryPublicAlbumPath,
  detectVideoContainer,
  COUNTDOWN_PRESETS,
  validateCountdownSeconds,
  generateNativeResolutionPresets,
  calculateAspectRatioLabel,
  createCustomResolutionPreset,
  STANDARD_POPULAR_PRESETS,
  serializeUserPreferences,
  parseUserPreferences,
  DEFAULT_USER_PREFERENCES,
  validateAppPackageName,
  getShaderPipelineCapabilities,
  getGoStreamingEngineSpecs
} from './recorderCore';

test('calculateEstimatedFileSize calculates byte size accurately', () => {
  // 10 seconds at 8 Mbps video + 192 kbps audio
  // Total bitrate = 8,000,000 + 192,000 = 8,192,000 bps = 1,024,000 bytes/sec
  // 10 sec = 10,240,000 bytes
  const bytes = calculateEstimatedFileSize(10, 8, 192);
  assert.equal(bytes, 10240000);

  // 0 duration returns 0
  assert.equal(calculateEstimatedFileSize(0, 10), 0);
  // Negative duration returns 0
  assert.equal(calculateEstimatedFileSize(-5, 10), 0);
});

test('formatBytes formats human-readable storage units', () => {
  assert.equal(formatBytes(0), '0 B');
  assert.equal(formatBytes(512), '512.0 B');
  assert.equal(formatBytes(1024), '1.0 KB');
  assert.equal(formatBytes(1048576), '1.0 MB');
  assert.equal(formatBytes(1073741824), '1.0 GB');
});

test('formatDuration formats seconds to standard time code', () => {
  assert.equal(formatDuration(0), '00:00');
  assert.equal(formatDuration(45), '00:45');
  assert.equal(formatDuration(75), '01:15');
  assert.equal(formatDuration(3665), '01:01:05');
  assert.equal(formatDuration(-10), '00:00');
});

test('validateResolution enforces codec constraints', () => {
  // Valid 1080x1920
  const validResult = validateResolution(1080, 1920);
  assert.equal(validResult.valid, true);

  // Odd number failure (must be even for YUV420p)
  const oddResult = validateResolution(1081, 1920);
  assert.equal(oddResult.valid, false);
  assert.match(oddResult.reason ?? '', /pares/i);

  // Too small
  const smallResult = validateResolution(120, 120);
  assert.equal(smallResult.valid, false);

  // Too large
  const largeResult = validateResolution(5000, 5000);
  assert.equal(largeResult.valid, false);
});

test('calculateEngineMetrics returns realistic metrics for C++23 and Rust', () => {
  const cppMetrics = calculateEngineMetrics('cpp23', 60, 12, 10);
  assert.ok(cppMetrics.cpuUsagePercent >= 5 && cppMetrics.cpuUsagePercent <= 30);
  assert.ok(cppMetrics.frameEncodingLatencyMs > 0);
  assert.ok(cppMetrics.totalFrames >= 590 && cppMetrics.totalFrames <= 610);
  assert.ok(cppMetrics.zeroCopyEfficiency >= 99);

  const rustMetrics = calculateEngineMetrics('rust2024', 60, 12, 10);
  assert.ok(rustMetrics.cpuUsagePercent >= 5 && rustMetrics.cpuUsagePercent <= 30);
  assert.ok(rustMetrics.frameEncodingLatencyMs > 0);
  assert.ok(rustMetrics.totalFrames >= 590 && rustMetrics.totalFrames <= 610);
  assert.ok(rustMetrics.zeroCopyEfficiency >= 99);
});

test('RESOLUTION_PRESETS contains standard mobile resolutions', () => {
  assert.ok(RESOLUTION_PRESETS.length >= 4);
  const fhd = RESOLUTION_PRESETS.find(p => p.id === '1080p');
  assert.ok(fhd !== undefined);
  assert.equal(fhd?.width, 1080);
  assert.equal(fhd?.height, 1920);
});

test('validateAudioSource correctly identifies audio recording modes', () => {
  const mic = validateAudioSource('mic');
  assert.equal(mic.valid, true);
  assert.equal(mic.mode, 'mic');

  const internal = validateAudioSource('internal');
  assert.equal(internal.valid, true);
  assert.equal(internal.mode, 'internal');

  const both = validateAudioSource('both');
  assert.equal(both.valid, true);
  assert.equal(both.mode, 'both');

  const mute = validateAudioSource('mute');
  assert.equal(mute.valid, true);
  assert.equal(mute.mode, 'mute');

  const invalid = validateAudioSource('invalid_mode');
  assert.equal(invalid.valid, false);
  assert.equal(invalid.mode, 'both'); // fallback seguro
});

test('mixPcm16Samples applies saturation clamping without digital clipping', () => {
  // Suma normal dentro de rango
  assert.equal(mixPcm16Samples(1000, 2000), 3000);
  assert.equal(mixPcm16Samples(-500, -1500), -2000);

  // Saturación superior (clamping a +32767)
  assert.equal(mixPcm16Samples(30000, 10000), 32767);
  assert.equal(mixPcm16Samples(32767, 10), 32767);

  // Saturación inferior (clamping a -32768)
  assert.equal(mixPcm16Samples(-30000, -10000), -32768);
  assert.equal(mixPcm16Samples(-32768, -5), -32768);
});

test('getFfmpegRepairCommand generates valid ignore_err copy command', () => {
  const cmd = getFfmpegRepairCommand('/input/corrupt.mp4', '/output/rescued.mp4');
  assert.match(cmd, /-err_detect ignore_err/);
  assert.match(cmd, /-c copy/);
  assert.match(cmd, /\+faststart/);
  assert.match(cmd, /\/output\/rescued\.mp4/);
});

test('getFfmpegLosslessMergeCommand configures faststart for mp4 and clean for mkv', () => {
  const mp4Cmd = getFfmpegLosslessMergeCommand('v.mp4', 'a.m4a', 'out.mp4', false);
  assert.match(mp4Cmd, /-movflags \+faststart/);
  assert.match(mp4Cmd, /-c:v copy/);

  const mkvCmd = getFfmpegLosslessMergeCommand('v.mp4', 'a.m4a', 'out.mkv', true);
  assert.doesNotMatch(mkvCmd, /-movflags \+faststart/);
  assert.match(mkvCmd, /out\.mkv/);
});

test('getGalleryPublicAlbumPath formats public directory path', () => {
  const path = getGalleryPublicAlbumPath('VeloRec_12345.mp4');
  assert.equal(path, 'Movies/VeloRec/VeloRec_12345.mp4');

  const sanitized = getGalleryPublicAlbumPath('video space@#$.mp4');
  assert.equal(sanitized, 'Movies/VeloRec/video_space___.mp4');
});

test('COUNTDOWN_PRESETS contains standard preparation delays', () => {
  assert.deepEqual(Array.from(COUNTDOWN_PRESETS), [0, 3, 5, 10]);
});

test('validateCountdownSeconds validates delays and provides safe fallback', () => {
  // 0s = instant
  const zero = validateCountdownSeconds(0);
  assert.equal(zero.valid, true);
  assert.equal(zero.seconds, 0);
  assert.match(zero.label, /inmediato/i);

  // Negative treated as 0
  const negative = validateCountdownSeconds(-2);
  assert.equal(negative.valid, true);
  assert.equal(negative.seconds, 0);

  // 3s = recommended
  const three = validateCountdownSeconds(3);
  assert.equal(three.valid, true);
  assert.equal(three.seconds, 3);
  assert.match(three.label, /recomendado/i);

  // 5s and 10s
  assert.equal(validateCountdownSeconds(5).valid, true);
  assert.equal(validateCountdownSeconds(5).seconds, 5);
  assert.equal(validateCountdownSeconds(10).valid, true);
  assert.equal(validateCountdownSeconds(10).seconds, 10);

  // Invalid value falls back to 3s
  const invalid = validateCountdownSeconds(99);
  assert.equal(invalid.valid, false);
  assert.equal(invalid.seconds, 3);
});

test('generateNativeResolutionPresets creates proportional even resolutions for mobile aspect ratios', () => {
  // Test with typical 20:9 smartphone screen (1080 x 2400)
  const presets20x9 = generateNativeResolutionPresets(1080, 2400);
  assert.equal(presets20x9.length, 5);

  // All presets must have even dimensions for YUV420p hardware codecs
  for (const preset of presets20x9) {
    assert.equal(preset.width % 2, 0, `Width ${preset.width} must be even`);
    assert.equal(preset.height % 2, 0, `Height ${preset.height} must be even`);
    assert.ok(preset.width >= 240 && preset.height >= 240);
    assert.equal(preset.isNative, true);
    assert.ok(preset.recommendedBitrateMbps >= 4);
  }

  // Native 100% should match screen
  const native100 = presets20x9.find(p => p.id === 'native-100');
  assert.ok(native100 !== undefined);
  assert.equal(native100?.width, 1080);
  assert.equal(native100?.height, 2400);

  // Test with non-even screen input to ensure normalization
  const oddPresets = generateNativeResolutionPresets(1081, 2399);
  for (const preset of oddPresets) {
    assert.equal(preset.width % 2, 0);
    assert.equal(preset.height % 2, 0);
  }
});

test('calculateAspectRatioLabel correctly maps standard aspect ratios', () => {
  assert.equal(calculateAspectRatioLabel(1080, 2400), '20:9');
  assert.equal(calculateAspectRatioLabel(720, 1600), '20:9');
  assert.equal(calculateAspectRatioLabel(1080, 2340), '19.5:9');
  assert.equal(calculateAspectRatioLabel(720, 1560), '19.5:9');
  assert.equal(calculateAspectRatioLabel(1440, 3040), '19:9');
  assert.equal(calculateAspectRatioLabel(1080, 2520), '21:9');
  assert.equal(calculateAspectRatioLabel(1080, 1920), '9:16');
  assert.equal(calculateAspectRatioLabel(720, 1280), '9:16');
});

test('createCustomResolutionPreset enforces parity and sets custom metadata', () => {
  // Odd numbers should be rounded down/adjusted to even
  const customOdd = createCustomResolutionPreset(1081, 2399);
  assert.equal(customOdd.width % 2, 0);
  assert.equal(customOdd.height % 2, 0);
  assert.equal(customOdd.width, 1080);
  assert.equal(customOdd.height, 2398);
  assert.equal(customOdd.isCustom, true);
  assert.match(customOdd.id, /^custom-/);

  // Even numbers remain untouched
  const customEven = createCustomResolutionPreset(1080, 2400, 'Mi Resolución Gamer');
  assert.equal(customEven.width, 1080);
  assert.equal(customEven.height, 2400);
  assert.equal(customEven.name, 'Mi Resolución Gamer');
  assert.equal(customEven.isCustom, true);
  assert.equal(customEven.aspectRatio, '20:9');
  assert.ok(customEven.recommendedBitrateMbps >= 8);
});

test('STANDARD_POPULAR_PRESETS includes 20:9, 19.5:9, 19:9, 21:9, and 9:16 presets', () => {
  assert.ok(STANDARD_POPULAR_PRESETS.length >= 10);
  for (const preset of STANDARD_POPULAR_PRESETS) {
    assert.equal(preset.width % 2, 0, `Width of ${preset.id} must be even`);
    assert.equal(preset.height % 2, 0, `Height of ${preset.id} must be even`);
    assert.ok(preset.recommendedBitrateMbps > 0);
  }

  const p20x9 = STANDARD_POPULAR_PRESETS.find(p => p.id === 'std_2400x1080');
  assert.ok(p20x9 !== undefined);
  assert.equal(p20x9?.aspectRatio, '20:9');

  const p19_5 = STANDARD_POPULAR_PRESETS.find(p => p.id === 'std_2340x1080');
  assert.ok(p19_5 !== undefined);
  assert.equal(p19_5?.aspectRatio, '19.5:9');

  const p19 = STANDARD_POPULAR_PRESETS.find(p => p.id === 'std_3040x1440');
  assert.ok(p19 !== undefined);
  assert.equal(p19?.aspectRatio, '19:9');

  const p21 = STANDARD_POPULAR_PRESETS.find(p => p.id === 'std_2520x1080');
  assert.ok(p21 !== undefined);
  assert.equal(p21?.aspectRatio, '21:9');
});

test('serializeUserPreferences and parseUserPreferences maintain settings across sessions', () => {
  const customSettings = {
    engine: 'rust2024' as const,
    resolutionId: 'custom-1200x540',
    customWidth: 1200,
    customHeight: 540,
    bitrateMbps: 24,
    fps: 90,
    codec: 'HEVC' as const,
    audioSource: 'mic' as const,
    countdownSec: 5
  };

  const serialized = serializeUserPreferences(customSettings);
  assert.equal(typeof serialized, 'string');

  const restored = parseUserPreferences(serialized);
  assert.deepEqual(restored, customSettings);

  // Fallback seguro ante JSON corrupto o nulo
  const fallbackNull = parseUserPreferences(null);
  assert.deepEqual(fallbackNull, DEFAULT_USER_PREFERENCES);

  const fallbackCorrupt = parseUserPreferences('{invalid json}');
  assert.deepEqual(fallbackCorrupt, DEFAULT_USER_PREFERENCES);
});

test('validateAppPackageName validates Android package names correctly', () => {
  // Paquetes válidos de juegos y apps populares
  assert.equal(validateAppPackageName('com.dts.freefireth'), true);
  assert.equal(validateAppPackageName('com.tencent.ig'), true);
  assert.equal(validateAppPackageName('com.supercell.brawlstars'), true);
  assert.equal(validateAppPackageName('com.velorec.recorder'), true);
  assert.equal(validateAppPackageName('org.videolan.vlc'), true);

  // Paquetes inválidos
  assert.equal(validateAppPackageName(''), false);
  assert.equal(validateAppPackageName(null), false);
  assert.equal(validateAppPackageName(undefined), false);
  assert.equal(validateAppPackageName('invalidpackage'), false); // Sin punto
  assert.equal(validateAppPackageName('.com.test'), false); // Empieza con punto
  assert.equal(validateAppPackageName('com.test.'), false); // Termina con punto
  assert.equal(validateAppPackageName('123com.test'), false); // Empieza con número
});

test('getShaderPipelineCapabilities provides GLSL and SPIR-V specs', () => {
  const caps = getShaderPipelineCapabilities();
  assert.equal(caps.vulkanSpirvSupported, true);
  assert.equal(caps.glslEs32Supported, true);
  assert.equal(caps.zeroCpuOverhead, true);
  assert.ok(caps.formats.includes('GLSL_ES32'));
  assert.ok(caps.formats.includes('SPIR_V'));

  const effectIds = caps.supportedEffects.map(e => e.id);
  assert.ok(effectIds.includes('passthrough'));
  assert.ok(effectIds.includes('facecam_circular'));
  assert.ok(effectIds.includes('gaming_color_boost'));
});

test('getGoStreamingEngineSpecs defines RTMP and SRT streaming specs', () => {
  const specs = getGoStreamingEngineSpecs();
  assert.match(specs.engine, /Go/);
  assert.ok(specs.protocols.includes('RTMP'));
  assert.ok(specs.protocols.includes('SRT'));
  assert.equal(specs.zeroCopyNalDispatch, true);
  assert.equal(specs.queueCapacity, 1024);
  assert.equal(specs.ringBufferBytes, 4 * 1024 * 1024);
  assert.ok(specs.crossPlatformAbis.includes('arm64-v8a'));
  assert.ok(specs.crossPlatformAbis.includes('armeabi-v7a'));
});
