#!/usr/bin/env sh
cd "$(dirname "$0")"
java -Xms2G -Xmx6G -jar dxdcraft-server-launcher.jar "$@"
