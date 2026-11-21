#include "minidaw/analysis/pyin.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <limits>

namespace minidaw::analysis {
namespace {

constexpr std::size_t kWindow = 1024;
constexpr std::size_t kHalfWindow = kWindow / 2;
constexpr double kMinHz = 60.0;
constexpr double kMaxHz = 2094.0;
constexpr std::size_t kThresholds = 100;
constexpr double kBetaA = 2.0;
constexpr double kBetaB = 18.0;
constexpr double kAbsoluteMinimumWeight = 0.01;
constexpr double kBinsPerOctave = 60.0;
constexpr double kMaxOctavesPerSecond = 35.92;
constexpr double kSwitchProbability = 0.01;
constexpr float kSilencePeak = 1e-4F;
constexpr double kTiny = 1e-30;

// A pitch candidate of one frame: frequency in Hz and its share of the prior of the thresholds.
struct Trough {
  double frequency = 0.0;
  double probability = 0.0;
};

// Prior over the thresholds 0.01 to 1.00: a Beta distribution with mean 0.1, as in pYIN.
std::array<double, kThresholds> thresholdPrior() {
  std::array<double, kThresholds> prior{};
  double sum = 0.0;
  for (std::size_t k = 0; k < kThresholds; ++k) {
    const double s = static_cast<double>(k + 1) / static_cast<double>(kThresholds);
    prior.at(k) = std::pow(s, kBetaA - 1.0) * std::pow(1.0 - s, kBetaB - 1.0);
    sum += prior.at(k);
  }
  for (double& p : prior) {
    p /= sum;
  }
  return prior;
}

// Stage 1 of pYIN for the window that starts at `window`: the troughs of the cumulative mean
// normalized difference, each weighted by the thresholds that pick it.
std::vector<Trough> frameTroughs(std::span<const float> window,
                                 const std::array<double, kThresholds>& prior,
                                 std::vector<double>& cmnd, std::vector<double>& diff) {
  const auto tauMin = static_cast<std::size_t>(std::floor(kAnalysisRate / kMaxHz));
  const auto tauMax = static_cast<std::size_t>(std::ceil(kAnalysisRate / kMinHz));
  float peak = 0.0F;
  for (const float x : window) {
    peak = std::max(peak, std::abs(x));
  }
  if (peak < kSilencePeak) {
    return {};
  }
  cmnd.assign(tauMax + 2, 1.0);
  diff.assign(tauMax + 2, 0.0);
  double running = 0.0;
  // The symmetric form: for each lag, the compared samples lie centered on the frame, so the
  // frame reports the pitch at its own time.
  for (std::size_t tau = 1; tau <= tauMax + 1; ++tau) {
    const std::size_t first = kHalfWindow - kHalfWindow / 2 - tau / 2;
    double d = 0.0;
    for (std::size_t j = first; j < first + kHalfWindow; ++j) {
      const double delta = static_cast<double>(window[j]) - static_cast<double>(window[j + tau]);
      d += delta * delta;
    }
    running += d;
    diff[tau] = d;
    cmnd[tau] = running > 0.0 ? d * static_cast<double>(tau) / running : 1.0;
  }
  std::vector<std::size_t> troughs;
  for (std::size_t tau = tauMin; tau <= tauMax; ++tau) {
    if (cmnd[tau] < cmnd[tau - 1] && cmnd[tau] <= cmnd[tau + 1]) {
      troughs.push_back(tau);
    }
  }
  if (troughs.empty()) {
    return {};
  }
  std::vector<double> mass(troughs.size(), 0.0);
  std::size_t lowest = 0;
  for (std::size_t t = 1; t < troughs.size(); ++t) {
    if (cmnd[troughs[t]] < cmnd[troughs[lowest]]) {
      lowest = t;
    }
  }
  for (std::size_t k = 0; k < kThresholds; ++k) {
    const double s = static_cast<double>(k + 1) / static_cast<double>(kThresholds);
    std::size_t picked = troughs.size();
    for (std::size_t t = 0; t < troughs.size(); ++t) {
      if (cmnd[troughs[t]] < s) {
        picked = t;
        break;
      }
    }
    if (picked < troughs.size()) {
      mass[picked] += prior.at(k);
    } else {
      mass[lowest] += prior.at(k) * kAbsoluteMinimumWeight;
    }
  }
  std::vector<Trough> out;
  out.reserve(troughs.size());
  for (std::size_t t = 0; t < troughs.size(); ++t) {
    if (mass[t] <= 0.0) {
      continue;
    }
    const std::size_t tau = troughs[t];
    // The parabola goes through the raw difference: it is smoother at short periods.
    const double a = diff[tau - 1];
    const double b = diff[tau];
    const double c = diff[tau + 1];
    const double denominator = a - 2.0 * b + c;
    double shift = 0.0;
    if (denominator != 0.0) {
      shift = std::clamp(0.5 * (a - c) / denominator, -1.0, 1.0);
    }
    const double frequency = kAnalysisRate / (static_cast<double>(tau) + shift);
    if (frequency >= kMinHz && frequency <= kMaxHz) {
      out.push_back(Trough{frequency, mass[t]});
    }
  }
  return out;
}

// The grid of pitch for stage 2: bins of 20 cents from 60 Hz up.
struct Grid {
  std::size_t bins = 0;
  std::size_t maxJump = 0;
  std::vector<double> logWeight;
  std::vector<double> logNorm;

