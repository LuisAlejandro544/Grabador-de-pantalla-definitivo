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
  getGoStreamingEngineSpecs,
  getRustStreamingEngineSpecs,
  detectStorageTechnology,
  calculateOptimalThreadAffinity,
  calculateSavingProgress,
  estimateDirectGallerySpeedup,
  validateDirectGalleryTarget,
  MIN_CUSTOM_BITRATE_MBPS,
  MAX_SAFE_PHONE_BITRATE_MBPS,
  MAX_EXTREME_HARDWARE_BITRATE_MBPS,
  getBitrateProfile,
  preserveBitrateOnResolutionChange,
  getRustIntegratedLibraries,
  getStreamingEngineComparison,
  getRustRealTimeMuxingCapabilities,
  estimateMuxingLatency,
  getRustOboeArchitectureSpecs,
  calculateAudioSyncOffset,
  getRustYuvMigrationSpecs,
  parseResilienceTelemetry,
  calculateOomKillRisk,
  getRustResilienceGuardianSpecs,
  parseLogcatLine,
  formatDiagnosticReport,
  getDebugInspectorSpecs,
  diagnoseNdkStlLinkage,
  calculateAppListFingerprint,
  shouldRescanInstalledApps,
  getOnboardingPagesDefinition,
  parseVersionTag,
  isNewerVersion,
  getStorageSafetyGuaranteeInfo,
  getReleaseOptimizationProfile,
  validateSigningSecrets
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

test('getRustStreamingEngineSpecs defines RTMP and SRT streaming specs for Rust', () => {
  const specs = getRustStreamingEngineSpecs();
  assert.match(specs.engine, /Rust/);
  assert.ok(specs.protocols.includes('RTMP'));
  assert.ok(specs.protocols.includes('SRT'));
  assert.equal(specs.zeroCopyNalDispatch, true);
  assert.equal(specs.zeroGarbageCollection, true);
  assert.equal(specs.nativeModule, 'streamer.rs');
  assert.equal(specs.queueCapacity, 1024);
  assert.equal(specs.ringBufferBytes, 4 * 1024 * 1024);
  assert.ok(specs.crossPlatformAbis.includes('arm64-v8a'));
  assert.ok(specs.crossPlatformAbis.includes('armeabi-v7a'));
});

test('detectStorageTechnology identifies eMMC and recommends asynchronous buffer pacing', () => {
  // Dispositivo con memoria eMMC (/sys/block/mmcblk0)
  const emmcResult = detectStorageTechnology(['mmcblk0', 'mmcblk0p1', 'mmcblk0rpmb']);
  assert.equal(emmcResult.type, 'eMMC');
  assert.equal(emmcResult.isEmmc, true);
  assert.equal(emmcResult.bufferPacingRecommended, true);
  assert.equal(emmcResult.pacingBatchLimit, 16);
  assert.match(emmcResult.description, /eMMC/);

  // Dispositivo con memoria UFS rápida (SCSI sda/sdb)
  const ufsResult = detectStorageTechnology(['sda', 'sdb', 'sdc']);
  assert.equal(ufsResult.type, 'UFS');
  assert.equal(ufsResult.isEmmc, false);
  assert.equal(ufsResult.bufferPacingRecommended, false);
  assert.equal(ufsResult.pacingBatchLimit, 0);

  // Dispositivo con memoria NVMe
  const nvmeResult = detectStorageTechnology(['nvme0n1', 'nvme0n1p1']);
  assert.equal(nvmeResult.type, 'NVMe');
  assert.equal(nvmeResult.isEmmc, false);
  assert.equal(nvmeResult.bufferPacingRecommended, false);
});

