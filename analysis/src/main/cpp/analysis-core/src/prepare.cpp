#include "minidaw/analysis/prepare.h"

#include <cmath>
#include <cstddef>
#include <numbers>
#include <stdexcept>

#include "minidaw/analysis/frame.h"

namespace minidaw::analysis {
namespace {

constexpr double kPassBandHz = 7200.0;
constexpr std::size_t kTapsPerFactor = 64;
constexpr double kHighPassHz = 60.0;

// Low-pass filter with a Blackman window, for the reduction by an integer factor.
std::vector<double> lowPass(std::size_t factor, int sampleRate) {
  const std::size_t taps = kTapsPerFactor * factor + 1;
  const double cutoff = kPassBandHz / static_cast<double>(sampleRate);
  const double middle = static_cast<double>(taps - 1) / 2.0;
  const double pi = std::numbers::pi;
  std::vector<double> h(taps);
  double sum = 0.0;
  for (std::size_t n = 0; n < taps; ++n) {
    const double x = static_cast<double>(n) - middle;
    const double sinc = x == 0.0 ? 2.0 * cutoff : std::sin(2.0 * pi * cutoff * x) / (pi * x);
    const double phase = 2.0 * pi * static_cast<double>(n) / static_cast<double>(taps - 1);
    const double window = 0.42 - 0.5 * std::cos(phase) + 0.08 * std::cos(2.0 * phase);
    h[n] = sinc * window;
    sum += h[n];
  }
  for (double& value : h) {
    value /= sum;
  }
  return h;
}

std::vector<double> reduce(std::span<const float> audio, std::size_t factor, int sampleRate) {
  const std::vector<double> h = lowPass(factor, sampleRate);
  const std::size_t middle = (h.size() - 1) / 2;
  const std::size_t count = (audio.size() + factor - 1) / factor;
  std::vector<double> out(count);
  for (std::size_t k = 0; k < count; ++k) {
    const std::size_t center = k * factor;
    double sum = 0.0;
    for (std::size_t n = 0; n < h.size(); ++n) {
      if (center + middle >= n && center + middle - n < audio.size()) {
        sum += h[n] * static_cast<double>(audio[center + middle - n]);
      }
    }
    out[k] = sum;
  }
  return out;
}

// High-pass filter of second order after Butterworth, as one biquad (RBJ cookbook).
std::vector<float> highPass(const std::vector<double>& x) {
  const double w0 = 2.0 * std::numbers::pi * kHighPassHz / static_cast<double>(kAnalysisRate);
  // Q is 1 / sqrt(2), so 2 * Q is sqrt(2).
  const double alpha = std::sin(w0) / std::numbers::sqrt2;
  const double cosw = std::cos(w0);
  const double a0 = 1.0 + alpha;
  const double b0 = (1.0 + cosw) / 2.0 / a0;
  const double b1 = -(1.0 + cosw) / a0;
  const double a1 = -2.0 * cosw / a0;
  const double a2 = (1.0 - alpha) / a0;
  std::vector<float> y(x.size());
  double x1 = 0.0;
  double x2 = 0.0;
  double y1 = 0.0;
  double y2 = 0.0;
  for (std::size_t i = 0; i < x.size(); ++i) {
    const double out = b0 * x[i] + b1 * x1 + b0 * x2 - a1 * y1 - a2 * y2;
    x2 = x1;
    x1 = x[i];
    y2 = y1;
    y1 = out;
    y[i] = static_cast<float>(out);
  }
  return y;
}

}  // namespace

std::vector<float> prepare(std::span<const float> audio, int sampleRate) {
  if (sampleRate < kAnalysisRate || sampleRate % kAnalysisRate != 0) {
    throw std::invalid_argument("the rate must be a multiple of 16 kHz");
  }
  const auto factor = static_cast<std::size_t>(sampleRate / kAnalysisRate);
  if (factor == 1) {
    std::vector<double> copy(audio.begin(), audio.end());
    return highPass(copy);
  }
  return highPass(reduce(audio, factor, sampleRate));
}

}  // namespace minidaw::analysis
