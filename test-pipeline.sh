#!/bin/bash

# Simple test script to run pipeline and view results

echo "Starting Power Data Pipeline Test..."
echo ""

# Start application if not running
if ! curl -s http://localhost:8080/api/pipeline/stats > /dev/null 2>&1; then
    echo "Starting Spring Boot application..."
    mvn spring-boot:run &

    # Wait for startup
    echo "Waiting for application to start..."
    for i in {1..30}; do
        if curl -s http://localhost:8080/api/pipeline/stats > /dev/null 2>&1; then
            echo "Application started!"
            break
        fi
        sleep 2
    done
fi

echo ""
echo "Running pipeline..."
curl -X POST http://localhost:8080/api/pipeline/run

echo ""
echo ""
echo "Pipeline Statistics:"
curl -s http://localhost:8080/api/pipeline/stats | python -m json.tool

echo ""
echo ""
echo "Sample Data (Maharashtra):"
curl -s "http://localhost:8080/api/pipeline/data?state=Maharashtra" | python -m json.tool | head -100

echo ""
echo "Test complete!"
