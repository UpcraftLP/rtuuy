#!/bin/sh

status=$(curl --silent --show-error http://localhost:3000/api/health | jq --raw-output '.status')

echo "status: $status"

# return 0 if healthy, 1 otherwise
[ "$status" = "healthy" ] && exit 0 || exit 1
