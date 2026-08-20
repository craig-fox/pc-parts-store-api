#!/bin/bash

set -euo pipefail

echo "Running tests..."

exec mvn clean verify