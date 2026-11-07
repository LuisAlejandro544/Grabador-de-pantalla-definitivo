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
  detectVideoContainer
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

test('detectVideoContainer identifies mp4 and mkv formats', () => {
  assert.equal(detectVideoContainer('clip.mp4'), 'mp4');
  assert.equal(detectVideoContainer('CLIP.MP4'), 'mp4');
  assert.equal(detectVideoContainer('record.mkv'), 'mkv');
  assert.equal(detectVideoContainer('audio.wav'), 'unknown');
});


