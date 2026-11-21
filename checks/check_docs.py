"""Consistency check of the planning documents of the project "mini daw".

Run:  python3 checks/check_docs.py
It prints one line per check and ends with "RESULT: OK" or "RESULT: PROBLEMS FOUND".

The script looks for the nine documents in the folder above "checks/" and then in its folder "docs/".
"""
import collections
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
NAMES = [
    "requirements.md", "technology_stack.md", "architecture.md",
    "feature_specifications.md", "ui_ux_specification.md",
    "development_roadmap.md", "risk_assessment.md",
    "opus_implementation_guide.md", "project_decisions.md",
]
BASE = next((d for d in (ROOT, os.path.join(ROOT, "docs")) if os.path.exists(os.path.join(d, NAMES[0]))), ROOT)
docs = {}
for n in NAMES:
    p = os.path.join(BASE, n)
    if os.path.exists(p):
        docs[n] = open(p, encoding="utf-8").read()
missing_docs = [n for n in NAMES if n not in docs]
print(f"documents found: {len(docs)} of {len(NAMES)}" + (f" | not written yet: {', '.join(missing_docs)}" if missing_docs else ""))

ok = True
req = docs["requirements.md"]
dec = docs["project_decisions.md"]
tech = docs["technology_stack.md"]
arch = docs.get("architecture.md", "")

# ---------------------------------------------------------------- ID families
# Base families: defined in requirements.md and project_decisions.md.
BASE_ID = r"(?:(?:FR|NFR)-[A-Z0-9]+|CON|AD|[ADQTUG])"
# Families of later stages: (pattern of the prefix, document that defines them).
LATER = [
    (r"AC-[A-Z0-9]+", "feature_specifications.md"),   # acceptance criteria
    (r"MT", "feature_specifications.md"),             # manual tests of the owner
    (r"PH", "development_roadmap.md"),                # phases
    (r"TK", "development_roadmap.md"),                # tasks
    (r"GATE", "development_roadmap.md"),              # gates
    (r"RISK", "risk_assessment.md"),                  # risks
]


def expand(text, prefix_pattern=BASE_ID):
    """IDs that a text names. Expands 'X-01 to -06' and 'X-03, -04'."""
    ids = set()
    full = re.compile(r"(?<![A-Za-z0-9-])(" + prefix_pattern + r")-(\d{2,3})\b")
    for m in full.finditer(text):
        prefix, num = m.group(1), m.group(2)
        ids.add(f"{prefix}-{num}")
        tail = text[m.end():m.end() + 60]
        r = re.match(r"\s+to\s+(?:" + re.escape(prefix) + r")?-(\d{2,3})\b", tail)
        if r:
            for n in range(int(num), int(r.group(1)) + 1):
                ids.add(f"{prefix}-{n:0{len(num)}d}")
        pos = 0
        while True:
            l = re.match(r",\s+-(\d{2,3})\b", tail[pos:])
            if not l:
                break
            ids.add(f"{prefix}-{l.group(1)}")
            pos += l.end()
            r2 = re.match(r"\s+to\s+-(\d{2,3})\b", tail[pos:])
            if r2:
                for n in range(int(l.group(1)), int(r2.group(1)) + 1):
                    ids.add(f"{prefix}-{n:0{len(num)}d}")
    return ids


def defined(text, prefix_pattern=BASE_ID):
    """IDs that a text defines: the first cell of a table row."""
    out = []
    for line in text.splitlines():
        m = re.match(r"\|\s*(" + prefix_pattern + r"-\d{2,3})\b", line)
        if m:
            out.append(m.group(1))
    return out


req_def, dec_def = defined(req), defined(dec)
for name, d in (("requirements.md", req_def), ("project_decisions.md", dec_def)):
    dup = [k for k, v in collections.Counter(d).items() if v > 1]
    print(f"{name}: {len(d)} IDs defined; duplicates: {dup or 'none'}")
    ok &= not dup

all_def = set(req_def) | set(dec_def)
for name, text in docs.items():
    missing = sorted(expand(text) - all_def)
    print(f"{name}: referenced but undefined: {missing or 'none'}")
    ok &= not missing

# Only requirements.md and project_decisions.md may define base IDs.
for name, text in docs.items():
    if name in ("requirements.md", "project_decisions.md"):
        continue
    extra = defined(text)
    if extra:
        print(f"{name}: defines base IDs in a first table cell (must be none): {extra}")
        ok = False

req_refs = expand(req)
unref = sorted(i for i in dec_def if i not in req_refs and i not in expand(dec.replace("| " + i + " ", "| ")))
print("defined in project_decisions.md, referenced nowhere else:", unref or "none")

# ---------------------------------------------------------------- later ID families
for pattern, home in LATER:
    if home not in docs:
        continue
    d = defined(docs[home], pattern)
    dup = [k for k, v in collections.Counter(d).items() if v > 1]
    refs = set()
    for text in docs.values():
        refs |= expand(text, pattern)
    undefined = sorted(refs - set(d))
    label = pattern.split("-")[0]
    print(f"{label} IDs: {len(d)} defined in {home}; duplicates: {dup or 'none'}; referenced but undefined: {undefined or 'none'}")
    ok &= not dup and not undefined


