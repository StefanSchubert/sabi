#!/usr/bin/env bash
#
# <!--
#   - Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
#   - See project LICENSE file for the detailed terms and conditions.
#   -->
#
#

# =============================================================================
# server_redeploy.sh
#
# Full redeploy of the sabi-backend Docker container on ARM (MacBook M1/M2/M3/M4).
#
# Usage:
#   bash server_redeploy.sh [--boundary] [--flyway]
#
# Options:
#   --boundary   Install sabi-boundary into local Maven repo before building
#                the server (required after pom.xml / boundary source changes).
#   --flyway     Run the Flyway migration container after the backend starts
#                (required after adding new DB migration scripts).
#
# What this script does:
#   1. Stop + remove the running sabi-backend container (if any)
#   2. Optionally: mvn install sabi-boundary (--boundary flag)
#   3. Build sabi-server JAR (skipping tests)
#   4. Copy the JAR into the Docker context  (copyjars.sh)
#   5. Rebuild + start the sabi-backend container via docker compose (ARM)
#   6. Optionally: run the Flyway container to apply pending migrations (--flyway)
#   7. Tail the backend log for 30 seconds so you can verify startup
#
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-arm.yml"
BOUNDARY_DIR="${REPO_ROOT}/sabi-boundary"
SERVER_DIR="${REPO_ROOT}/sabi-server"

INSTALL_BOUNDARY=false
RUN_FLYWAY=false

for arg in "$@"; do
  case "$arg" in
    --boundary) INSTALL_BOUNDARY=true ;;
    --flyway)   RUN_FLYWAY=true ;;
    *)
      echo "Unknown option: $arg"
      echo "Usage: bash server_redeploy.sh [--boundary] [--flyway]"
      exit 1
      ;;
  esac
done

echo ""
echo "========================================"
echo "  sabi-backend redeploy  (ARM)"
echo "========================================"
echo "  Boundary install : ${INSTALL_BOUNDARY}"
echo "  Flyway migration : ${RUN_FLYWAY}"
echo "========================================"
echo ""

# --- Step 1: Stop + remove existing backend container -------------------------
echo "[1/6] Stopping and removing sabi-backend container (if running)..."
docker compose -f "${COMPOSE_FILE}" stop sabi-backend  || true
docker compose -f "${COMPOSE_FILE}" rm  -f sabi-backend || true
echo "      Done."

# --- Step 2 (optional): Install sabi-boundary ---------------------------------
if [ "${INSTALL_BOUNDARY}" = true ]; then
  echo "[2/6] Installing sabi-boundary into local Maven repo..."
  (cd "${BOUNDARY_DIR}" && mvn install -DskipTests -q)
  echo "      Done."
else
  echo "[2/6] Skipping boundary install (use --boundary to enable)."
fi

# --- Step 3: Build sabi-server -----------------------------------------------
echo "[3/6] Building sabi-server JAR (tests skipped)..."
(cd "${SERVER_DIR}" && mvn package -DskipTests -q)
echo "      Done."

# --- Step 4: Copy JAR into Docker context ------------------------------------
echo "[4/6] Copying JARs into Docker context..."
(cd "${SCRIPT_DIR}" && bash copyjars.sh)
echo "      Done."

# --- Step 5: Rebuild + start backend container --------------------------------
echo "[5/6] Rebuilding and starting sabi-backend container..."
docker compose -f "${COMPOSE_FILE}" up --build -d sabi-backend
echo "      Done — container started."

# --- Step 6 (optional): Run Flyway migrations ---------------------------------
if [ "${RUN_FLYWAY}" = true ]; then
  echo "[6/6] Running Flyway migrations..."
  docker compose -f "${COMPOSE_FILE}" run --rm flyway
  echo "      Flyway finished."
else
  echo "[6/6] Skipping Flyway (use --flyway to run pending migrations)."
fi

# --- Step 7: Tail startup log ------------------------------------------------
echo ""
echo "Waiting 5 s for container to initialise, then tailing log for 25 s..."
sleep 5
timeout 25 docker logs -f sabi-as 2>&1 || true

echo ""
echo "========================================"
echo "  Redeploy complete."
echo "  Container : sabi-as"
echo "  Compose   : docker-compose-arm.yml"
echo "========================================"

