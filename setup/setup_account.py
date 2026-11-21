"""Setup of the Claude API account of the owner for extraction (FR-AIX-20, feature_specifications.md 5.2.8).

Run:  ANTHROPIC_API_KEY=... python3 setup/setup_account.py --toolkit build/toolkit/toolkit.pyz
          --model build/models/htdemucs_fp16weights.onnx [--check]

The owner runs it, on a computer with Python or as the manual run "Setup of the account" of the CI
service. It reads the API key from the variable ANTHROPIC_API_KEY and stores it nowhere. It never
prints the key.

Steps:
  1. Stop with a clear message when the key is missing.
  2. Look for the fixed names on the account. Items with the right version are kept.
  3. Upload the Toolkit and the model file, if they are missing.
  4. Create the environment with the packages of D-046 and the limited network of D-047.
  5. Create the agent with the system prompt of Section 5.2.4, the small model tier, and the
     tools shell, read and write.
  6. Print the names and the IDs.

A second run changes nothing (AC-AIX-18). With --check the script only reports what exists and
makes no change. An item that exists with the right name but other content stops the script: a
change of the setup needs a new version, so the fixed names change too.
"""

import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
SETUP_VERSION = 1
NAMES = {
    "agent": f"minidaw-extractor-v{SETUP_VERSION}",
    "environment": f"minidaw-env-v{SETUP_VERSION}",
    "toolkit": f"minidaw-toolkit-v{SETUP_VERSION}.pyz",
    "model": f"minidaw-htdemucs-v{SETUP_VERSION}.onnx",
}
# The small model tier first (D-047). The check V-06 also runs the middle tier per Session.
SMALL_TIER = "claude-haiku-5-5"
TOOLS_ON = ("bash", "read", "write")
TOOLS_OFF = ("edit", "glob", "grep", "web_fetch", "web_search")


def system_prompt():
    with open(os.path.join(HERE, f"system_prompt_v{SETUP_VERSION}.txt"), encoding="utf-8") as f:
        return f.read().rstrip("\n")


def packages():
    """The Python packages of the Toolkit (toolkit/requirements.txt, D-046)."""
    with open(os.path.join(ROOT, "toolkit", "requirements.txt"), encoding="utf-8") as f:
        return [line.split("#")[0].strip() for line in f if line.split("#")[0].strip()]


def environment_config():
    """Limited network: only the package registries (D-047, NFR-SEC-06). No other host."""
    return {
        "type": "cloud",
        "networking": {"type": "limited", "allow_package_managers": True},
        "packages": {"type": "packages", "pip": packages()},
    }


def agent_tools():
    """The tools shell, read and write (D-047). All other tools of the toolset are off, the web
    tools too. The Session runs unattended, so the three tools run without a confirmation."""
    return [{
        "type": "agent_toolset_20260401",
        "default_config": {"enabled": False},
        "configs": [{"name": n, "enabled": True, "permission_policy": {"type": "always_allow"}} for n in TOOLS_ON]
        + [{"name": n, "enabled": False} for n in TOOLS_OFF],
    }]


def find(items, name):
    for item in items:
        if getattr(item, "name", None) == name or getattr(item, "filename", None) == name:
            return item
    return None


def same_agent(agent):
    """Differences between an agent on the account and the agent of this version."""
    found = []
    model = getattr(agent, "model", None)
    model_id = model if isinstance(model, str) else getattr(model, "id", None)
    if model_id and model_id != SMALL_TIER:
        found.append(f"model {model_id}, expected {SMALL_TIER}")
    system = getattr(agent, "system", None)
    if system is not None and system.rstrip("\n") != system_prompt():
        found.append("another system prompt")
    return found


def same_environment(env):
    found = []
    config = getattr(env, "config", None)
    networking = getattr(config, "networking", None)
    if networking is not None and getattr(networking, "type", None) != "limited":
        found.append(f"network {getattr(networking, 'type', None)}, expected limited")
    return found


class Setup:
    def __init__(self, client, out=print):
        self.client = client
        self.out = out
        self.problems = []
        self.created = []
        self.ids = {}

    def file(self, kind, path, check):
        name = NAMES[kind]
        existing = find(self.client.files.list(), name)
        if existing is not None:
            size = getattr(existing, "size_bytes", None)
            if path and size is not None and size != os.path.getsize(path):
                self.problems.append(f"{name} exists with {size} bytes, the local file has {os.path.getsize(path)}")
            self.ids[kind] = existing.id
            return
        if check:
            self.problems.append(f"{name} is missing")
            return
        if not path or not os.path.isfile(path):
            self.problems.append(f"{name} is missing on the account, and the local file {path} does not exist")
            return
        with open(path, "rb") as f:
            uploaded = self.client.files.upload(file=(name, f, "application/octet-stream"))
        self.ids[kind] = uploaded.id
        self.created.append(name)

    def environment(self, check):
        name = NAMES["environment"]
        existing = find(self.client.beta.environments.list(), name)
        if existing is not None:
            self.problems += [f"{name}: {p}" for p in same_environment(existing)]
            self.ids["environment"] = existing.id
            return
        if check:
            self.problems.append(f"{name} is missing")
            return
        env = self.client.beta.environments.create(name=name, config=environment_config(),
                                                   description="mini daw: the Sandbox of extraction (D-046, D-047)")
        self.ids["environment"] = env.id
        self.created.append(name)

    def agent(self, check):
        name = NAMES["agent"]
        existing = find(self.client.beta.agents.list(), name)
        if existing is not None:
            self.problems += [f"{name}: {p}" for p in same_agent(existing)]
            self.ids["agent"] = existing.id
            return
        if check:
            self.problems.append(f"{name} is missing")
            return
        agent = self.client.beta.agents.create(name=name, model=SMALL_TIER, system=system_prompt(), tools=agent_tools(),
                                               description="mini daw: the Orchestrator of extraction (D-047)",
                                               metadata={"minidaw_setup_version": str(SETUP_VERSION)})
        self.ids["agent"] = agent.id
        self.created.append(name)

    def run(self, toolkit, model, check):
        self.file("toolkit", toolkit, check)
        self.file("model", model, check)
        self.environment(check)
        self.agent(check)
        for kind in ("agent", "environment", "toolkit", "model"):
            self.out(f"{kind:<12} {NAMES[kind]:<28} {self.ids.get(kind, '-')}")
        self.out("created: " + (", ".join(self.created) if self.created else "nothing"))
        for p in self.problems:
            self.out("problem: " + p)
        return 0 if not self.problems else 1


def main(argv=None, out=print):
    ap = argparse.ArgumentParser(description="Setup of the Claude API account for extraction (FR-AIX-20)")
    ap.add_argument("--toolkit", help="toolkit.pyz, built by toolkit/build.py")
    ap.add_argument("--model", help="the model file of toolkit/models.toml")
    ap.add_argument("--check", action="store_true", help="report what exists; make no change")
    args = ap.parse_args(argv)
    if not os.environ.get("ANTHROPIC_API_KEY"):
        out("The variable ANTHROPIC_API_KEY is empty. Set it to an API key of the Claude Console (task O-1).")
        out("In the CI service it is the encrypted secret ANTHROPIC_API_KEY of the repository.")
        return 2
    import anthropic

    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])
    try:
        return Setup(client, out).run(args.toolkit, args.model, args.check)
    except anthropic.APIStatusError as e:
        out(f"The Claude API refused a call: HTTP {e.status_code}: {getattr(e, 'message', '')[:300]}")
    except anthropic.APIConnectionError:
        out("The Claude API cannot be reached.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