# ---------------------------------------------------------------- sources
def sources(name, letter):
    text = docs.get(name, "")
    d = set(re.findall(r"^\|\s*(" + letter + r"\d+)\s*\|", text, flags=re.M))
    r = set(re.findall(r"\[(" + letter + r"\d+)\]", text))
    return d, r


s_def, s_ref = sources("requirements.md", "S")
print("sources defined:", len(s_def), "| cited but undefined:", sorted(s_ref - s_def) or "none",
      "| defined but never cited:", sorted(s_def - s_ref, key=lambda s: int(s[1:])) or "none")
ok &= not (s_ref - s_def)
r_def, r_ref = sources("technology_stack.md", "R")
print("technology_stack.md sources defined:", len(r_def), "| cited but undefined:", sorted(r_ref - r_def) or "none", "| never cited:", sorted(r_def - r_ref) or "none")
ok &= not (r_ref - r_def)
for name, letter in (("architecture.md", "W"), ("feature_specifications.md", "F"),
                     ("development_roadmap.md", "P"), ("risk_assessment.md", "K")):
    if name in docs:
        x_def, x_ref = sources(name, letter)
        print(f"{name} sources defined:", len(x_def), "| cited but undefined:", sorted(x_ref - x_def) or "none", "| never cited:", sorted(x_def - x_ref) or "none")
        ok &= not (x_ref - x_def) and not (x_def - x_ref)

# ---------------------------------------------------------------- checks V
v_def = set(re.findall(r"^\|\s*(V-\d+)\s*\|", tech, flags=re.M))
v_ref = set()
for text in docs.values():
    v_ref |= set(re.findall(r"(?<![A-Za-z0-9-])V-\d{2}\b", text))
print("checks V defined:", sorted(v_def), "| referenced but undefined:", sorted(v_ref - v_def) or "none")
ok &= not (v_ref - v_def)

# ---------------------------------------------------------------- shape of each document
NUM = r"\d+(?:\.\d+){0,2}"                      # a section number with up to three levels
HEAD = r"^#{2,4} (" + NUM + r")[. ]"
for name, text in docs.items():
    lines = text.splitlines()
    bad = []
    i = 0
    while i < len(lines):
        if lines[i].startswith("|") and i + 1 < len(lines) and re.match(r"^\|[-| ]+\|$", lines[i + 1]):
            n = lines[i].count("|")
            j = i
            while j < len(lines) and lines[j].startswith("|"):
                if lines[j].count("|") != n:
                    bad.append((j + 1, lines[j][:60]))
                j += 1
            i = j
        else:
            i += 1
    fences = len(re.findall(r"^```", text, flags=re.M))
    # references to sections of the same document must exist
    heads = set(re.findall(HEAD, text, flags=re.M))
    bad_sec = []
    prose = re.sub(r"```.*?```", "", text, flags=re.S)
    for m in re.finditer(r"(`[a-z_]+\.md` )?Sections? (" + NUM + r")((?:,| and| to) (" + NUM + r"))*", prose):
        if m.group(1) or re.match(r" of `[a-z_]+\.md`", prose[m.end():m.end() + 40]):
            continue
        for num in re.findall(NUM, m.group(0)):
            if num not in heads:
                bad_sec.append(num)
    # a line "---" right below a line of text would turn this text into a heading
    rule_bad = [k + 1 for k in range(1, len(lines)) if lines[k].strip() == "---" and lines[k - 1].strip()]
    # a list or a table needs an empty line before it
    glue = [k + 1 for k in range(1, len(lines))
            if re.match(r"(1\. |\| )", lines[k]) and lines[k - 1].strip()
            and not re.match(r"(\d+\. |\||\s)", lines[k - 1]) and not lines[k - 1].startswith("```")]
    in_code = False
    code_lines = set()
    for k, l in enumerate(lines):
        if l.startswith("```"):
            in_code = not in_code
            code_lines.add(k + 1)
        elif in_code:
            code_lines.add(k + 1)
    rule_bad = [k for k in rule_bad if k not in code_lines]
    glue = [k for k in glue if k not in code_lines]
    problems = []
    if rule_bad:
        problems.append(f"a rule line right below text, lines: {rule_bad}")
    if glue:
        problems.append(f"a list or a table without an empty line before it, lines: {glue}")
    if bad:
        problems.append(f"table rows with a wrong number of cells: {bad}")
    if fences % 2:
        problems.append("odd number of code fences")
    if bad_sec:
        problems.append(f"references to own sections that do not exist: {sorted(set(bad_sec))}")
    print(f"{name}: tables, lists, rule lines, code fences, own sections: {'; '.join(problems) if problems else 'ok'}")
    ok &= not problems

# references to sections of another document must exist there
cross_bad = []
for name, text in docs.items():
    prose = re.sub(r"```.*?```", "", text, flags=re.S)
    pairs = []
    for m in re.finditer(r"`([a-z_]+\.md)` Sections? (" + NUM + r")((?:,| and| to) (" + NUM + r"))*", prose):
        pairs += [(m.group(1), num) for num in re.findall(NUM, m.group(0)[len(m.group(1)) + 2:])]
    pairs += [(m.group(2), m.group(1)) for m in re.finditer(r"Sections? (" + NUM + r") of `([a-z_]+\.md)`", prose)]
    for other, num in pairs:
        if other in docs:
            heads = set(re.findall(HEAD, docs[other], flags=re.M))
            if num not in heads:
                cross_bad.append(f"{name} -> {other} Section {num}")
