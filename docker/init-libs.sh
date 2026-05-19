#!/bin/sh
# Runs once before solr-foreground via /docker-entrypoint-initdb.d.
# Copies the freshly-built umich-solr-extensions JAR from the read-only
# bind mount at /opt/umich-ext into ${SOLR_HOME}/lib so Solr 10 picks it
# up at the node classloader level.
#
# Solr 10 no longer honours <lib> in solrconfig.xml, so this is the
# supported alternative to put a custom plugin JAR on the classpath
# without rebuilding the Solr image.
#
# Remove stale copies of our JAR so an `mvn package` + restart picks up
# the new build deterministically.
set -e

SRC_DIR=/opt/umich-ext
DEST_DIR="${SOLR_HOME}/lib"
mkdir -p "$DEST_DIR"

# Remove stale copies
rm -f "$DEST_DIR"/umich-solr-extensions-*.jar

for jar in "$SRC_DIR"/umich-solr-extensions-*.jar; do
  [ -e "$jar" ] || { echo "init-libs: no umich-solr-extensions JAR found in $SRC_DIR; run 'mvn package -DskipTests' on the host first" >&2; exit 1; }
  cp "$jar" "$DEST_DIR/"
  echo "init-libs: copied $(basename "$jar") -> $DEST_DIR"
done
