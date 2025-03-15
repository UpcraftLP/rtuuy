#!/bin/sh

status=$(wget --quiet -O - --user-agent="rtuuy_healthcheck" http://localhost:3000/api/health | grep -o '"status":\s*"[^"]*"' | awk -F: '{print $2}' | tr -d '"')

if [ "$status" = "healthy" ]; then
		echo "OK"
    exit 0
else
    echo "status: $status"
    exit 1
fi
