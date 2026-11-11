#!/usr/bin/env python3
"""Rebuild the design-panel SVGs with Python 3's standard library.

The panel modules beside this script are the editable diagram source:
baseline_panels.py (architecture, use cases, class and collaborator panels),
structure_panels.py (technology stack, packages, deployment, database) and
behaviour_panels.py (sequence diagrams and state machines). Coordinates are deliberate:
UML roles, labels and routed links stay readable without a graph-layout dependency.
Run from any directory; --check verifies source names, XML and committed exports.
"""

from pathlib import Path
import argparse
import sys
import xml.etree.ElementTree as ET

import baseline_panels
import behaviour_panels
import structure_panels

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
BUILDERS = baseline_panels.BUILDERS + structure_panels.BUILDERS + behaviour_panels.BUILDERS


def verify_names(svg):
    """Catch renamed or removed Java classes and database tables named in a panel."""
    root = ET.fromstring(svg)
    classes = {path.stem for path in (ROOT/"backend/src/main/java").rglob("*.java")}
    migrations = "\n".join(path.read_text() for path in sorted((ROOT/"backend/src/main/resources/db/migrations").glob("*.sql")))
    named_classes = {element.attrib["data-class"] for element in root.iter() if "data-class" in element.attrib}
    named_tables = {element.attrib["data-table"] for element in root.iter() if "data-table" in element.attrib}
    missing = sorted(named_classes-classes) + sorted(table for table in named_tables if table not in migrations)
    if missing:
        raise ValueError("Diagram references absent Java classes or tables: "+", ".join(missing))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Validate SVGs and fail if committed exports differ from source")
    args = parser.parse_args()
    changed = []
    for builder in BUILDERS:
        diagram = builder()
        svg = diagram.svg()
        verify_names(svg)
        output = HERE/"exports"/(diagram.slug+".svg")
        if args.check:
            if not output.exists() or output.read_text() != svg:
                changed.append(str(output.relative_to(ROOT)))
        else:
            output.parent.mkdir(exist_ok=True)
            output.write_text(svg)
            print(output.relative_to(ROOT))
    if changed:
        print("Stale or missing exports:\n"+"\n".join(changed), file=sys.stderr)
        return 1
    if args.check:
        print(f"Verified {len(BUILDERS)} deterministic SVG exports, XML syntax and referenced Java class and table names.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
