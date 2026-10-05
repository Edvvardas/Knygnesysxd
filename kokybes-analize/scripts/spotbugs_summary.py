"""SpotBugs XML -> skaitomas sąrašas (kategorija, prioritetas, tipas, vieta)."""
import sys
import xml.etree.ElementTree as ET

PRIORITY = {"1": "High", "2": "Medium", "3": "Low"}

root = ET.parse(sys.argv[1]).getroot()
bugs = root.findall("BugInstance")
print(f"Iš viso radinių: {len(bugs)}\n")
for b in sorted(bugs, key=lambda b: (b.get("priority"), b.get("category"), b.get("type"))):
    line = b.find("SourceLine")
    cls = b.find("Class").get("classname").replace("lt.prifkodas.knygnesys.", "")
    where = f"{line.get('sourcefile')}:{line.get('start')}" if line is not None and line.get("start") else cls
    msg = b.find("ShortMessage")
    print(f"[{PRIORITY.get(b.get('priority'), b.get('priority'))}] {b.get('category')}/{b.get('type')} "
          f"@ {cls} ({where})\n    {msg.text if msg is not None else ''}")
