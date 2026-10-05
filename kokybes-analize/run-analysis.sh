#!/usr/bin/env bash
# Paleidžia visus kodo kokybės analizės įrankius Knygnesys backend'ui.
# Naudojimas (Git Bash):   cd kokybes-analize && ./run-analysis.sh
# Rezultatai rašomi į kokybes-analize/rezultatai/
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
APP="$HERE/../knygnesys-app"
SRC="$APP/src/main/java"
OUT="$HERE/rezultatai"
TOOLS="$HERE/tools"
mkdir -p "$OUT"
export PYTHONIOENCODING=utf-8

# Projektas skirtas Java 21; PMD tipų rezoliucija ir Mockito nepalaiko JDK 26 klasių failų, todėl naudojam JDK 21.
JDK_DIR="$(ls -d "$TOOLS"/jdk-21* 2>/dev/null | head -1 || true)"
if [ -n "$JDK_DIR" ]; then
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
java -version 2>&1 | head -1

echo "== 1. Build + testai + JaCoCo padengimas =="
(cd "$APP" && mvn -q clean \
  org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test \
  org.jacoco:jacoco-maven-plugin:0.8.13:report \
  dependency:build-classpath -Dmdep.outputFile="$OUT/classpath.txt")
cp "$APP/target/site/jacoco/jacoco.csv" "$OUT/jacoco.csv"
AUX="$(cat "$OUT/classpath.txt");$APP/target/classes"

PMD="$TOOLS/pmd-bin-7.17.0/bin/pmd"
echo "== 2. PMD: sudėtingumo metrikos (CC, Cognitive, NPath kiekvienam metodui) =="
"$PMD" check -d "$SRC" -R "$HERE/config/pmd-sudetingumas.xml" --aux-classpath "$AUX" \
  -f text -r "$OUT/pmd-sudetingumas.txt" --no-cache --no-progress || true

echo "== 3. PMD: statinė analizė (quickstart + design) =="
"$PMD" check -d "$SRC" -R "$HERE/config/pmd-statine.xml" --aux-classpath "$AUX" \
  -f text -r "$OUT/pmd-statine.txt" --no-cache --no-progress || true
"$PMD" check -d "$SRC" -R "$HERE/config/pmd-statine.xml" --aux-classpath "$AUX" \
  -f html -r "$OUT/pmd-statine.html" --no-cache --no-progress || true

echo "== 4. SpotBugs =="
(cd "$APP" && mvn -q com.github.spotbugs:spotbugs-maven-plugin:4.9.3.0:spotbugs \
  -Dspotbugs.effort=Max -Dspotbugs.threshold=Low -Dspotbugs.xmlOutput=true || true)
cp "$APP/target/spotbugsXml.xml" "$OUT/spotbugs.xml" 2>/dev/null || true
python "$HERE/scripts/spotbugs_summary.py" "$OUT/spotbugs.xml" > "$OUT/spotbugs.txt" || true

echo "== 5. CK: objektinės metrikos (WMC, CBO, RFC, DIT, NOC, LCOM...) =="
mkdir -p "$OUT/ck"
(cd "$OUT/ck" && java -jar "$TOOLS/ck.jar" "$SRC" false 0 false "$OUT/ck/" >/dev/null 2>&1)

echo "== 6. multimetric: Maintainability Index, Halstead =="
# Kelyje yra tarpų, todėl leidžiam iš $SRC su santykiniais keliais.
(
  cd "$SRC"
  mapfile -t JAVA_FILES < <(find . -name '*.java' | sort)
  python "$HERE/scripts/run_multimetric.py" --jobs 1 --maintindex microsoft "${JAVA_FILES[@]}" > "$OUT/multimetric-microsoft.json"
  python "$HERE/scripts/run_multimetric.py" --jobs 1 --maintindex sei "${JAVA_FILES[@]}" > "$OUT/multimetric-sei.json"
)

echo "== 7. lizard: CC kryžminė patikra =="
python -m lizard -l java "$SRC" > "$OUT/lizard.txt" || true

echo "== 8. SonarQube (jei serveris paleistas: docker start sonarqube-knygnesys) =="
if [ -f "$TOOLS/sonar-token.txt" ] && curl -s localhost:9000/api/system/status | grep -q '"UP"'; then
  (cd "$APP" && mvn -q org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
    -Dsonar.host.url=http://localhost:9000 -Dsonar.token="$(cat "$TOOLS/sonar-token.txt")" \
    -Dsonar.projectKey=knygnesys -Dsonar.projectName=Knygnesys-backend \
    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml)
  echo "Sonar ataskaita: http://localhost:9000/dashboard?id=knygnesys"
  sleep 15
  python "$HERE/scripts/sonar_export.py" "$OUT" "$(cat "$TOOLS/sonar-token.txt")" || true
else
  echo "SonarQube nepasiekiamas - praleidžiama."
fi

echo "== 9. Suvestinės lentelės =="
python "$HERE/scripts/summarize.py" "$OUT" > "$OUT/suvestine.md"
echo "Baigta. Žr. $OUT/suvestine.md"
