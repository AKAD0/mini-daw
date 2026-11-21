#include "test_melodies.h"

#include <cmath>
#include <cstddef>
#include <cstdint>
#include <numbers>
#include <random>
#include <utility>

namespace minidaw::analysis::fixtures {
namespace {

constexpr double kAmplitude = 0.3;
constexpr double kEdgeSeconds = 0.010;
constexpr double kTailSeconds = 0.3;

struct Tone {
  double start = 0.0;
  double end = 0.0;
  int pitch = 0;
};

struct Synth {
  std::vector<Tone> tones;
  int harmonics = 8;
  double vibratoSemitones = 0.0;
  double vibratoHz = 5.5;
  double glideSeconds = 0.0;
  double noiseSnrDb = 0.0;
};

bool touches(double a, double b) { return std::abs(a - b) < 1e-9; }

// Tones that follow each other: each lasts `length`, with `pause` between two of them.
std::vector<Tone> sequence(const std::vector<int>& pitches, double length, double pause) {
  std::vector<Tone> tones;
  double t = 0.2;
  for (const int p : pitches) {
    tones.push_back(Tone{t, t + length, p});
    t += length + pause;
  }
  return tones;
}

// Pitch at time t inside tone k, with a linear glide around a boundary to a touching tone.
double pitchAt(const Synth& s, std::size_t k, double t) {
  const Tone& tone = s.tones[k];
  const double half = s.glideSeconds / 2.0;
  double pitch = tone.pitch;
  if (half > 0.0 && k + 1 < s.tones.size() && touches(tone.end, s.tones[k + 1].start) &&
      t > tone.end - half) {
    pitch += (t - (tone.end - half)) / s.glideSeconds * (s.tones[k + 1].pitch - tone.pitch);
  }
  if (half > 0.0 && k > 0 && touches(s.tones[k - 1].end, tone.start) && t < tone.start + half) {
    const double previous = s.tones[k - 1].pitch;
    pitch = previous + (t - (tone.start - half)) / s.glideSeconds * (tone.pitch - previous);
  }
  return pitch + s.vibratoSemitones * std::sin(2.0 * std::numbers::pi * s.vibratoHz * t);
}

double envelopeAt(const Synth& s, std::size_t k, double t) {
  const Tone& tone = s.tones[k];
  double gain = 1.0;
  const bool joinedBefore = k > 0 && touches(s.tones[k - 1].end, tone.start);
  const bool joinedAfter = k + 1 < s.tones.size() && touches(tone.end, s.tones[k + 1].start);
  if (!joinedBefore && t < tone.start + kEdgeSeconds) {
    gain = 0.5 - 0.5 * std::cos(std::numbers::pi * (t - tone.start) / kEdgeSeconds);
  }
  if (!joinedAfter && t > tone.end - kEdgeSeconds) {
    gain = 0.5 - 0.5 * std::cos(std::numbers::pi * (tone.end - t) / kEdgeSeconds);
  }
  return gain;
}

std::vector<float> render(const Synth& s) {
  const double length = s.tones.back().end + kTailSeconds;
  const auto count = static_cast<std::size_t>(length * kMelodyRate);
  std::vector<double> out(count, 0.0);
  double norm = 0.0;
  for (int h = 1; h <= s.harmonics; ++h) {
    norm += 1.0 / h;
  }
  double phase = 0.0;
  std::size_t k = 0;
  double power = 0.0;
  std::size_t sounding = 0;
  for (std::size_t n = 0; n < count; ++n) {
    const double t = static_cast<double>(n) / kMelodyRate;
    while (k < s.tones.size() && t >= s.tones[k].end) {
      ++k;
    }
    if (k >= s.tones.size() || t < s.tones[k].start) {
      continue;
    }
    const double frequency = 440.0 * std::exp2((pitchAt(s, k, t) - 69.0) / 12.0);
    phase += 2.0 * std::numbers::pi * frequency / kMelodyRate;
    double value = 0.0;
    for (int h = 1; h <= s.harmonics; ++h) {
      value += std::sin(h * phase) / h;
    }
    out[n] = kAmplitude * envelopeAt(s, k, t) * value / norm;
    power += out[n] * out[n];
    ++sounding;
  }
  if (s.noiseSnrDb > 0.0 && sounding > 0) {
    const double rms = std::sqrt(power / static_cast<double>(sounding));
    const double peak = rms * std::pow(10.0, -s.noiseSnrDb / 20.0) * std::sqrt(3.0);
    std::mt19937 generator(20261121U);
    for (double& x : out) {
      const double u = static_cast<double>(generator()) / 4294967295.0;
      x += peak * (2.0 * u - 1.0);
    }
  }
  return {out.begin(), out.end()};
}

TestMelody melody(std::string name, const Synth& s, bool whistle) {
  TestMelody m;
  m.name = std::move(name);
  m.whistle = whistle;
  m.audio = render(s);
  for (const Tone& tone : s.tones) {
    m.notes.push_back(ExpectedNote{tone.start, tone.pitch});
  }
  return m;
}

}  // namespace

std::vector<TestMelody> testMelodies() {
  std::vector<TestMelody> all;
  Synth legato;
  legato.tones = sequence({55, 57, 59, 60, 62, 60, 57, 55}, 0.4, 0.0);
  all.push_back(melody("tones with overtones, legato", legato, false));

  Synth whistle;
  whistle.harmonics = 1;
  whistle.tones = sequence({84, 86, 88, 89, 91, 93, 95, 93}, 0.3, 0.05);
  all.push_back(melody("pure tones of a whistle", whistle, true));

  Synth vibrato;
  vibrato.vibratoSemitones = 0.5;
  vibrato.tones = sequence({57, 62, 59, 64}, 0.6, 0.12);
  all.push_back(melody("tones with vibrato", vibrato, false));

  Synth slides;
  slides.glideSeconds = 0.06;
  slides.tones = sequence({55, 60, 57, 62}, 0.45, 0.0);
  all.push_back(melody("tones with slides", slides, false));

  Synth noise;
  noise.noiseSnrDb = 20.0;
  noise.tones = sequence({60, 62, 64, 65, 67}, 0.4, 0.15);
  all.push_back(melody("tones with noise", noise, false));

  Synth pauses;
  pauses.tones = sequence({60, 64, 67, 72, 67, 64, 60}, 0.4, 0.2);
  all.push_back(melody("tones with pauses", pauses, false));
  return all;
}

std::vector<float> sine(double frequency, double seconds, int rate, double amplitude) {
  const auto count = static_cast<std::size_t>(seconds * rate);
  std::vector<float> out(count);
  for (std::size_t n = 0; n < count; ++n) {
    const double t = static_cast<double>(n) / rate;
    out[n] = static_cast<float>(amplitude * std::sin(2.0 * std::numbers::pi * frequency * t));
  }
  return out;
}

}  // namespace minidaw::analysis::fixtures
