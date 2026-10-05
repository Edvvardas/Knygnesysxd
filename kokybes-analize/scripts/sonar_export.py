"""Eksportuoja SonarQube rezultatus (projekto/failų metrikos, radiniai) į JSON failus.
Naudojimas: python sonar_export.py <rezultatu_katalogas> <token>"""
import base64
import json
import sys
import urllib.request

OUT, TOKEN = sys.argv[1], sys.argv[2]
BASE = "http://localhost:9000"
AUTH = "Basic " + base64.b64encode(f"{TOKEN}:".encode()).decode()

PROJECT_METRICS = ("ncloc,lines,functions,classes,complexity,cognitive_complexity,sqale_rating,sqale_index,"
                   "sqale_debt_ratio,code_smells,bugs,reliability_rating,vulnerabilities,security_rating,"
                   "security_hotspots,coverage,line_coverage,branch_coverage,duplicated_lines_density,"
                   "comment_lines_density")
FILE_METRICS = "ncloc,complexity,cognitive_complexity,sqale_rating,sqale_index,code_smells,coverage"


def get(path):
    req = urllib.request.Request(BASE + path, headers={"Authorization": AUTH})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def dump(name, data):
    with open(f"{OUT}/{name}", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)


dump("sonar-projektas.json", get(f"/api/measures/component?component=knygnesys&metricKeys={PROJECT_METRICS}"))
dump("sonar-failai.json", get(f"/api/measures/component_tree?component=knygnesys&qualifiers=FIL"
                              f"&metricKeys={FILE_METRICS}&ps=500"))
dump("sonar-issues.json", get("/api/issues/search?componentKeys=knygnesys&ps=500&statuses=OPEN,CONFIRMED"))
print("Sonar rezultatai eksportuoti.")
