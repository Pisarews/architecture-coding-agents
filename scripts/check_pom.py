#!/usr/bin/env python3
"""Check that a variant's pom.xml declares exactly the domain's allowed dependency set."""
import sys
import xml.etree.ElementTree as ET

NS = {"m": "http://maven.apache.org/POM/4.0.0"}

ALLOWED = {
    "fhir": {
        ("org.springframework.boot", "spring-boot-starter-web", "compile"),
        ("org.springframework.boot", "spring-boot-starter-data-jpa", "compile"),
        ("org.springframework.boot", "spring-boot-starter-validation", "compile"),
        ("org.flywaydb", "flyway-core", "compile"),
        ("com.h2database", "h2", "runtime"),
        ("ca.uhn.hapi.fhir", "hapi-fhir-structures-r4", "compile"),
        ("org.springframework.boot", "spring-boot-starter-test", "test"),
        ("com.tngtech.archunit", "archunit-junit5", "test"),
    },
}
PARENT = ("org.springframework.boot", "spring-boot-starter-parent", "3.5.16")


def main(domain: str, pom: str) -> int:
    root = ET.parse(pom).getroot()
    parent = root.find("m:parent", NS)
    got_parent = tuple(parent.findtext(f"m:{k}", namespaces=NS) for k in ("groupId", "artifactId", "version"))
    ok = True
    if got_parent != PARENT:
        print(f"parent {got_parent} != {PARENT}")
        ok = False
    deps = set()
    for d in root.findall("m:dependencies/m:dependency", NS):
        deps.add((d.findtext("m:groupId", namespaces=NS), d.findtext("m:artifactId", namespaces=NS),
                  d.findtext("m:scope", default="compile", namespaces=NS)))
    want = ALLOWED[domain]
    for extra in sorted(deps - want):
        print(f"unexpected dependency: {extra}")
    for missing in sorted(want - deps):
        print(f"missing dependency: {missing}")
    return 0 if ok and deps == want else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