print("references to sections of other documents that do not exist:", sorted(set(cross_bad)) or "none")
ok &= not cross_bad

# ---------------------------------------------------------------- stage decisions
if arch:
    d3 = {f"D-{n:03d}" for n in range(57, 73)}
    print("Stage 3 decisions missing in architecture.md:", sorted(d3 - expand(arch)) or "none",
          "| missing in project_decisions.md:", sorted(d3 - set(dec_def)) or "none")
    ok &= not (d3 - expand(arch)) and not (d3 - set(dec_def))

# every new document starts with the table that maps the items of the brief to its sections
def cells_of(line):
    return [c.strip() for c in line.strip().strip("|").split("|")]


for name in ("ui_ux_specification.md", "feature_specifications.md", "development_roadmap.md",
             "risk_assessment.md", "opus_implementation_guide.md"):
    if name not in docs:
        continue
    lines = docs[name].splitlines()
    heads = set(re.findall(HEAD, docs[name], flags=re.M))
    rows, bad_map, empty = 0, [], []
    for i, line in enumerate(lines):
        if not line.startswith("| Item that the brief asks for"):
            continue
        j = i + 2                                   # skip the header and the separator
        while j < len(lines) and lines[j].startswith("|"):
            rows += 1
            found = []
            for cell in cells_of(lines[j])[1:]:
                found += re.findall(r"(?<![\w.-])(" + NUM + r")(?![\w-])", cell)
            if not found:
                empty.append(cells_of(lines[j])[0][:40])
            bad_map += [n for n in found if n not in heads]
            j += 1
    state = "present" if rows else "MISSING"
    print(f"{name}: table that maps the items of the brief: {state}; rows: {rows}; "
          f"rows without a section: {empty or 'none'}; sections that do not exist: {sorted(set(bad_map)) or 'none'}")
    ok &= bool(rows) and not bad_map and not empty

# ---------------------------------------------------------------- Stage 4: criteria and manual tests
fs = docs.get("feature_specifications.md", "")
ui = docs.get("ui_ux_specification.md", "")
if fs:
    mt_def = set(defined(fs, r"MT"))
    ac_rows = [cells_of(l) for l in fs.splitlines() if re.match(r"\| AC-[A-Z0-9]+-\d{2} ", l)]
    bad_shape = [c[0] for c in ac_rows if len(c) != 4]
    bad_check, no_req, used_mt = [], [], set()
    kinds = collections.Counter()
    covered = set()
    for c in ac_rows:
        if len(c) != 4:
            continue
        tokens = [t.strip() for t in c[2].split(",")]
        if not tokens or any(not (t == "Program" or t in mt_def) for t in tokens):
            bad_check.append(c[0])
        mts = [t for t in tokens if t in mt_def]
        used_mt |= set(mts)
        kinds["program" if not mts else ("manual test" if "Program" not in tokens else "program and manual test")] += 1
        ids = expand(c[3])
        if not ids:
            no_req.append(c[0])
        covered |= ids
    print(f"acceptance criteria: {len(ac_rows)}; by check: {dict(sorted(kinds.items()))}; "
          f"wrong shape: {bad_shape or 'none'}; check is neither a program nor a manual test: {bad_check or 'none'}; "
          f"without a requirement: {no_req or 'none'}")
    ok &= not bad_shape and not bad_check and not no_req
    unused_mt = sorted(mt_def - used_mt)
    print(f"manual tests: {len(mt_def)}; not used by any criterion: {unused_mt or 'none'}")
    ok &= not unused_mt

    # every requirement has a criterion, or stands in the table of exceptions (Section 10)
    m = re.search(r"^## 10\. Requirements without an acceptance criterion.*?(?=^## )", fs, flags=re.M | re.S)
    exceptions = expand(m.group(0)) if m else set()
    reqs = [i for i in req_def if i.startswith(("FR-", "NFR-"))]
    uncovered = [i for i in reqs if i not in covered and i not in exceptions]
    both = sorted(i for i in exceptions if i in covered)
    print(f"requirements: {len(reqs)}; with a criterion: {len([i for i in reqs if i in covered])}; "
          f"in the table of exceptions: {len(exceptions)}; without both: {uncovered or 'none'}; in both: {both or 'none'}")
    ok &= bool(m) and not uncovered and not both

    # the nine items of the brief for each core feature and for the Mixer; the eight explanations of the Mixer
    heads3 = set(re.findall(HEAD, fs, flags=re.M))
    need = [f"{s}.{k}" for s in (2, 3, 4, 5, 12) for k in range(1, 10)] + [f"3.2.{k}" for k in range(1, 9)] + ["3.10"]
    lack = [n for n in need if n not in heads3]
    print("feature_specifications.md: sections for the items of the brief that are missing:", lack or "none")
    ok &= not lack

    d4_ui = {f"D-{n:03d}" for n in range(76, 81)}
    d4_fs = {f"D-{n:03d}" for n in range(81, 92)}
    print("Stage 4 decisions missing in ui_ux_specification.md:", sorted(d4_ui - expand(ui)) or "none",
          "| missing in feature_specifications.md:", sorted(d4_fs - expand(fs)) or "none",
          "| missing in project_decisions.md:", sorted((d4_ui | d4_fs) - set(dec_def)) or "none")
    ok &= not (d4_ui - expand(ui)) and not (d4_fs - expand(fs)) and not ((d4_ui | d4_fs) - set(dec_def))

