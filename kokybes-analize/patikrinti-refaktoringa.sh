#!/usr/bin/env bash
# Patikrina siūlomą StatsService refaktoringą NEKEIČIANT originalaus kodo:
# nukopijuoja knygnesys-app į laikiną katalogą, pakeičia StatsService.java į pasiulymai/StatsService.java,
# paleidžia StatsServiceTest ir PMD/CK/multimetric metrikas abiem versijoms.
# Naudojimas (Git Bash):   cd kokybes-analize && ./patikrinti-refaktoringa.sh
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$HERE/tools"
export PYTHONIOENCODING=utf-8
JDK_DIR="$(ls -d "$TOOLS"/jdk-21* 2>/dev/null | head -1 || true)"
if [ -n "$JDK_DIR" ]; then export JAVA_HOME="$JDK_DIR"; export PATH="$JAVA_HOME/bin:$PATH"; fi

TMP="$(mktemp -d)"
REL="src/main/java/lt/prifkodas/knygnesys/stats/service"
cp -r "$HERE/../knygnesys-app" "$TMP/app"
rm -rf "$TMP/app/target"
cp "$HERE/pasiulymai/StatsService.java" "$TMP/app/$REL/StatsService.java"

echo "== Testai su refaktoruota versija =="
(cd "$TMP/app" && mvn -q test -Dtest=StatsServiceTest > "$TMP/test.log" 2>&1 || true)
grep -h "Tests run" "$TMP/app/target/surefire-reports/"*.txt || { tail -30 "$TMP/test.log"; exit 1; }

measure() {  # $1 = pavadinimas, $2 = katalogas su StatsService.java
  echo "== $1 =="
  "$TOOLS/pmd-bin-7.17.0/bin/pmd" check -d "$2/StatsService.java" -R "$HERE/config/pmd-sudetingumas.xml" \
    -f text --no-cache --no-progress 2>/dev/null \
    | grep -E "total cyclomatic|(cyclomatic|cognitive) complexity of ([6-9]|[1-9][0-9])" \
    | sed -E 's/.*StatsService.java:/  eil. /' || true   # PMD grąžina 4, kai randa pažeidimų
  (cd "$2" && python "$HERE/scripts/run_multimetric.py" --jobs 1 --maintindex microsoft StatsService.java) \
    | python -c "import sys,json; v=list(json.load(sys.stdin)['files'].values())[0]; print(f\"  MI(MS)={v['maintainability_index']:.1f}  LOC={v['loc']}  HalsteadV={v['halstead_volume']:.0f}\")"
  mkdir -p "$TMP/ck-$1" && (cd "$TMP/ck-$1" && java -jar "$TOOLS/ck.jar" "$2" false 0 false "$TMP/ck-$1/" >/dev/null 2>&1)
  python -c "import csv,sys; r=next(csv.DictReader(open(sys.argv[1]))); print(f\"  CK: WMC={r['wmc']} CBO={r['cbo']} RFC={r['rfc']} LCOM*={float(r['lcom*']):.2f} LOC={r['loc']}\")" "$TMP/ck-$1/class.csv"
}
measure "ORIGINALAS" "$HERE/../knygnesys-app/$REL"
measure "REFAKTORUOTA" "$TMP/app/$REL"
rm -rf "$TMP"