test('calculateOptimalThreadAffinity pins encoder to LITTLE cores and reserves Big cores for gaming', () => {
  // Simulación de un SoC de 8 núcleos como MediaTek Helio G81 / G85:
  // 6 núcleos Cortex-A55 @ 1.8 GHz (eficiencia) + 2 núcleos Cortex-A75 @ 2.0 GHz (rendimiento gamer)
  const helioG81Freqs = [1800000, 1800000, 1800000, 1800000, 1800000, 1800000, 2000000, 2000000];
  const affinityG81 = calculateOptimalThreadAffinity(8, helioG81Freqs);

  assert.equal(affinityG81.totalCores, 8);
  assert.deepEqual(affinityG81.efficiencyCoreIndices, [0, 1, 2, 3, 4, 5]);
  assert.equal(affinityG81.efficiencyCoresCount, 6);
  assert.equal(affinityG81.bigCoresReservedForGaming, 2);
  // Máscara binaria: 1+2+4+8+16+32 = 63 (0x3F)
  assert.equal(affinityG81.cpuAffinityMask, 63);

  // Fallback determinista cuando no se pueden leer frecuencias por permisos de SELinux
  const fallback8Cores = calculateOptimalThreadAffinity(8);
  assert.equal(fallback8Cores.totalCores, 8);
  assert.deepEqual(fallback8Cores.efficiencyCoreIndices, [0, 1, 2, 3]);
  assert.equal(fallback8Cores.efficiencyCoresCount, 4);
  assert.equal(fallback8Cores.bigCoresReservedForGaming, 4);
  // Máscara binaria: 1+2+4+8 = 15 (0x0F)
  assert.equal(fallback8Cores.cpuAffinityMask, 15);

  // Fallback para procesadores de 4 núcleos
  const quadCore = calculateOptimalThreadAffinity(4);
  assert.equal(quadCore.efficiencyCoresCount, 2);
  assert.equal(quadCore.bigCoresReservedForGaming, 2);
});

test('calculateSavingProgress returns correct progression and completion flags', () => {
  const p1 = calculateSavingProgress('stopping');
  assert.equal(p1.percent, 25);
  assert.equal(p1.isComplete, false);
  assert.match(p1.label, /Deteniendo codificación/i);

  const p2 = calculateSavingProgress('finalizing');
  assert.equal(p2.percent, 75);
  assert.equal(p2.isComplete, false);
  assert.match(p2.label, /Publicando en Galería/i);

  const p3 = calculateSavingProgress('completed');
  assert.equal(p3.percent, 100);
  assert.equal(p3.isComplete, true);
  assert.match(p3.label, /éxito/i);
});

test('estimateDirectGallerySpeedup demonstrates time savings on eMMC and UFS', () => {
  // 300 MB recording
  const fileBytes = 300 * 1024 * 1024;

  const emmcReport = estimateDirectGallerySpeedup(fileBytes, true);
  assert.equal(emmcReport.fileSizeMb, 300);
  // En eMMC a 40 MB/s, 300 MB tardarían ~7.5 segundos
  assert.ok(emmcReport.legacyCopyTimeMs >= 7000);
  assert.equal(emmcReport.directFdTimeMs, 12);
  assert.ok(emmcReport.timeSavedMs >= 7000);
  assert.ok(emmcReport.speedupFactor >= 100);

  const ufsReport = estimateDirectGallerySpeedup(fileBytes, false);
  assert.equal(ufsReport.fileSizeMb, 300);
  assert.ok(ufsReport.legacyCopyTimeMs >= 1500);
  assert.ok(ufsReport.speedupFactor >= 50);
});