# ---------------------------------------------------------------- Stage 5: roadmap and risks
rm = docs.get("development_roadmap.md", "")
rk = docs.get("risk_assessment.md", "")


def report(label, problems):
    """Prints one line for a group of checks and returns True when the group has no problem."""
    print(f"{label}: {'; '.join(problems) if problems else 'ok'}")
    return not problems


if rm and fs:
    ITEMS = ["Objectives", "Features to implement", "Technical dependencies", "Implementation order",
             "Expected deliverables", "Testing requirements", "Definition of done", "Risks to watch"]
    items_of, done_gate, risks_of, task_phase, task_cells, stray = {}, {}, {}, {}, {}, []
    phase = None
    for line in rm.splitlines():
        h = re.match(r"### 6\.\d+ (PH-\d{2})\b", line)
        if h:
            phase = h.group(1)
            items_of[phase] = []
        elif re.match(r"#{2,4} ", line):
            phase = None
        if not line.startswith("|"):
            continue
        c = cells_of(line)
        if re.fullmatch(r"TK-\d{3}", c[0]):
            if phase is None:
                stray.append(c[0])
            task_phase[c[0]] = phase
            task_cells[c[0]] = c
        elif phase and c[0] in ITEMS:
            items_of[phase].append(c[0])
            if c[0] == "Definition of done":
                done_gate[phase] = expand(c[1], "GATE")
            if c[0] == "Risks to watch":
                risks_of[phase] = expand(c[1], "RISK")

    # --- phases: one section each, with the seven items of the brief in their order, and the risks
    ph_rows = {cells_of(l)[0]: cells_of(l) for l in rm.splitlines() if re.match(r"\| PH-\d{2} ", l)}
    problems = []
    no_section = [p for p in ph_rows if p not in items_of]
    wrong_items = [p for p, it in items_of.items() if it != ITEMS]
    if no_section:
        problems.append(f"phases without a section: {no_section}")
    if wrong_items:
        problems.append(f"sections without the seven items of the brief in their order: {wrong_items}")
    ok &= report(f"phases: {len(ph_rows)}; seven items of the brief in each", problems)

    # --- gates: one per phase, with the number of the phase
    gates = {}
    for l in rm.splitlines():
        if re.match(r"\| GATE-\d{2} ", l):
            c = cells_of(l)
            gates[c[0]] = {"phase": c[1], "must": expand(c[2], "MT"), "early": expand(c[3], "MT"), "cells": len(c)}
    problems = []
    for p in ph_rows:
        g = "GATE-" + p[3:]
        if g not in gates or gates[g]["phase"] != p or gates[g]["cells"] != 5:
            problems.append(f"{p} has no gate row {g}")
        if done_gate.get(p) != {g}:
            problems.append(f"the definition of done of {p} does not name {g}")
    if len(gates) != len(ph_rows):
        problems.append("count of gates differs from count of phases")
    ok &= report(f"gates: {len(gates)}; one per phase", problems)

    # --- tasks: shape, size, order, dependencies, ranges of the overview
    problems = []
    nums = sorted(int(t[3:]) for t in task_cells)
    if nums != list(range(1, len(nums) + 1)):
        problems.append("the numbers of the tasks have a gap")
    if stray:
        problems.append(f"tasks outside a phase: {stray}")
    for t, c in sorted(task_cells.items()):
        if len(c) != 6:
            problems.append(f"{t}: wrong number of cells")
            continue
        if c[4] not in ("S", "M", "L"):
            problems.append(f"{t}: size {c[4]!r}")
        needs = expand(c[3], "TK")
        if not needs and c[3] != "—":
            problems.append(f"{t}: cell 'Needs' is neither a task nor a dash")
        late = sorted(n for n in needs if int(n[3:]) >= int(t[3:]) or n not in task_cells)
        if late:
            problems.append(f"{t} needs a later or unknown task: {late}")
    for p, c in ph_rows.items():
        named = expand(c[4], "TK")
        real = {t for t, ph in task_phase.items() if ph == p}
        if named != real:
            problems.append(f"{p}: the overview names other tasks than its section")
    sizes = collections.Counter(c[4] for c in task_cells.values() if len(c) == 6)
    ok &= report(f"tasks: {len(task_cells)}; sizes: {dict(sorted(sizes.items()))}; shape, order and dependencies", problems)

    # --- each criterion belongs to exactly one task
    ac_def = defined(fs, r"AC-[A-Z0-9]+")
    owner = collections.defaultdict(list)
    for t, c in task_cells.items():
        for a in expand(c[5] if len(c) == 6 else "", r"AC-[A-Z0-9]+"):
            owner[a].append(t)
    none_task = [a for a in ac_def if a not in owner]
    many = {a: sorted(ts) for a, ts in owner.items() if len(ts) > 1}
    print(f"criteria with exactly one task: {len([a for a in ac_def if len(owner.get(a, [])) == 1])} of {len(ac_def)}; "
          f"without a task: {none_task or 'none'}; with several tasks: {many or 'none'}")
    ok &= not none_task and not many

    # --- a criterion with the mark "P1:" belongs to a phase of version 1.0 (D-102)
    late = {p for p, c in ph_rows.items() if len(c) == 5 and c[3] == "1.0"}
    marked = [c[0] for c in ac_rows if c[1].startswith("P1:")]
    early_p1 = sorted(a for a in marked if any(task_phase[t] not in late for t in owner.get(a, [])))
    print(f"criteria with the mark P1: {len(marked)}; phases of version 1.0: {len(late)}; "
          f"marked criteria in an earlier phase: {early_p1 or 'none'}")
    ok &= bool(late) and not early_p1

    # --- a criterion with a manual test: the gate of its phase must list the test
    problems = []
    for c in ac_rows:
        mts = [t.strip() for t in c[2].split(",") if t.strip() in mt_def]
        for a_task in owner.get(c[0], []):
            g = "GATE-" + task_phase[a_task][3:]
            miss = [m for m in mts if m not in gates.get(g, {}).get("must", set())]
            if miss:
                problems.append(f"{c[0]} ({a_task}): {g} does not list {miss}")
    never = sorted(m for m in mt_def if not any(m in g["must"] for g in gates.values()))
    if never:
        problems.append(f"manual tests that must pass at no gate: {never}")
    ok &= report("manual tests at the gates of their criteria", problems)

    # --- each check V is a task of its home phase and has a planned answer.
    # V-01 to V-07 run before the MVP, in PH-00. V-08 runs in the first phase of version 1.1 (D-112).
    problems = []
    v11 = sorted(p for p, c in ph_rows.items() if len(c) == 5 and c[3] == "1.1")
    V_HOME = {"V-08": v11[0] if v11 else "?"}
    answer_rows = set(re.findall(r"^\| (V-\d{2}) \|", rm, flags=re.M))
    for v in sorted(v_def):
        home = V_HOME.get(v, "PH-00")
        inside = " ".join(" ".join(c) for t, c in task_cells.items() if task_phase[t] == home)
        outside = " ".join(" ".join(c[:2]) for t, c in task_cells.items() if task_phase[t] != home)
        if not re.search(r"Check " + v + r"\b", inside):
            problems.append(f"{v} is no task of {home}")
        if re.search(r"Check " + v + r"\b", outside):
            problems.append(f"{v} is planned as a task outside {home}")
        if v not in answer_rows:
            problems.append(f"{v} has no row with a planned answer")
    ok &= report(f"checks V as tasks of their home phase (V-08 in {V_HOME['V-08']}, all others in PH-00)", problems)

    # --- requirements that feature_specifications.md leaves to this document
    left = set()
    m10 = re.search(r"^## 10\. Requirements without an acceptance criterion.*?(?=^## )", fs, flags=re.M | re.S)
    for l in (m10.group(0).splitlines() if m10 else []):
        if l.startswith("|") and "`development_roadmap.md`" in cells_of(l)[-1]:
            left |= expand(l)
    miss = sorted(left - expand(rm))
    print(f"requirements left to development_roadmap.md: {len(left)}; not cited there: {miss or 'none'}")
    ok &= bool(left) and not miss

    # --- the two derived tables equal their sources
    problems = []
    for l in rm.splitlines():
        if re.match(r"\| MT-\d{2} ", l):
            c = cells_of(l)
            must = {g for g, v in gates.items() if c[0] in v["must"]}
            early = {g for g, v in gates.items() if c[0] in v["early"]}
            if expand(c[2], "GATE") != must or expand(c[3], "GATE") != early:
                problems.append(f"row {c[0]} of Section 8.1 differs from Section 7.2")
    listed = set(re.findall(r"^\| (MT-\d{2}) ", rm, flags=re.M))
    if listed != mt_def:
        problems.append("Section 8.1 does not list every manual test once")
    for l in rm.splitlines():
        g = re.match(r"\| `(AC-[A-Z0-9]+)` ", l)
        if g:
            c = cells_of(l)
            tasks = {t for a, ts in owner.items() if a.startswith(g.group(1) + "-") for t in ts}
            count = len([a for a in ac_def if a.startswith(g.group(1) + "-")])
            if expand(c[3], "TK") != tasks or c[2] != str(count):
                problems.append(f"row {g.group(1)} of Section 9.2 differs from Section 6")
    groups_listed = set(re.findall(r"^\| `(AC-[A-Z0-9]+)` ", rm, flags=re.M))
    if groups_listed != {re.match(r"(AC-[A-Z0-9]+)-\d+", a).group(1) for a in ac_def}:
        problems.append("Section 9.2 does not list every group of criteria")
    ok &= report("derived tables of the roadmap (Sections 8.1 and 9.2)", problems)

