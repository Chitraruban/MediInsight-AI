#!/bin/bash
SPRING_DATASOURCE_URL="postgres://renderuser:password@host/db"
if [ -n "$SPRING_DATASOURCE_URL" ] && [[ "$SPRING_DATASOURCE_URL" == postgres://* ]]; then
  export JDBC_URL=$(echo $SPRING_DATASOURCE_URL | sed "s/postgres:\/\//jdbc:postgresql:\/\//")
  echo "Starting with PostgreSQL configuration"
  echo "JDBC_URL=$JDBC_URL"
else
  echo "Starting with default configuration"
fi
