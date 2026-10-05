"""Sujungia visų įrankių rezultatus į vieną Markdown suvestinę (rezultatai/suvestine.md).
Naudojimas: python summarize.py <rezultatu_katalogas>"""
import csv
import json
import os
import re
import statistics
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

OUT = sys.argv[1]
SRC = os.path.normpath(os.path.join(OUT, "..", "..", "knygnesys-app", "src", "main", "java"))
PKG = "lt.prifkodas.knygnesys."
SEP = "knygnesys" + os.sep


def p(path):
    return os.path.join(OUT, path)


def short(path):
    """Kelias -> 'stats/service/StatsService.java'."""
    path = path.replace("\\", "/")
    return path.split("lt/prifkodas/knygnesys/")[-1].lstrip("./")


def table(headers, rows):
    lines = ["| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    lines += ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    return "\n".join(lines) + "\n"


print("# Kodo kokybės analizės suvestinė (sugeneruota automatiškai)\n")
print("Šaltinis: `kokybes-analize/rezultatai/`. Sugeneruota `scripts/summarize.py`.\n")

# ---------- 1. Metodų sudėtingumas: PMD + lizard ----------
pmd = {}
for line in open(p("pmd-sudetingumas.txt"), encoding="utf-8"):
    parts = line.rstrip("\n").split("\t")
    if len(parts) < 3:
        continue
    file_line = parts[0]
    m = re.search(r"The method '([^(]+)\(.*?has (?:a|an) (cyclomatic|cognitive|NPath) complexity of ([\d,]+)", parts[2])
    if not m:
        continue
    f, ln = short(file_line.rsplit(":", 2)[0]), file_line.rsplit(":", 2)[1]
    pmd.setdefault((f, int(ln), m.group(1)), {})[m.group(2)] = int(m.group(3).replace(",", ""))

lizard = {}
for line in open(p("lizard.txt"), encoding="utf-8"):
    m = re.match(r"\s*(\d+)\s+(\d+)\s+\d+\s+\d+\s+\d+\s+(\w+)::(\w+)@(\d+)-\d+@(.*)$", line)
    if m:
        lizard[(short(m.group(6)), int(m.group(5)))] = (int(m.group(2)), int(m.group(1)))

rows = []
for (f, ln, name), v in sorted(pmd.items(), key=lambda kv: (-kv[1].get("cyclomatic", 1), -kv[1].get("cognitive", 0))):
    liz = lizard.get((f, ln), ("-", "-"))
    rows.append((f"`{f.split('/')[-1].replace('.java', '')}.{name}()`", f"{f}:{ln}",
                 v.get("cyclomatic", 1), v.get("cognitive", 0), v.get("NPath", 1), liz[0], liz[1]))
print("## 1. Metodų sudėtingumas (PMD 7.17 + lizard 1.24)\n")
print(f"Metodų iš viso (PMD): {len(rows)}. Žemiau – metodai, kurių CC ≥ 3 arba Cognitive ≥ 3.\n")
print(table(["Metodas", "Vieta", "CC (PMD)", "Cognitive (PMD)", "NPath (PMD)", "CC (lizard)", "NLOC"],
            [r for r in rows if r[2] >= 3 or r[3] >= 3]))
ccs = [r[2] for r in rows]
print(f"CC pasiskirstymas: vidurkis {statistics.mean(ccs):.2f}, mediana {statistics.median(ccs)}, "
      f"CC=1: {sum(c == 1 for c in ccs)}, CC 2–5: {sum(2 <= c <= 5 for c in ccs)}, "
      f"CC 6–10: {sum(6 <= c <= 10 for c in ccs)}, CC > 10: {sum(c > 10 for c in ccs)}\n")

# ---------- 2. Prižiūrimumas ----------
print("## 2. Prižiūrimumo indeksas (multimetric 2.4.4)\n")
ms = json.load(open(p("multimetric-microsoft.json"), encoding="utf-8"))["files"]
sei = json.load(open(p("multimetric-sei.json"), encoding="utf-8"))["files"]
mi_rows = []
for f in ms:
    mi_rows.append((short(f), round(ms[f]["maintainability_index"], 1), round(sei[f]["maintainability_index"], 1),
                    ms[f]["loc"], round(ms[f]["halstead_volume"], 0)))
mi_rows.sort(key=lambda r: r[1])
vals = [r[1] for r in mi_rows]
print(f"Failų: {len(vals)}. MI (Microsoft, 0–100): vidurkis **{statistics.mean(vals):.1f}**, "
      f"mediana {statistics.median(vals):.1f}, min {min(vals)}. "
      f"Žalia (≥20): {sum(v >= 20 for v in vals)}, geltona (10–19): {sum(10 <= v < 20 for v in vals)}, "
      f"raudona (<10): {sum(v < 10 for v in vals)}.\n")
print(f"MI (SEI, su komentarais): vidurkis {statistics.mean(r[2] for r in mi_rows):.1f}\n")
print("10 žemiausio MI failų:\n")
print(table(["Failas", "MI (MS)", "MI (SEI)", "LOC", "Halstead V"], mi_rows[:10]))

if os.path.exists(p("sonar-projektas.json")):
    sp = {m["metric"]: m.get("value") for m in json.load(open(p("sonar-projektas.json"), encoding="utf-8"))["component"]["measures"]}
    rating = {"1.0": "A", "2.0": "B", "3.0": "C", "4.0": "D", "5.0": "E"}
    print("### SonarQube (Community 26.9) projekto metrikos\n")
    print(table(["Metrika", "Reikšmė"], [
        ("Maintainability rating", rating.get(sp.get("sqale_rating"), sp.get("sqale_rating"))),
        ("Technical debt (min)", sp.get("sqale_index")),
        ("Debt ratio (%)", sp.get("sqale_debt_ratio")),
        ("Code smells", sp.get("code_smells")),
        ("Reliability rating / Bugs", f"{rating.get(sp.get('reliability_rating'))} / {sp.get('bugs')}"),
        ("Security rating / Vulnerabilities", f"{rating.get(sp.get('security_rating'))} / {sp.get('vulnerabilities')}"),
        ("Cyclomatic complexity (suma)", sp.get("complexity")),
        ("Cognitive complexity (suma)", sp.get("cognitive_complexity")),
        ("NCLOC / klasės / metodai", f"{sp.get('ncloc')} / {sp.get('classes')} / {sp.get('functions')}"),
        ("Coverage (line / branch)", f"{sp.get('coverage')}% ({sp.get('line_coverage')}% / {sp.get('branch_coverage')}%)"),
        ("Dublikuotos eilutės (%)", sp.get("duplicated_lines_density")),
        ("Komentarų tankis (%)", sp.get("comment_lines_density")),
    ]))
    sf = json.load(open(p("sonar-failai.json"), encoding="utf-8"))["components"]
    srows = []
    for c in sf:
        m = {x["metric"]: x.get("value") for x in c["measures"]}
        srows.append((short(c["path"]), m.get("ncloc"), m.get("complexity"), m.get("cognitive_complexity"),
                      m.get("code_smells", "0"), m.get("sqale_index", "0"), m.get("coverage", "-")))
    srows.sort(key=lambda r: -int(r[3] or 0))
    print("Sonar failų lygio metrikos (pagal cognitive complexity, top 8):\n")
    print(table(["Failas", "NCLOC", "CC", "Cognitive", "Code smells", "Debt (min)", "Coverage %"], srows[:8]))

# ---------- 3. OO metrikos: CK ----------
print("## 3. Objektinės metrikos (CK 0.7.0)\n")
ck = list(csv.DictReader(open(p("ck/class.csv"), encoding="utf-8")))
ck.sort(key=lambda x: -int(x["wmc"]))
print(table(["Klasė", "Tipas", "WMC", "CBO", "Fan-in", "Fan-out", "RFC", "DIT", "NOC", "LCOM", "LCOM*", "TCC", "LOC"],
            [(x["class"].replace(PKG, ""), x["type"], x["wmc"], x["cbo"], x["fanin"], x["fanout"], x["rfc"], x["dit"],
              x["noc"], x["lcom"], round(float(x["lcom*"]), 2) if x["lcom*"] not in ("NaN", "") else "-",
              round(float(x["tcc"]), 2) if x["tcc"] not in ("NaN", "") else "-", x["loc"])
             for x in ck if x["type"] == "class"]))
classes = [x for x in ck if x["type"] == "class"]
print(f"Klasių: {len(classes)}, interfeisų: {sum(x['type'] == 'interface' for x in ck)}. "
      f"Max DIT = {max(int(x['dit']) for x in ck)}, NOC > 0: {sum(int(x['noc']) > 0 for x in ck)} klasės. "
      f"Vid. CBO (klasės) = {statistics.mean(int(x['cbo']) for x in classes):.1f}.\n")

# ---------- 4. Paketų priklausomybės (iš import sakinių) ----------
print("## 4. Paketų (modulių) priklausomybės: Ca, Ce, nestabilumas I = Ce/(Ca+Ce)\n")
deps = defaultdict(set)
for root, _, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith(".java"):
            continue
        rel = os.path.relpath(os.path.join(root, fn), SRC).replace(os.sep, "/")
        parts = rel.split("/")
        module = parts[3] if len(parts) > 4 else "(root)"
        for line in open(os.path.join(root, fn), encoding="utf-8"):
            m = re.match(r"import (?:static )?lt\.prifkodas\.knygnesys\.(\w+)", line)
            if m and m.group(1) != module and not m.group(1)[0].isupper():
                deps[module].add(m.group(1))
modules = sorted(set(deps) | {d for v in deps.values() for d in v})
ca = Counter(d for v in deps.values() for d in v)
mrows = []
for mname in modules:
    ce = len(deps.get(mname, ()))
    i = ce / (ca[mname] + ce) if ca[mname] + ce else 0
    mrows.append((mname, ca[mname], ce, f"{i:.2f}", ", ".join(sorted(deps.get(mname, ()))) or "–"))
print(table(["Modulis", "Ca (fan-in)", "Ce (fan-out)", "I", "Priklauso nuo"], mrows))

# ---------- 5. Statinė analizė ----------
print("## 5. Statinės analizės radinių suvestinė\n")
pmd_rules = Counter()
for line in open(p("pmd-statine.txt"), encoding="utf-8"):
    parts = line.split("\t")
    if len(parts) >= 3:
        pmd_rules[parts[1].rstrip(":")] += 1
print(f"### PMD (quickstart + design): {sum(pmd_rules.values())} radiniai\n")
print(table(["Taisyklė", "Kiekis"], pmd_rules.most_common()))

sb = ET.parse(p("spotbugs.xml")).getroot().findall("BugInstance")
sb_types = Counter((b.get("category"), b.get("type")) for b in sb)
print(f"### SpotBugs 4.9.3: {len(sb)} radiniai\n")
print(table(["Kategorija", "Tipas", "Kiekis"], [(c, t, n) for (c, t), n in sb_types.most_common()]))

if os.path.exists(p("sonar-issues.json")):
    issues = json.load(open(p("sonar-issues.json"), encoding="utf-8"))["issues"]
    rules = Counter((i.get("severity"), i.get("type"), i["rule"]) for i in issues)
    print(f"### SonarQube: {len(issues)} radiniai\n")
    print(table(["Svarba", "Tipas", "Taisyklė", "Kiekis"], [(s, t, r, n) for (s, t, r), n in rules.most_common()]))

# ---------- 6. Padengimas ----------
print("## 6. Testų padengimas (JaCoCo 0.8.13)\n")
jac = list(csv.DictReader(open(p("jacoco.csv"), encoding="utf-8")))
jrows = []
for r in jac:
    li = int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])
    br = int(r["BRANCH_MISSED"]) + int(r["BRANCH_COVERED"])
    if "service" in r["PACKAGE"]:
        jrows.append((r["PACKAGE"].replace(PKG, "") + "." + r["CLASS"],
                      f"{100 * int(r['LINE_COVERED']) / li:.0f}%" if li else "-",
                      f"{100 * int(r['BRANCH_COVERED']) / br:.0f}%" if br else "-"))
print(table(["Servisas", "Eilučių padengimas", "Šakų padengimas"], sorted(jrows)))