if rm and rk:
    LEVEL = {"Low": 1, "Medium": 2, "High": 3}
    LABELS = ["Rating:", "Causes:", "Effects:", "Mitigations in the design:", "Mitigations in the plan:",
              "Early signs:", "Answers if it happens:", "Watched by:"]
    over = {cells_of(l)[0]: cells_of(l) for l in rk.splitlines() if re.match(r"\| RISK-\d{2} ", l)}
    sections = {}
    for m in re.finditer(r"^### (\d+\.\d+) (RISK-\d{2}): [^\n]*\n(.*?)(?=^### |^## |^---$|\Z)", rk, flags=re.M | re.S):
        sections[m.group(2)] = (m.group(1), m.group(3))
    problems = []
    watch_v = collections.defaultdict(set)
    for r, c in sorted(over.items()):
        if len(c) != 6 or c[2] not in LEVEL or c[3] not in LEVEL or c[4] != str(LEVEL.get(c[2], 0) * LEVEL.get(c[3], 0)):
            problems.append(f"{r}: wrong row in the overview")
            continue
        if r not in sections:
            problems.append(f"{r}: no section")
            continue
        number, body = sections[r]
        pos = [body.find(lab) for lab in LABELS]
        if -1 in pos or pos != sorted(pos):
            problems.append(f"{r}: fields missing or out of order")
            continue
        if f"Rating: likelihood {c[2].lower()}, weight {c[3].lower()}." not in body:
            problems.append(f"{r}: the rating differs from the overview")
        w = re.search(r"^Watched by: (.*?)\. Phases: (.*)\.$", body, flags=re.M)
        if not w:
            problems.append(f"{r}: line 'Watched by' has a wrong form")
            continue
        seen = set(re.findall(r"\bV-\d{2}\b", w.group(1))) | expand(w.group(1), "MT")
        listed = set(re.findall(r"\bV-\d{2}\b", c[5])) | expand(c[5], "MT")
        if seen != listed:
            problems.append(f"{r}: 'Watched by' differs from the overview")
        phases = expand(w.group(2), "PH")
        if phases != {p for p, rs in risks_of.items() if r in rs}:
            problems.append(f"{r}: its phases differ from the rows 'Risks to watch' of the roadmap")
        for mt in expand(w.group(1), "MT"):
            if not any(mt in gates["GATE-" + p[3:]]["must"] | gates["GATE-" + p[3:]]["early"] for p in phases if "GATE-" + p[3:] in gates):
                problems.append(f"{r}: no gate of its phases runs {mt}")
        for v in re.findall(r"\bV-\d{2}\b", w.group(1)):
            watch_v[v].add(r)
        if int(r[5:]) <= 11 and number != f"3.{int(r[5:])}":
            problems.append(f"{r}: a risk of the brief must be Section 3.{int(r[5:])}")
    for l in rk.splitlines():
        v = re.match(r"\| (V-\d{2}) \|", l)
        if v and expand(cells_of(l)[2], "RISK") != watch_v.get(v.group(1), set()):
            problems.append(f"row {v.group(1)} of the table of the checks differs from the lines 'Watched by'")
    unknown_risk = sorted(set().union(*risks_of.values()) - set(over)) if risks_of else []
    if unknown_risk:
        problems.append(f"the roadmap watches unknown risks: {unknown_risk}")
    ok &= report(f"risks: {len(over)}; overview, fields, ratings, phases and tests", problems)
    ok &= len([r for r in over if int(r[5:]) <= 11]) == 11

    d5_rm = {f"D-{n:03d}" for n in range(92, 97)}
    d5_rk = {"D-097"}
    print("Stage 5 decisions missing in development_roadmap.md:", sorted(d5_rm - expand(rm)) or "none",
          "| missing in risk_assessment.md:", sorted(d5_rk - expand(rk)) or "none",
          "| missing in project_decisions.md:", sorted((d5_rm | d5_rk) - set(dec_def)) or "none")
    ok &= not (d5_rm - expand(rm)) and not (d5_rk - expand(rk)) and not ((d5_rm | d5_rk) - set(dec_def))

