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
  generateNativeResolutionPresets
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


