"""AC-AIX-18: a second run of the setup script changes nothing on the account. The environment
that it creates has the limited network of D-047 (feature_specifications.md Sections 5.2.8, 5.8)."""

import os

import setup_account as sa
from conftest import TEST_KEY

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")


def run(*args):
    lines = []
    code = sa.main(list(args), out=lines.append)
    return code, lines


def posts(fake, path):
    return [r for r in fake.requests if r["method"] == "POST" and r["path"] == path]


def test_the_first_run_creates_the_four_items(fake, local_files):
    toolkit, model = local_files
    code, lines = run("--toolkit", toolkit, "--model", model)
    assert code == 0, lines
    assert len(posts(fake, "/v1/files")) == 2
    assert len(posts(fake, "/v1/environments")) == 1 and len(posts(fake, "/v1/agents")) == 1
    assert {f["filename"] for f in fake.files.values()} == {"minidaw-toolkit-v1.pyz", "minidaw-htdemucs-v1.onnx"}
    assert any("created: minidaw-toolkit-v1.pyz" in line for line in lines), lines


def test_a_second_run_changes_nothing(fake, local_files):
    toolkit, model = local_files
    assert run("--toolkit", toolkit, "--model", model)[0] == 0
    before = len(fake.changes())
    ids = (dict(fake.agents), dict(fake.environments), dict(fake.files))
    code, lines = run("--toolkit", toolkit, "--model", model)
    assert code == 0, lines
    assert len(fake.changes()) == before, fake.changes()[before:]
    assert (fake.agents, fake.environments, fake.files) == ids
    assert "created: nothing" in lines


def test_the_environment_has_the_limited_network_of_d047(fake, local_files):
    run("--toolkit", local_files[0], "--model", local_files[1])
    body = posts(fake, "/v1/environments")[0]["body"]
    assert body["name"] == "minidaw-env-v1"
    networking = body["config"]["networking"]
    assert networking == {"type": "limited", "allow_package_managers": True}, networking
    with open(os.path.join(ROOT, "toolkit", "requirements.txt")) as f:
        wanted = [line.strip() for line in f if line.strip() and not line.startswith("#")]
    assert body["config"]["packages"] == {"type": "packages", "pip": wanted}


def test_the_agent_has_the_small_tier_the_prompt_and_three_tools(fake, local_files):
    run("--toolkit", local_files[0], "--model", local_files[1])
    request = posts(fake, "/v1/agents")[0]
    body = request["body"]
    assert body["name"] == "minidaw-extractor-v1" and body["model"] == "claude-haiku-5-5"
    assert body["system"] == sa.system_prompt()
    toolset = body["tools"][0]
    enabled = {c["name"] for c in toolset["configs"] if c["enabled"]}
    assert toolset["default_config"]["enabled"] is False
    assert enabled == {"bash", "read", "write"}
    assert {"web_fetch", "web_search"} <= {c["name"] for c in toolset["configs"] if not c["enabled"]}
    assert "managed-agents-2026-04-01" in request["headers"].get("anthropic-beta", "")


def test_check_reports_and_changes_nothing(fake):
    code, lines = run("--check")
    assert code == 1
    assert fake.changes() == []
    assert sum("is missing" in line for line in lines) == 4, lines


def test_an_item_with_other_content_stops_the_setup(fake, local_files):
    toolkit, model = local_files
    run("--toolkit", toolkit, "--model", model)
    with open(toolkit, "ab") as f:
        f.write(b" changed")
    before = len(fake.changes())
    code, lines = run("--toolkit", toolkit, "--model", model)
    assert code == 1 and len(fake.changes()) == before
    assert any("minidaw-toolkit-v1.pyz exists with" in line for line in lines), lines


def test_a_missing_key_stops_with_a_message(fake, monkeypatch):
    monkeypatch.delenv("ANTHROPIC_API_KEY")
    code, lines = run("--check")
    assert code == 2 and fake.requests == []
    assert "ANTHROPIC_API_KEY" in lines[0]


def test_the_key_is_never_printed(fake, local_files):
    _, first = run("--toolkit", local_files[0], "--model", local_files[1])
    _, second = run("--check")
    assert not any(TEST_KEY in line for line in first + second)


def test_the_system_prompt_equals_the_text_of_the_specification():
    with open(os.path.join(ROOT, "docs", "feature_specifications.md"), encoding="utf-8") as f:
        text = f.read()
    block = text[text.index("#### 5.2.4 System prompt of the agent"):].split("```text\n", 1)[1].split("\n```", 1)[0]
    assert sa.system_prompt() == block