  Grid() {
    bins = static_cast<std::size_t>(std::floor(kBinsPerOctave * std::log2(kMaxHz / kMinHz))) + 1;
    const double perFrame = kMaxOctavesPerSecond * kStepSeconds * kBinsPerOctave;
    maxJump = static_cast<std::size_t>(std::lround(perFrame));
    logWeight.resize(maxJump + 1);
    for (std::size_t d = 0; d <= maxJump; ++d) {
      logWeight[d] = std::log(static_cast<double>(maxJump + 1 - d));
    }
    logNorm.resize(bins);
    for (std::size_t i = 0; i < bins; ++i) {
      double sum = 0.0;
      const std::size_t lo = i >= maxJump ? i - maxJump : 0;
      const std::size_t hi = std::min(bins - 1, i + maxJump);
      for (std::size_t j = lo; j <= hi; ++j) {
        sum += static_cast<double>(maxJump + 1 - (i > j ? i - j : j - i));
      }
      logNorm[i] = std::log(sum);
    }
  }

  [[nodiscard]] std::size_t binOf(double frequency) const {
    const double position = kBinsPerOctave * std::log2(frequency / kMinHz);
    const auto bin = static_cast<std::size_t>(std::max(0L, std::lround(position)));
    return std::min(bin, bins - 1);
  }

