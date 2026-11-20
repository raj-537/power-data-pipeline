# AWS Bedrock LLM Mode Setup (Optional)

The Q&A agent has **two modes**:

| Mode | Status | Description |
|------|--------|-------------|
| **rule-based** | ✅ **Production-ready** | Fast keyword matching, works out-of-the-box |
| **llm** | 🔧 Optional | AWS Bedrock Claude 3.5 Sonnet, requires AWS setup |

---

## Quick Start (Rule-Based Mode)

**No setup needed!** Rule-based mode is enabled by default and works perfectly:

```bash
curl -X POST http://localhost:8080/api/qa/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Which state had the highest shortage?"}'
```

---

## LLM Mode Setup (Optional)

If you want to enable AWS Bedrock LLM mode:

### 1. **AWS Bedrock Account Access**

- Log into AWS Console → **Amazon Bedrock**
- Navigate to: **Model Access** (left sidebar)
- Request access to: **Claude 3.5 Sonnet**
- ⚠️ **Access approval can take 1-2 business days**

### 2. **AWS Credentials**

**Option A: Use the batch script (Windows):**

```bash
# Copy the template
copy start-with-llm.bat.example start-with-llm.bat

# Edit start-with-llm.bat and add your AWS credentials
# Then run it
start-with-llm.bat
```

**Option B: Set environment variables manually:**

```bash
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_BEDROCK_ENABLED=true
```

⚠️ **Security:** Never commit `start-with-llm.bat` with real credentials! Use the `.example` template.

### 3. **Restart Application**

```bash
mvn spring-boot:run
```

### 4. **Test LLM Mode**

```bash
curl -X POST "http://localhost:8080/api/qa/ask?mode=llm" \
  -H "Content-Type: application/json" \
  -d '{"question": "Analyze the power shortage patterns"}'
```

---

## Why Rule-Based is Default

✅ **Benefits for production:**
- No external dependencies (AWS account, API costs)
- Fast response times (~100ms vs ~3s)
- Predictable behavior
- Zero setup for interviewer/reviewer
- Perfect for demonstration

💡 **LLM mode shows:**
- Cloud AI integration skills
- AWS Bedrock API knowledge
- Hybrid architecture design

---

## Troubleshooting

**Error: "Model version has reached end of life"**
- Your AWS account doesn't have access to Claude models
- Go to AWS Bedrock Console → Request Model Access
- Use rule-based mode (works perfectly without AWS)

**Error: "AWS Bedrock is not configured"**
- Set `AWS_BEDROCK_ENABLED=true` environment variable
- Ensure AWS credentials are set
- Or use rule-based mode (no setup needed)