# ---------------------------------------------------------------- Stage 6: guide of the implementer
guide = docs.get("opus_implementation_guide.md", "")
if guide and rm and fs:
    # --- the table of the phases points to the right sections, tasks and gates of the roadmap
    problems = []
    g_rows = {cells_of(l)[0]: cells_of(l) for l in guide.splitlines() if re.match(r"\| PH-\d{2} ", l)}
    sec_of = {m.group(2): m.group(1) for m in re.finditer(r"^### (6\.\d+) (PH-\d{2})\b", rm, flags=re.M)}
    for p in ph_rows:
        c = g_rows.get(p)
        if not c or len(c) != 4:
            problems.append(f"{p}: no row with four cells")
            continue
        if c[1] != f"`development_roadmap.md` Section {sec_of.get(p)}":
            problems.append(f"{p}: wrong section of the roadmap")
        if expand(c[2], "TK") != {t for t, ph in task_phase.items() if ph == p}:
            problems.append(f"{p}: other tasks than in the roadmap")
        if expand(c[3], "GATE") != {"GATE-" + p[3:]}:
            problems.append(f"{p}: wrong gate")
    if set(g_rows) != set(ph_rows):
        problems.append("the rows differ from the phases of the roadmap")
    ok &= report(f"guide: table of the phases, {len(g_rows)} rows equal to the roadmap", problems)

    # --- the list of the open items equals the states in project_decisions.md
    problems = []
    status = {cells_of(l)[0]: cells_of(l)[2] for l in dec.splitlines() if re.match(r"\| D-\d{3} ", l)}
    open_q = {cells_of(l)[0] for l in dec.splitlines() if re.match(r"\| Q-\d{2} ", l) and "**Answered" not in l}
    want = {"Open questions": open_q,
            "Proposed decisions": {i for i, s in status.items() if s == "Proposed"},
            "Conditional decisions": {i for i, s in status.items() if s == "Conditional"}}
    m73 = re.search(r"^### 7\.3 .*?(?=^### |^## |^---$)", guide, flags=re.M | re.S)
    got = {cells_of(l)[0]: expand(cells_of(l)[1]) for l in (m73.group(0).splitlines() if m73 else []) if l.startswith("|")}
    for kind, ids in want.items():
        if got.get(kind) != ids:
            problems.append(f"{kind}: the guide names {sorted(got.get(kind, []))}, the log has {sorted(ids)}")
    ok &= report(f"guide: open items equal to project_decisions.md ({len(open_q)} questions, "
                 f"{len(want['Proposed decisions'])} Proposed, {len(want['Conditional decisions'])} Conditional)", problems)

    # --- requirements that feature_specifications.md leaves to the guide; decisions of Stage 6; the eight other documents
    problems = []
    left_guide = set()
    m10 = re.search(r"^## 10\. Requirements without an acceptance criterion.*?(?=^## )", fs, flags=re.M | re.S)
    for l in (m10.group(0).splitlines() if m10 else []):
        if l.startswith("|") and "`opus_implementation_guide.md`" in cells_of(l)[-1]:
            left_guide |= expand(l)
    if not left_guide or left_guide - expand(guide):
        problems.append(f"requirements left to the guide and not cited there: {sorted(left_guide - expand(guide)) or 'none are left'}")
    d6_guide = {"D-098", "D-099", "D-100"}
    d6_check = {"D-101", "D-102"}
    if d6_guide - expand(guide) or d6_check - expand(fs) or (d6_guide | d6_check) - set(dec_def):
        problems.append("a decision of Stage 6 is missing in the guide, in feature_specifications.md or in the log")
    row98 = next((l for l in dec.splitlines() if l.startswith("| D-098 ")), "")
    lost = [v for v in re.findall(r"`([^`]+)`", row98) if f"`{v}`" not in guide]
    if not row98 or lost:
        problems.append(f"values of D-098 that the guide does not name: {lost or 'no row'}")
    unnamed = [n for n in NAMES if n != "opus_implementation_guide.md" and f"`{n}`" not in guide]
    if unnamed:
        problems.append(f"documents that the guide does not name: {unnamed}")
    ok &= report("guide: requirement NFR-MNT-07, decisions of Stage 6, names of the other documents", problems)

    # --- the guide copies nothing: no sentence of eight or more words stands in another document too
    units, in_code = [], False
    for line in guide.splitlines():
        if line.startswith("```"):
            in_code = not in_code
            continue
        if in_code or line.startswith(("#", "**Project:**")) or re.match(r"^\|[-| ]+\|$", line):
            continue
        pieces = cells_of(line) if line.startswith("|") else [re.sub(r"^\d+\. ", "", line)]
        for piece in pieces:
            units += [s.strip() for s in re.split(r"(?<=[.!?:])\s+", piece) if len(s.split()) >= 8]
    copied = sorted({f"{s[:50]}... ({n})" for s in units for n, text in docs.items()
                     if n != "opus_implementation_guide.md" and s in text})
    print(f"guide: sentences of eight or more words: {len(units)}; also found in another document: {copied or 'none'}")
    ok &= not copied