  [[nodiscard]] static double frequencyOf(std::size_t bin) {
    return kMinHz * std::exp2(static_cast<double>(bin) / kBinsPerOctave);
  }
};

}  // namespace

std::vector<Frame> Pyin::detect(std::span<const float> audio) {
  const std::size_t count = frameCount(audio.size());
  std::vector<Frame> frames(count);
  if (count == 0) {
    return frames;
  }
  std::vector<float> padded(audio.size() + kWindow, 0.0F);
  std::copy(audio.begin(), audio.end(), padded.begin() + static_cast<std::ptrdiff_t>(kHalfWindow));
  const std::span<const float> all(padded);
  const std::array<double, kThresholds> prior = thresholdPrior();
  std::vector<double> cmnd;
  std::vector<double> diff;
  std::vector<std::vector<Trough>> candidates(count);
  std::vector<double> voicedProbability(count, 0.0);
  for (std::size_t i = 0; i < count; ++i) {
    candidates[i] = frameTroughs(all.subspan(i * kHop, kWindow), prior, cmnd, diff);
    double sum = 0.0;
    for (const Trough& t : candidates[i]) {
      sum += t.probability;
    }
    voicedProbability[i] = std::min(1.0, sum);
    frames[i].loudness = frameLoudness(audio, i);
  }

  // Stage 2: Viterbi over the states "voiced at bin b" (0 .. bins-1) and "unvoiced" (bins ..).
  const Grid grid;
  const std::size_t bins = grid.bins;
  const double logStay = std::log(1.0 - kSwitchProbability);
  const double logSwitch = std::log(kSwitchProbability);
  const double logTiny = std::log(kTiny);
  std::vector<double> observation(2 * bins);
  std::vector<double> previous(2 * bins, logTiny);
  std::vector<double> current(2 * bins);
  std::vector<std::uint16_t> back(count * 2 * bins, 0);
  auto observe = [&](std::size_t i) {
    std::fill(observation.begin(), observation.begin() + static_cast<std::ptrdiff_t>(bins), 0.0);
    for (const Trough& t : candidates[i]) {
      observation[grid.binOf(t.frequency)] += t.probability;
    }
    const double share = (1.0 - voicedProbability[i]) / static_cast<double>(bins);
    const double unvoiced = std::log(std::max(share, kTiny));
    for (std::size_t b = 0; b < bins; ++b) {
      observation[b] = std::log(std::max(observation[b], kTiny));
      observation[bins + b] = unvoiced;
    }
  };
  observe(0);
  for (std::size_t b = 0; b < bins; ++b) {
    previous[bins + b] = -std::log(static_cast<double>(bins)) + observation[bins + b];
  }
  for (std::size_t i = 1; i < count; ++i) {
    observe(i);
    for (std::size_t target = 0; target < 2 * bins; ++target) {
      const bool targetVoiced = target < bins;
      const std::size_t bin = targetVoiced ? target : target - bins;
      const std::size_t lo = bin >= grid.maxJump ? bin - grid.maxJump : 0;
      const std::size_t hi = std::min(bins - 1, bin + grid.maxJump);
      const double fromVoiced = targetVoiced ? logStay : logSwitch;
      const double fromUnvoiced = targetVoiced ? logSwitch : logStay;
      double best = -std::numeric_limits<double>::infinity();
      std::size_t from = 0;
      for (std::size_t source = lo; source <= hi; ++source) {
        const std::size_t jump = bin > source ? bin - source : source - bin;
        const double move = grid.logWeight[jump] - grid.logNorm[source];
        const double viaVoiced = previous[source] + move + fromVoiced;
        const double viaUnvoiced = previous[bins + source] + move + fromUnvoiced;
        if (viaVoiced > best) {
          best = viaVoiced;
          from = source;
        }
        if (viaUnvoiced > best) {
          best = viaUnvoiced;
          from = bins + source;
        }
      }
      current[target] = best + observation[target];
      back[i * 2 * bins + target] = static_cast<std::uint16_t>(from);
    }
    std::swap(previous, current);
  }
  const auto last = std::max_element(previous.begin(), previous.end());
  auto state = static_cast<std::size_t>(last - previous.begin());
  for (std::size_t step = 0; step < count; ++step) {
    const std::size_t i = count - 1 - step;
    if (state < bins) {
      double frequency = Grid::frequencyOf(state);
      double probability = 0.0;
      for (const Trough& t : candidates[i]) {
        const std::size_t b = grid.binOf(t.frequency);
        const std::size_t distance = b > state ? b - state : state - b;
        if (distance <= 1 && t.probability > probability) {
          probability = t.probability;
          frequency = t.frequency;
        }
      }
      frames[i].frequency = static_cast<float>(frequency);
      frames[i].confidence = static_cast<float>(voicedProbability[i]);
    }
    state = back[i * 2 * bins + state];
  }
  return frames;
}

}  // namespace minidaw::analysis
