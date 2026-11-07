import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateEstimatedFileSize,
  formatBytes,
  formatDuration,
  validateResolution,
  calculateEngineMetrics,
  RESOLUTION_PRESETS
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
