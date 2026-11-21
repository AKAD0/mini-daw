#include "minidaw/analysis/swiftf0.h"

#include <onnxruntime_cxx_api.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <stdexcept>
#include <utility>

namespace minidaw::analysis {
namespace {

constexpr float kMinHz = 46.875F;
constexpr float kMaxHz = 2093.75F;
constexpr std::size_t kWindowFrames = 1875;
constexpr std::size_t kLeftFrames = 11;
constexpr std::size_t kLookaheadFrames = 10;
constexpr float kSilencePeak = 1e-3F;

// The values of an output as float. The model gives the pitch as float64 and the confidence as
// float32 (TK-008 found this in the run "Check"), so the code reads the type of each output.
std::vector<float> valuesOf(const Ort::Value& value) {
  const auto info = value.GetTensorTypeAndShapeInfo();
  const std::size_t count = info.GetElementCount();
  if (info.GetElementType() == ONNX_TENSOR_ELEMENT_DATA_TYPE_DOUBLE) {
    const auto* data = value.GetTensorData<double>();
    std::vector<float> out(count);
    for (std::size_t i = 0; i < count; ++i) {
      out[i] = static_cast<float>(data[i]);
    }
    return out;
  }
  if (info.GetElementType() == ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT) {
    const auto* data = value.GetTensorData<float>();
    return {data, data + count};
  }
  throw std::runtime_error("SwiftF0: an output has neither the type float32 nor float64");
}

}  // namespace

struct SwiftF0::Model {
  Ort::Env env{ORT_LOGGING_LEVEL_WARNING, "minidaw"};
  Ort::Session session{nullptr};
  Ort::MemoryInfo memory = Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault);

  // Pitch and confidence for one window of audio.
  std::pair<std::vector<float>, std::vector<float>> run(std::span<const float> window) {
    std::vector<float> audio(window.begin(), window.end());
    const std::array<std::int64_t, 2> shape{1, static_cast<std::int64_t>(audio.size())};
    float low = kMinHz;
    float high = kMaxHz;
    std::array<Ort::Value, 3> inputs{
        Ort::Value::CreateTensor<float>(memory, audio.data(), audio.size(), shape.data(),
                                        shape.size()),
        Ort::Value::CreateTensor<float>(memory, &low, 1, nullptr, 0),
        Ort::Value::CreateTensor<float>(memory, &high, 1, nullptr, 0)};
    const std::array<const char*, 3> inputNames{"audio", "fmin", "fmax"};
    const std::array<const char*, 2> outputNames{"pitch", "confidence"};
    std::vector<Ort::Value> outputs =
        session.Run(Ort::RunOptions{nullptr}, inputNames.data(), inputs.data(), inputs.size(),
                    outputNames.data(), outputNames.size());
    return {valuesOf(outputs[0]), valuesOf(outputs[1])};
  }
};

SwiftF0::SwiftF0(const std::string& modelPath) : model_(std::make_unique<Model>()) {
  Ort::SessionOptions options;
  options.SetIntraOpNumThreads(1);
  options.SetInterOpNumThreads(1);
  options.SetExecutionMode(ExecutionMode::ORT_SEQUENTIAL);
  model_->session = Ort::Session(model_->env, modelPath.c_str(), options);
}

SwiftF0::~SwiftF0() = default;

std::vector<Frame> SwiftF0::detect(std::span<const float> audio) {
  const std::size_t count = frameCount(audio.size());
  std::vector<Frame> frames(count);
  const std::size_t full = std::max<std::size_t>(1, audio.size() / kHop);
  for (std::size_t start = 0; start < full && !audio.empty(); start += kWindowFrames) {
    const std::size_t end = std::min(start + kWindowFrames, full);
    const std::size_t left = start >= kLeftFrames ? start - kLeftFrames : 0;
    std::size_t stop = audio.size();
    if (end < full) {
      stop = std::min(audio.size(), (end + kLookaheadFrames) * kHop);
    }
    const auto [pitch, confidence] = model_->run(audio.subspan(left * kHop, stop - left * kHop));
    for (std::size_t i = start; i < end && i - left < pitch.size() && i < count; ++i) {
      frames[i].frequency = pitch[i - left];
      frames[i].confidence = confidence[i - left];
    }
  }
  for (std::size_t i = 0; i < count; ++i) {
    const std::size_t begin = i * kHop;
    const std::size_t stop = std::min(audio.size(), begin + kHop);
    float peak = 0.0F;
    for (std::size_t n = begin; n < stop; ++n) {
      peak = std::max(peak, std::abs(audio[n]));
    }
    if (peak < kSilencePeak) {
      frames[i].frequency = 0.0F;
      frames[i].confidence = 0.0F;
    }
    frames[i].loudness = frameLoudness(audio, i);
  }
  return frames;
}

}  // namespace minidaw::analysis