# ---------------------------------------------------------------- version 1.1: the Decomposer (change 7.0)
if rm and fs and guide:
    problems = []
    # requirements with the priority V1.1
    v11_req = [cells_of(l)[0] for l in req.splitlines() if re.match(r"\| (?:FR|NFR)-[A-Z0-9]+-\d{2} ", l) and cells_of(l)[-1] == "V1.1"]
    if not v11_req:
        problems.append("no requirement has the priority V1.1")
    alien = [i for i in v11_req if not i.startswith("FR-DEC-")]
    if alien:
        problems.append(f"requirements with the priority V1.1 outside the group FR-DEC: {alien}")
    dec_pri = {cells_of(l)[0]: cells_of(l)[-1] for l in req.splitlines() if l.startswith("| FR-DEC-")}
    odd = sorted(i for i, pr in dec_pri.items() if pr not in ("V1.1", "P2"))
    if odd:
        problems.append(f"requirements of the group FR-DEC with another priority than V1.1 or P2: {odd}")
    # criteria of the group AC-DEC and phases of version 1.1
    ph11 = {p for p, c in ph_rows.items() if len(c) == 5 and c[3] == "1.1"}
    ph10 = {p for p, c in ph_rows.items() if len(c) == 5 and c[3] == "1.0"}
    if not ph11 or (ph10 and min(ph11) <= max(ph10)):
        problems.append("the phases of version 1.1 are missing or do not follow version 1.0")
    dec_acs = [a for a in ac_def if a.startswith("AC-DEC-")]
    early = sorted(a for a in dec_acs if any(task_phase[t] not in ph11 for t in owner.get(a, [])))
    if early:
        problems.append(f"criteria of the Decomposer outside version 1.1: {early}")
    stray11 = sorted(a for a, ts in owner.items() if not a.startswith("AC-DEC-") and any(task_phase[t] in ph11 for t in ts))
    if stray11:
        problems.append(f"other criteria inside version 1.1: {stray11}")
    by_dec = set()
    for c in ac_rows:
        if c[0].startswith("AC-DEC-") and len(c) == 4:
            by_dec |= expand(c[3])
    bare = sorted(i for i in v11_req if i not in by_dec)
    if bare:
        problems.append(f"requirements of version 1.1 without a criterion of the group AC-DEC: {bare}")
    # decisions of the change
    d7 = {f"D-{n:03d}" for n in range(103, 113)}
    cited = {
        "D-103": ("development_roadmap.md", "opus_implementation_guide.md"),
        "D-104": tuple(n for n in NAMES if n != "project_decisions.md"),
        "D-105": ("requirements.md", "feature_specifications.md"),
        "D-106": ("technology_stack.md", "architecture.md", "feature_specifications.md"),
        "D-107": ("technology_stack.md", "feature_specifications.md"),
        "D-108": ("technology_stack.md", "architecture.md", "feature_specifications.md"),
        "D-109": ("architecture.md", "feature_specifications.md"),
        "D-110": ("technology_stack.md", "feature_specifications.md"),
        "D-111": ("technology_stack.md",),
        "D-112": ("technology_stack.md", "development_roadmap.md"),
    }
    if d7 - set(dec_def):
        problems.append(f"decisions missing in the log: {sorted(d7 - set(dec_def))}")
    for d, where in sorted(cited.items()):
        lost = [n for n in where if d not in expand(docs.get(n, ""))]
        if lost:
            problems.append(f"{d} is not cited in {lost}")
    # the second screen in the UI specification, the result format, the record of the change
    if not re.search(r"^## 19\. Decomposer", ui, flags=re.M) or "FR-DEC-01" not in expand(ui):
        problems.append("ui_ux_specification.md has no section for the Decomposer")
    if not re.search(r"^### 6\.11 Result of a Decomposition job", fs, flags=re.M):
        problems.append("feature_specifications.md has no format of a result")
    change_rows = re.findall(r"^\| Change (\d+\.\d+): [^|]+\|[^|]+\| ([^|]+) \|$", dec, flags=re.M)
    if not any(v == "7.0" and state.strip().startswith("Done") for v, state in change_rows):
        problems.append("project_decisions.md has no finished row for the change 7.0")
    ok &= report(f"version 1.1: {len(v11_req)} requirements with the priority V1.1, {len(dec_acs)} criteria of the Decomposer, "
                 f"{len(ph11)} phases, decisions D-103 to D-112", problems)

