"""Fixtures for the tests of setup/: the fake API and an API key that is no secret."""

import os
import sys

import pytest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, ".."))

from fake_api import FakeApi  # noqa: E402

# Built at run time, so the text of this file holds no value that looks like a key.
TEST_KEY = "test" + "-key-" + "for-the-fake-api-only"


@pytest.fixture
def fake(monkeypatch):
    with FakeApi() as api:
        monkeypatch.setenv("ANTHROPIC_API_KEY", TEST_KEY)
        monkeypatch.setenv("ANTHROPIC_BASE_URL", api.url)
        yield api


@pytest.fixture
def local_files(tmp_path):
    toolkit = tmp_path / "toolkit.pyz"
    toolkit.write_bytes(b"PK toolkit")
    model = tmp_path / "htdemucs.onnx"
    model.write_bytes(b"onnx model" * 10)
    return str(toolkit), str(model)
