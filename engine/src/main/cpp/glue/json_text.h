#pragma once

#include <array>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <string>

namespace minidaw::engine::glue {

// Writes one flat JSON object for the checks of PH-00. The names and the texts hold no quote.
class Json {
 public:
  Json& text(const char* name, const std::string& value) {
    key(name);
    out_ += "\"" + value + "\"";
    return *this;
  }

  Json& number(const char* name, int64_t value) {
    key(name);
    out_ += std::to_string(value);
    return *this;
  }

  // A number with three decimals, for example of milliseconds. A value that is no finite number
  // becomes null.
  Json& decimal(const char* name, double value) {
    key(name);
    if (!std::isfinite(value)) {
      out_ += "null";
      return *this;
    }
    std::array<char, 32> buffer{};
    std::snprintf(buffer.data(), buffer.size(), "%.3f", value);
    out_ += buffer.data();
    return *this;
  }

  [[nodiscard]] std::string str() const { return out_ + "}"; }

 private:
  void key(const char* name) {
    out_ += out_.size() > 1 ? ",\"" : "\"";
    out_ += name;
    out_ += "\":";
  }

  std::string out_ = "{";
};

}  // namespace minidaw::engine::glue