# ---------------------------------------------------------------- stages and versions
stage_rows = re.findall(r"^\| (\d)\. [^|]+\|[^|]+\| ([^|]+) \|$", dec, flags=re.M)
print("stages:", "; ".join(f"{n}: {state.strip()[:30]}" for n, state in stage_rows))
not_done = [n for n, state in stage_rows if not state.strip().startswith("Done")]
print(f"stages marked as done: {len(stage_rows) - len(not_done)} of {len(stage_rows)}; not done: {not_done or 'none'}")
ok &= len(stage_rows) == 6 and not not_done

no_change_line = []
for n in NAMES:
    if n in docs:
        v = re.search(r"\*\*Version:\*\* ([0-9.]+)", docs[n])
        if v and v.group(1) != "1.0" and f"**Change in {v.group(1)}:**" not in docs[n]:
            no_change_line.append(n)
print("documents above version 1.0 without a line 'Change in' for their version:", no_change_line or "none")
ok &= not no_change_line

if rm and fs:
    cont = re.search(r"^\*\*Continuation point\.\*\*.*?(?=^---$)", dec, flags=re.M | re.S)
    next_task = re.search(r"^\| Next task \| (TK-\d{3}) \|$", cont.group(0), flags=re.M) if cont else None
    state = "missing"
    if next_task:
        state = next_task.group(1) if next_task.group(1) in task_cells else next_task.group(1) + " (unknown task)"
    print("continuation point: next task:", state)
    ok &= bool(next_task) and next_task.group(1) in task_cells

# ---------------------------------------------------------------- counts
groups = collections.Counter(re.match(r"(.*)-\d+$", i).group(1) for i in req_def + dec_def)
print("counts:", dict(sorted(groups.items())))
pri = collections.Counter(re.findall(r"\|\s*(P0|P1|V1\.1|P2)\s*\|\s*$", req, flags=re.M))
print("functional requirements by priority:", dict(pri))
print("documents (name, version, lines):")
for n in NAMES:
    if n in docs:
        v = re.search(r"\*\*Version:\*\* ([0-9.]+)", docs[n])
        print(f"  {n} | {v.group(1) if v else '?'} | {len(docs[n].splitlines())}")
        ok &= bool(v)
    else:
        print(f"  {n} | not written yet | —")
print("RESULT:", "OK" if ok else "PROBLEMS FOUND")
