"""The command line of the Toolkit: python toolkit.pyz PROGRAM OPTIONS.

Every program prints exactly one line of JSON and ends with the exit code 0. An error is reported
inside the JSON as "ok": false and "error" (feature_specifications.md Section 5.2.5, rule 1).
No program uses the network (rule 2).
"""

import argparse
import json
import sys

from . import VERSION
from . import report as rp
from .errors import ToolkitError

OUTPUTS = "/mnt/session/outputs"
MODEL = "/mnt/session/uploads/" + rp.MODEL_FILE
MODEL_RATE = 44100


class Parser(argparse.ArgumentParser):
    def error(self, message):
        raise ToolkitError(message)


def parser():
    p = Parser(prog="toolkit.pyz", add_help=False)
    sub = p.add_subparsers(dest="program", parser_class=Parser)
    c = sub.add_parser("convert", add_help=False)
    c.add_argument("--in", dest="input", required=True)
    c.add_argument("--out", required=True)
    s = sub.add_parser("separate", add_help=False)
    s.add_argument("--in", dest="input", required=True)
    s.add_argument("--outdir", required=True)
    s.add_argument("--model", default=MODEL)
    d = sub.add_parser("describe", add_help=False)
    d.add_argument("--in", dest="input", required=True)
    d.add_argument("--start", type=float)
    d.add_argument("--end", type=float)
    d.add_argument("--picture")
    n = sub.add_parser("narrow", add_help=False)
    n.add_argument("--in", dest="input", required=True)
    n.add_argument("--out", required=True)
    n.add_argument("--start", type=float)
    n.add_argument("--end", type=float)
    n.add_argument("--component", choices=("harmonic", "percussive"))
    n.add_argument("--band", type=float, nargs=2, metavar=("LOW", "HIGH"))
    n.add_argument("--onset-nearest", dest="nearest", type=float)
    n.add_argument("--gate-db", dest="gate", type=float)
    f = sub.add_parser("finish", add_help=False)
    f.add_argument("--in", dest="input", required=True)
    f.add_argument("--out", required=True)
    q = sub.add_parser("quick", add_help=False)
    q.add_argument("--class", dest="kind", required=True, choices=rp.STEMS)
    q.add_argument("--in", dest="input", required=True)
    q.add_argument("--start", type=float, required=True)
    q.add_argument("--end", type=float, required=True)
    q.add_argument("--outdir", default=OUTPUTS)
    q.add_argument("--model", default=MODEL)
    r = sub.add_parser("report", add_help=False)
    r.add_argument("--json", dest="draft", required=True)
    r.add_argument("--outdir", default=OUTPUTS)
    r.add_argument("--model", default=MODEL)
    return p


def convert(a):
    from . import audio as au

    audio, rate = au.read(a.input)
    audio = au.resample(au.channels(audio, 2), rate, MODEL_RATE)
    au.write(a.out, audio, MODEL_RATE)
    return {"length_s": round(audio.shape[1] / MODEL_RATE, 4), "rate": MODEL_RATE, "channels": 2,
            "peak_db": au.peak_db(audio), "mean_db": au.mean_db(audio)}


def describe(a):
    from . import audio as au
    from . import measure

    audio, rate = au.read(a.input)
    part, offset = au.span(audio, rate, a.start, a.end)
    result = measure.describe(part, rate, offset)
    if a.picture:
        measure.write_png(a.picture, part, rate)
        result["picture"] = a.picture
    return result


def narrow(a):
    from . import audio as au
    from . import measure
    from . import narrow as nw

    audio, rate = au.read(a.input)
    part, offset = nw.narrow(audio, rate, a.start, a.end, a.component, a.band, a.nearest, a.gate)
    au.write(a.out, part, rate)
    return measure.describe(part, rate, offset)


def finish(a):
    from . import audio as au
    from . import narrow as nw

    audio, rate = au.read(a.input)
    out, out_rate = nw.finish(audio, rate)
    au.write(a.out, out, out_rate)
    return {"length_s": round(out.shape[1] / out_rate, 4), "peak_db": au.peak_db(out), "rate": out_rate}


def report(a):
    with open(a.draft, encoding="utf-8") as f:
        try:
            draft = json.load(f)
        except json.JSONDecodeError as e:
            raise ToolkitError(f"the draft is no valid JSON: {e.msg}") from None
    return rp.build(draft, a.outdir, a.model)


def not_yet(a):
    raise ToolkitError(f"the program {a.program} comes with the next version of the Toolkit (TK-011b)")


PROGRAMS = {"convert": convert, "separate": not_yet, "describe": describe, "narrow": narrow,
            "finish": finish, "quick": not_yet, "report": report}


def run(argv):
    try:
        args = parser().parse_args(argv)
        if not args.program:
            raise ToolkitError("name a program: " + ", ".join(PROGRAMS))
        result = PROGRAMS[args.program](args)
        out = {"ok": True, "program": args.program, "toolkit": VERSION}
        out.update(result)
    except ToolkitError as e:
        out = {"ok": False, "toolkit": VERSION, "error": str(e)[:300]}
    except Exception as e:  # noqa: BLE001 - every error must reach the Orchestrator as JSON
        out = {"ok": False, "toolkit": VERSION, "error": f"{type(e).__name__}: {e}"[:300]}
    return out


def main(argv=None):
    print(json.dumps(run(sys.argv[1:] if argv is None else argv), ensure_ascii=False, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