test('validateDirectGalleryTarget handles valid and invalid targets deterministically', () => {
  // Valid
  const valid = validateDirectGalleryTarget({
    rawFd: 42,
    uri: 'content://media/external/video/media/999',
    filePath: '/storage/emulated/0/Movies/VeloRec/VeloRec_test.mp4'
  });
  assert.equal(valid.isValid, true);
  assert.equal(valid.reason, undefined);

  // Invalid null / undefined
  assert.equal(validateDirectGalleryTarget(null).isValid, false);
  assert.equal(validateDirectGalleryTarget(undefined).isValid, false);

  // Invalid FD
  const invalidFd = validateDirectGalleryTarget({
    rawFd: -1,
    uri: 'content://media/external/video/media/999',
    filePath: '/storage/emulated/0/Movies/VeloRec/VeloRec_test.mp4'
  });
  assert.equal(invalidFd.isValid, false);
  assert.match(invalidFd.reason ?? '', /FD/i);

  // Invalid URI
  const invalidUri = validateDirectGalleryTarget({
    rawFd: 12,
    uri: 'file:///path/to/video.mp4',
    filePath: '/storage/emulated/0/Movies/VeloRec/VeloRec_test.mp4'
  });
  assert.equal(invalidUri.isValid, false);
  assert.match(invalidUri.reason ?? '', /content:\/\//i);

  // Empty path
  const emptyPath = validateDirectGalleryTarget({
    rawFd: 12,
    uri: 'content://media/external/video/media/999',
    filePath: '   '
  });
  assert.equal(emptyPath.isValid, false);
  assert.match(emptyPath.reason ?? '', /ruta/i);
});

test('getBitrateProfile supports 1 Mbps minimum and evaluates thermal safety to not choke phone', () => {
  // 1 Mbps Ultra Ahorro
  const ultraSaving = getBitrateProfile(1);
  assert.equal(ultraSaving.bitrateMbps, 1);
  assert.equal(ultraSaving.isValid, true);
  assert.equal(ultraSaving.tier, 'ultra_saving');
  assert.equal(ultraSaving.thermalSafety, 'ultra_cool');
  assert.equal(ultraSaving.storageUsagePerMinuteMb, 7.5); // (1 * 60) / 8 = 7.5 MB/min
  assert.equal(ultraSaving.recommendedForPhone, true);

  // Negative or 0 should clamp to 1 Mbps
  const zeroClamp = getBitrateProfile(0);
  assert.equal(zeroClamp.bitrateMbps, MIN_CUSTOM_BITRATE_MBPS);
  const negClamp = getBitrateProfile(-10);
  assert.equal(negClamp.bitrateMbps, 1);

  // 12 Mbps Zona Óptima Gamer
  const optimal12 = getBitrateProfile(12);
  assert.equal(optimal12.bitrateMbps, 12);
  assert.equal(optimal12.tier, 'safe_gaming');
  assert.equal(optimal12.thermalSafety, 'optimal');
  assert.equal(optimal12.storageUsagePerMinuteMb, 90); // (12 * 60) / 8 = 90 MB/min
  assert.equal(optimal12.recommendedForPhone, true);

  // 24 Mbps Alta Calidad
  const high24 = getBitrateProfile(24);
  assert.equal(high24.tier, 'high_quality');
  assert.equal(high24.thermalSafety, 'moderate');
  assert.equal(high24.recommendedForPhone, true);

  // 50 Mbps Calidad Pro en eMMC alerta de contención
  const proEmmc = getBitrateProfile(50, true);
  assert.equal(proEmmc.tier, 'pro_quality');
  assert.equal(proEmmc.thermalSafety, 'heavy');
  assert.equal(proEmmc.recommendedForPhone, false);
  assert.ok(proEmmc.warningMessage !== undefined);

  // 60 Mbps Safe Limit
  const maxSafe = getBitrateProfile(MAX_SAFE_PHONE_BITRATE_MBPS);
  assert.equal(maxSafe.bitrateMbps, 60);

  // 75 Mbps Extremo (alerta de sobrecalentamiento / riesgo de ahogo del hardware)
  const extreme = getBitrateProfile(75);
  assert.equal(extreme.tier, 'extreme_hardware');
  assert.equal(extreme.thermalSafety, 'extreme_throttle_risk');
  assert.equal(extreme.recommendedForPhone, false);
  assert.ok(extreme.warningMessage !== undefined);

  // Valores superiores a 80 se limitan al tope de hardware
  const overClamp = getBitrateProfile(150);
  assert.equal(overClamp.bitrateMbps, MAX_EXTREME_HARDWARE_BITRATE_MBPS);
});

test('preserveBitrateOnResolutionChange ensures resolution changes do not affect user custom bitrate', () => {
  // El usuario eligió 1 Mbps (ultra ahorro) y cambia de resolución a 1080p (que recomienda 12 Mbps)
  const preserved1 = preserveBitrateOnResolutionChange(1, 12);
  assert.equal(preserved1, 1, 'El bitrate personalizado de 1 Mbps debe conservarse intacto al cambiar resolución');

  // El usuario eligió 24 Mbps y cambia a 720p (que recomienda 8 Mbps)
  const preserved24 = preserveBitrateOnResolutionChange(24, 8);
  assert.equal(preserved24, 24, 'El bitrate personalizado de 24 Mbps debe conservarse intacto al cambiar resolución');

  // El usuario eligió 50 Mbps y cambia a Nativa 100%
  const preserved50 = preserveBitrateOnResolutionChange(50, 14);
  assert.equal(preserved50, 50, 'El bitrate personalizado de 50 Mbps debe conservarse intacto al cambiar resolución');
});

test('getRustIntegratedLibraries exposes oboe, yuvutils-rs and glow specifications', () => {
  const libs = getRustIntegratedLibraries();
  assert.equal(libs.length, 3);

  const oboe = libs.find(l => l.crate === 'oboe');
  assert.ok(oboe !== undefined);
  assert.equal(oboe?.category, 'audio');
  assert.equal(oboe?.license, 'Apache-2.0');
  assert.equal(oboe?.nativeModule, 'audio_oboe.rs');

  const yuv = libs.find(l => l.crate === 'yuvutils-rs');
  assert.ok(yuv !== undefined);
  assert.equal(yuv?.category, 'color');
  assert.equal(yuv?.simdNeomSupported, true);
  assert.equal(yuv?.nativeModule, 'color_yuv.rs');

  const glow = libs.find(l => l.crate === 'glow');
  assert.ok(glow !== undefined);
  assert.equal(glow?.category, 'gpu');
  assert.equal(glow?.nativeModule, 'gpu_glow.rs');
});

test('getStreamingEngineComparison compares Rust vs Go for on-device and server streaming', () => {
  const comparison = getStreamingEngineComparison();
  assert.equal(comparison.recommendationOnDevice, 'Rust');
  assert.equal(comparison.recommendationServer, 'Go');
  assert.equal(comparison.items.length, 2);

  const rustItem = comparison.items.find(i => i.language === 'Rust');
  assert.ok(rustItem !== undefined);
  assert.ok(rustItem?.memoryFootprintMb.includes('< 10 MB'));
  assert.ok(rustItem?.onDeviceClientVerdict.includes('RECOMENDADO'));

  const goItem = comparison.items.find(i => i.language === 'Go');
  assert.ok(goItem !== undefined);
  assert.ok(goItem?.serverRelayVerdict.includes('RECOMENDADO'));
});

test('getRustRealTimeMuxingCapabilities validates real-time muxer specs and zero-delay stop', () => {
  const caps = getRustRealTimeMuxingCapabilities();
  assert.equal(caps.engine, 'Rust 2024');
  assert.equal(caps.zeroDelayStop, true);
  assert.ok(caps.videoTrackFormat.includes('H.264 / HEVC / AV1'));
  assert.ok(caps.audioTrackFormat.includes('AAC-LC'));
  assert.equal(caps.threadSafeMuxerState, true);
  assert.ok(caps.summary.includes('menos de 15 ms'));
});

test('estimateMuxingLatency confirms zero wait (<15ms) for Rust real-time muxing vs legacy merge', () => {
  // Archivo típico de 200 MB
  const fileBytes = 200 * 1024 * 1024;

  const rustReport = estimateMuxingLatency(true, fileBytes);
  assert.equal(rustReport.isRealTimeRustMuxing, true);
  assert.equal(rustReport.fileSizeMb, 200);
  assert.equal(rustReport.stopLatencyMs, 12);
  assert.equal(rustReport.postProcessingRequired, false);
  assert.equal(rustReport.tempFilesCreated, 0);
  assert.ok(rustReport.speedupDescription.includes('Cero espera al detener'));

  const legacyReport = estimateMuxingLatency(false, fileBytes);
  assert.equal(legacyReport.isRealTimeRustMuxing, false);
  assert.equal(legacyReport.fileSizeMb, 200);
  assert.ok(legacyReport.stopLatencyMs >= 3000);
  assert.equal(legacyReport.postProcessingRequired, true);
  assert.equal(legacyReport.tempFilesCreated, 2);
  assert.ok(legacyReport.speedupDescription.includes('Fusión secundaria en disco'));
});

test('getRustOboeArchitectureSpecs confirms 100% Rust oboe crate integration and C++ removal', () => {
  const specs = getRustOboeArchitectureSpecs();
  assert.equal(specs.runtime, 'Rust 2024');
  assert.equal(specs.cppOboeRemoved, true);
  assert.equal(specs.oboeCrateIntegrated, true);
  assert.equal(specs.latencyTargetMs, 8);
  assert.ok(specs.audioBackends.some(b => b.includes('AAudio')));
  assert.ok(specs.summary.includes('eliminando dependencias de Oboe en C++23'));
});

test('calculateAudioSyncOffset accurately models zero-drift and backlog prevention in games', () => {
  const sampleRate = 48000;
  const startUs = 1000000; // t = 1.0s

  // 1. Camino feliz: audio sincronizado perfectamente con el reloj de muestras
  // Tras 48000 muestras (1 segundo exacto), currentMonoUs avanza 1s (1_000_000 us)
  const normalSync = calculateAudioSyncOffset(
    startUs,
    startUs + 1_000_000,
    48000,
    sampleRate,
    2048, // 1 cuadro en buffer (21.3 ms)
    6144
  );
  assert.equal(normalSync.isAligned, true);
  assert.equal(normalSync.actionTaken, 'normal');
  assert.equal(normalSync.backlogDroppedShorts, 0);
  assert.equal(normalSync.safePtsUs, startUs + 1_000_000);

  // 2. Mitigación de Lag Acumulado (Subway Surfers):
  // Si la CPU tuvo un pico y el ring buffer acumuló 12000 shorts (~125 ms de audio viejo atrasado)
  const backlogRecovery = calculateAudioSyncOffset(
    startUs,
    startUs + 5_000_000,
    240000,
    sampleRate,
    12000, // backlog excesivo
    6144
  );
  assert.equal(backlogRecovery.actionTaken, 'drop_old_backlog');
  assert.equal(backlogRecovery.backlogDroppedShorts, 12000 - 2048); // Descartó 9952 shorts viejos
  assert.equal(backlogRecovery.isAligned, false);

  // 3. Resincronización ante pausas o silencios del juego (>35ms de diferencia con el reloj real)
  const silenceRecovery = calculateAudioSyncOffset(
    startUs,
    startUs + 1_500_000, // El reloj avanzó 1.5s pero el juego solo mandó 1.0s de audio por pausa
    48000, // 1.0s de audio
    sampleRate,
    2048,
    6144
  );
  assert.equal(silenceRecovery.actionTaken, 'resync_timebase');
  assert.equal(silenceRecovery.driftUs, 500_000); // 500 ms de gap detectado
  assert.equal(silenceRecovery.safePtsUs, startUs + 1_500_000); // Reancla el PTS al tiempo real actual
});

test('getRustYuvMigrationSpecs confirms 100% Rust yuvutils-rs migration and C++ libyuv removal', () => {
  const specs = getRustYuvMigrationSpecs();
  assert.equal(specs.runtime, 'Rust 2024');
  assert.equal(specs.cppLibyuvRemoved, true);
  assert.equal(specs.crate, 'yuvutils-rs');
  assert.ok(specs.supportedFormats.includes('RGBA to NV12'));
  assert.ok(specs.supportedFormats.includes('RGBA to I420'));
  assert.ok(specs.supportedFormats.includes('Bilinear Plane Scaling'));
  assert.ok(specs.simdAcceleration.includes('ARM NEON'));
  assert.ok(specs.fpsThroughput >= 200);
  assert.ok(specs.summary.includes('Google libyuv fue migrado 100% a Rust 2024'));
});

test('parseResilienceTelemetry parses JSON from Rust guardian with defensive fallbacks', () => {
  const json = JSON.stringify({
    rss_kb: 14336,
    vmsize_kb: 70000,
    oom_score: 150,
    oom_score_adj: 100,
    system_available_ram_mb: 850,
    total_trims: 3,
    risk_level: 'OPTIMAL_HEALTHY'
  });

  const parsed = parseResilienceTelemetry(json);
  assert.equal(parsed.rssKb, 14336);
  assert.equal(parsed.rssMb, 14);
  assert.equal(parsed.oomScore, 150);
  assert.equal(parsed.oomScoreAdj, 100);
  assert.equal(parsed.systemAvailableRamMb, 850);
  assert.equal(parsed.totalTrims, 3);
  assert.equal(parsed.riskLevel, 'OPTIMAL_HEALTHY');

  // Fallback defensivo para null / string corrupto
  const fallback = parseResilienceTelemetry(null);
  assert.equal(fallback.riskLevel, 'OPTIMAL_HEALTHY');
  assert.ok(fallback.rssMb <= 15);
});

test('calculateOomKillRisk detects LMK danger zones and provides recommendations', () => {
  // Escenario 1: Normal en juego ligero o pantalla de inicio
  const safe = calculateOomKillRisk(120, 1200);
  assert.equal(safe.isSafe, true);
  assert.equal(safe.riskRating, 'Bajo');

  // Escenario 2: Presión moderada durante partida de Free Fire / Subway Surfers
  const moderate = calculateOomKillRisk(450, 160);
  assert.equal(moderate.isSafe, true);
  assert.equal(moderate.riskRating, 'Moderado');
  assert.ok(moderate.recommendation.includes('RSS protegida'));

  // Escenario 3: Presión crítica (memoria disponible < 80 MB o oom_score > 700)
  const critical = calculateOomKillRisk(750, 60);
  assert.equal(critical.isSafe, false);
  assert.equal(critical.riskRating, 'Crítico');
  assert.ok(critical.recommendation.includes('malloc_trim'));
});

test('getRustResilienceGuardianSpecs exposes anti-LMK and real-time scheduling architecture', () => {
  const specs = getRustResilienceGuardianSpecs();
  assert.equal(specs.engine, 'Rust 2024 Resilience Guardian');
  assert.equal(specs.zeroDataLossGuarantee, true);
  assert.ok(specs.antiLmkTechniques.some(t => t.includes('malloc_trim')));
  assert.ok(specs.antiLmkTechniques.some(t => t.includes('nice -16')));
  assert.ok(specs.antiLmkTechniques.some(t => t.includes('SYS_ioprio_set')));
  assert.ok(specs.antiLmkTechniques.some(t => t.includes('mlock')));
  assert.ok(specs.antiLmkTechniques.some(t => t.includes('SIGTERM')));
  assert.deepEqual(specs.supportedArchitectures, ['arm64-v8a', 'armeabi-v7a', 'x86_64']);
});

test('parseLogcatLine correctly parses standard Android logcat time format', () => {
  const line = '11-09 19:14:13.456 E/ScreenRecordService( 14096): Error al iniciar grabación: No implementation found';
  const parsed = parseLogcatLine(line);
  assert.ok(parsed !== null);
  assert.equal(parsed.timestamp, '11-09 19:14:13.456');
  assert.equal(parsed.level, 'E');
  assert.equal(parsed.tag, 'ScreenRecordService');
  assert.match(parsed.message, /No implementation found/);

  // Input inválido o vacío
  assert.equal(parseLogcatLine(''), null);
  assert.equal(parseLogcatLine('   '), null);
});

test('formatDiagnosticReport generates markdown with device, ABIs, and native library status', () => {
  const report = formatDiagnosticReport({
    model: 'Xiaomi Poco M5',
    androidVersion: '14 (API 34)',
    abis: ['arm64-v8a', 'armeabi-v7a'],
    rustLoaded: true,
    cppLoaded: true,
    recentErrors: ['19:14:13 [ERROR] ScreenRecordService: Codec ok']
  });

  assert.match(report, /### 📱 Reporte de Diagnóstico VeloRec Studio/);
  assert.match(report, /Xiaomi Poco M5/);
  assert.match(report, /arm64-v8a, armeabi-v7a/);
  assert.match(report, /libvelorec_rust2024.so`:\s*✅ Cargada con éxito/);
  assert.match(report, /libvelorec_cpp23.so`:\s*✅ Cargada con éxito/);
  assert.match(report, /Registros de Error Recientes/);
});

test('getDebugInspectorSpecs defines standalone activity capabilities and leakcanary style behavior', () => {
  const specs = getDebugInspectorSpecs();
  assert.equal(specs.name, 'VeloRec Inspector');
  assert.equal(specs.launcherIcon, true);
  assert.equal(specs.taskAffinity, 'com.velorec.recorder.debug');
  assert.ok(specs.capabilities.some(c => c.includes('Logcat')));
  assert.ok(specs.capabilities.some(c => c.includes('UncaughtExceptionHandler')));
  assert.ok(specs.capabilities.some(c => c.includes('Markdown')));
});

test('diagnoseNdkStlLinkage identifies __gxx_personality_v0 and recommends shared-stdcxx solution', () => {
  // Caso de error como el visto en el dispositivo del usuario
  const errorDiag = diagnoseNdkStlLinkage({
    isSharedStdcxxEnabled: false,
    isLibCxxPreloaded: false,
    rustLibraryLoaded: false,
    dlopenErrorMessage: 'dlopen failed: cannot locate symbol "__gxx_personality_v0" referenced by "/data/app/.../libvelorec_rust2024.so"'
  });

  assert.equal(errorDiag.isResolved, false);
  assert.match(errorDiag.diagnosis, /__gxx_personality_v0/);
  assert.match(errorDiag.actionRequired || '', /shared-stdcxx/);

  // Caso de error con malloc_trim en Android Bionic
  const mallocTrimDiag = diagnoseNdkStlLinkage({
    isSharedStdcxxEnabled: true,
    isLibCxxPreloaded: true,
    rustLibraryLoaded: false,
    dlopenErrorMessage: 'dlopen failed: cannot locate symbol "malloc_trim" referenced by ".../lib/arm64-v8a/libvelorec_rust2024.so"'
  });

  assert.equal(mallocTrimDiag.isResolved, false);
  assert.match(mallocTrimDiag.diagnosis, /malloc_trim/);
  assert.match(mallocTrimDiag.actionRequired || '', /dlsym|mallopt/);

  // Caso resuelto
  const okDiag = diagnoseNdkStlLinkage({
    isSharedStdcxxEnabled: true,
    isLibCxxPreloaded: true,
    rustLibraryLoaded: true,
    dlopenErrorMessage: null
  });

  assert.equal(okDiag.isResolved, true);
  assert.match(okDiag.diagnosis, /libc\+\+_shared\.so/);
});

test('shouldRescanInstalledApps prevents redundant scans and detects package additions/deletions', () => {
  const initialPackages = ['com.dts.freefireth', 'com.tencent.ig', 'com.supercell.brawlstars'];
  const initialHash = calculateAppListFingerprint(initialPackages);

  // 1. Segunda visita a la pestaña sin cambios: NO debe reescanear (reutiliza cache instantáneamente)
  const noChange = shouldRescanInstalledApps(initialHash, initialHash, true, false);
  assert.equal(noChange, false);

  // 2. Si no hay cache previa (primera apertura): SÍ debe escanear
  const firstOpen = shouldRescanInstalledApps(initialHash, initialHash, false, false);
  assert.equal(firstOpen, true);

  // 3. Si el usuario descargó una app nueva (Free Fire + Roblox): fingerprint cambia, SÍ debe escanear
  const withNewApp = ['com.dts.freefireth', 'com.tencent.ig', 'com.supercell.brawlstars', 'com.roblox.client'];
  const newHash = calculateAppListFingerprint(withNewApp);
  assert.notEqual(initialHash, newHash);
  const detectedNewApp = shouldRescanInstalledApps(initialHash, newHash, true, false);
  assert.equal(detectedNewApp, true);

  // 4. Si el usuario eliminó una app: fingerprint cambia, SÍ debe escanear
  const withDeletedApp = ['com.dts.freefireth', 'com.supercell.brawlstars'];
  const deletedHash = calculateAppListFingerprint(withDeletedApp);
  assert.notEqual(initialHash, deletedHash);
  const detectedDeletedApp = shouldRescanInstalledApps(initialHash, deletedHash, true, false);
  assert.equal(detectedDeletedApp, true);

  // 5. Si el usuario pulsa manualmente el botón de Refrescar: forceRefresh = true
  const manualRefresh = shouldRescanInstalledApps(initialHash, initialHash, true, true);
  assert.equal(manualRefresh, true);
});

test('getOnboardingPagesDefinition configures 4 structured walkthrough steps with permissions', () => {
  const pages = getOnboardingPagesDefinition();
  assert.equal(pages.length, 4);

  // Página 0: Rendimiento y Rust 2024
  assert.equal(pages[0].pageIndex, 0);
  assert.match(pages[0].title, /Sin Lag/);

  // Página 1: Audio Interno y sincronización
  assert.equal(pages[1].pageIndex, 1);
  assert.match(pages[1].title, /Sonido/);

  // Página 2: Lanzador 1-Click REC y Galería
  assert.equal(pages[2].pageIndex, 2);
  assert.match(pages[2].title, /1-Click/);

  // Página 3: Permisos del sistema
  assert.equal(pages[3].pageIndex, 3);
  assert.match(pages[3].title, /Permisos/);
  assert.ok(pages[3].requiredPermissions.includes('android.permission.RECORD_AUDIO'));
  assert.ok(pages[3].requiredPermissions.includes('android.permission.POST_NOTIFICATIONS'));
});

test('parseVersionTag accurately parses version string, numbers, and -E stable flag', () => {
  const v1 = parseVersionTag('v1.0.0-E');
  assert.equal(v1.major, 1);
  assert.equal(v1.minor, 0);
  assert.equal(v1.patch, 0);
  assert.equal(v1.isStable, true);
  assert.equal(v1.tagSuffix, 'E');

  const v2 = parseVersionTag('1.2.3');
  assert.equal(v2.major, 1);
  assert.equal(v2.minor, 2);
  assert.equal(v2.patch, 3);
  assert.equal(v2.isStable, false);

  const v3 = parseVersionTag(null);
  assert.equal(v3.major, 0);
  assert.equal(v3.isStable, false);
});

test('isNewerVersion detects when a remote release is newer than current version', () => {
  const current = '1.0.0-E';

  // Mayor versión
  assert.equal(isNewerVersion('v2.0.0-E', current), true);
  assert.equal(isNewerVersion('2.0.0', current), true);

  // Menor versión
  assert.equal(isNewerVersion('v1.1.0-E', current), true);
  assert.equal(isNewerVersion('1.0.1-E', current), true);

  // Misma versión
  assert.equal(isNewerVersion('v1.0.0-E', current), false);
  assert.equal(isNewerVersion('1.0.0-E', current), false);

  // Versión inferior
  assert.equal(isNewerVersion('v0.9.9-E', current), false);
  assert.equal(isNewerVersion('1.0.0-beta', current), false);
});

test('getStorageSafetyGuaranteeInfo confirms recordings in Movies/VeloRec are immune to updates', () => {
  const guarantee = getStorageSafetyGuaranteeInfo();
  assert.equal(guarantee.isImmuneToUpdates, true);
  assert.equal(guarantee.path, 'Movies/VeloRec');
  assert.equal(guarantee.storageType, 'MediaStore_Shared_Public');
  assert.match(guarantee.description, /NUNCA se eliminan/);
});

test('getReleaseOptimizationProfile strips debug inspector and logcat in release but keeps in debug', () => {
  const debugProfile = getReleaseOptimizationProfile('debug');
  assert.equal(debugProfile.hasDebugInspector, true);
  assert.equal(debugProfile.hasLogcatEnabled, true);
  assert.equal(debugProfile.isMinified, false);
  assert.equal(debugProfile.r8LogcatStripping, false);

  const releaseProfile = getReleaseOptimizationProfile('release');
  assert.equal(releaseProfile.hasDebugInspector, false);
  assert.equal(releaseProfile.hasLogcatEnabled, false);
  assert.equal(releaseProfile.isMinified, true);
  assert.equal(releaseProfile.isShrinkResources, true);
  assert.equal(releaseProfile.signingRequired, true);
  assert.equal(releaseProfile.r8LogcatStripping, true);
});

test('validateSigningSecrets requires all 4 mandatory release secrets', () => {
  // Caso incompleto
  const incomplete = validateSigningSecrets({
    keystoreBase64: 'abc123base64',
    keystorePassword: 'pass'
  });
  assert.equal(incomplete.isValid, false);
  assert.ok(incomplete.missingSecrets.includes('RELEASE_KEY_ALIAS'));
  assert.ok(incomplete.missingSecrets.includes('RELEASE_KEY_PASSWORD'));

  // Caso completo
  const complete = validateSigningSecrets({
    keystoreBase64: 'abc123base64',
    keystorePassword: 'storepassword123',
    keyAlias: 'velorec_release',
    keyPassword: 'keypassword123'
  });
  assert.equal(complete.isValid, true);
  assert.equal(complete.missingSecrets.length, 0);
  assert.equal(complete.configuredSecrets.length, 4);
});

