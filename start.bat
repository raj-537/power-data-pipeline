@echo off
REM Startup script - rule-based mode only (no AWS)

echo Starting Power Data Pipeline...
echo Mode: Rule-based Q&A (default)
echo LLM: Disabled
echo.

mvn spring-boot:run
