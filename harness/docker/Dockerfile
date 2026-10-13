# Sandbox for agent sessions and grading. Agent containers run with --network none,
# so every Maven dependency and plugin is resolved at image build time (offline afterwards).
FROM eclipse-temurin:21-jdk

RUN apt-get update \
 && apt-get install -y --no-install-recommends maven git curl ripgrep python3 \
 && rm -rf /var/lib/apt/lists/*

RUN git config --system user.email "agent@archagents.invalid" \
 && git config --system user.name "agent" \
 && git config --system init.defaultBranch main \
 && git config --system --add safe.directory '*'

# Warm the local Maven repository with everything the variants and the grading suite need.
COPY warm/ /tmp/warm/
# Variants: full build incl. their own tests (pulls surefire providers, ArchUnit, Spring test).
# Acceptance suite: run once against a dead port; every test fails, but the surefire JUnit provider gets resolved.
RUN set -e; \
    for p in /tmp/warm/fhir-none /tmp/warm/fhir-layered /tmp/warm/fhir-hexagonal; do \
      (cd "$p" && mvn -B -q dependency:go-offline && mvn -B -q package); \
    done; \
    (cd /tmp/warm/fhir-acceptance && mvn -B -q dependency:go-offline \
       && (mvn -B -q test -Dbase.url=http://localhost:1 > /dev/null 2>&1 || true)); \
    rm -rf /tmp/warm
COPY settings.xml /root/.m2/settings.xml

WORKDIR /workspace

# ---------------------------------------------------------------------------------------------
# Tag 2 and later: erosion measurement tools (harness/erosion.py, harness/EROSION.md).
# Everything above this line is unchanged from tag 1, so those layers come from the build cache.
# Pinned versions; the archives are checked against the SHA-256 digests published on the
# projects' GitHub releases. The metrics tool is built with a separate, throw-away local Maven
# repository so the agents' ~/.m2 stays exactly as in tag 1.
ARG PMD_VERSION=7.28.0
ARG PMD_SHA256=f974ba571f7bc01c73fffe11bade25fbc1f438698f9e9c00e416e8d65e9fbac2
ARG SPOTBUGS_VERSION=4.10.4
ARG SPOTBUGS_SHA256=72bc0d4edd686e462c0f71f42a049b27bf4da6708797ff7b2b56dd202714b4e5
RUN set -e; cd /tmp; \
    curl -fsSL -o pmd.zip "https://github.com/pmd/pmd/releases/download/pmd_releases%2F${PMD_VERSION}/pmd-dist-${PMD_VERSION}-bin.zip"; \
    echo "${PMD_SHA256}  pmd.zip" | sha256sum -c -; \
    python3 -m zipfile -e pmd.zip /opt; mv "/opt/pmd-bin-${PMD_VERSION}" /opt/pmd; chmod +x /opt/pmd/bin/*; rm pmd.zip; \
    curl -fsSL -o spotbugs.tgz "https://github.com/spotbugs/spotbugs/releases/download/${SPOTBUGS_VERSION}/spotbugs-${SPOTBUGS_VERSION}.tgz"; \
    echo "${SPOTBUGS_SHA256}  spotbugs.tgz" | sha256sum -c -; \
    tar -xzf spotbugs.tgz -C /opt; mv "/opt/spotbugs-${SPOTBUGS_VERSION}" /opt/spotbugs; chmod +x /opt/spotbugs/bin/*; rm spotbugs.tgz; \
    printf 'PMD_VERSION=%s\nSPOTBUGS_VERSION=%s\n' "$PMD_VERSION" "$SPOTBUGS_VERSION" > /opt/erosion-tools.env
COPY tools/metrics-tool/ /opt/metrics-tool/src/
RUN set -e; cd /opt/metrics-tool/src; \
    printf '<settings/>' > /tmp/online-settings.xml; \
    mvn -B -q -s /tmp/online-settings.xml -Dmaven.repo.local=/tmp/m2-metrics package; \
    cp target/metrics-tool.jar /opt/metrics-tool/metrics-tool.jar; \
    cp pmd-ruleset.xml /opt/metrics-tool/pmd-ruleset.xml; \
    rm -rf target /tmp/m2-metrics /tmp/online-settings.xml; \
    /opt/pmd/bin/pmd --version; java -cp /opt/spotbugs/lib/spotbugs.jar edu.umd.cs.findbugs.Version
