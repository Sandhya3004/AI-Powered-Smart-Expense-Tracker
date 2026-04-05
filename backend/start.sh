#!/bin/bash
# Start script for Spring Boot backend with environment variables

cd "$(dirname "$0")"

# Export environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "Environment variables loaded from .env"
fi

# Run Spring Boot application
./mvnw spring-boot:run
