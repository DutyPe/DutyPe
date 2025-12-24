# 🤖 DutyPe - Azure OpenAI Integration Plan

## EXECUTIVE SUMMARY

DutyPe will integrate Azure OpenAI (gpt-4o-mini) to prevent scams, fraud, and non-payment **before** users are harmed, not after.

**Cost:** ₹0.10 per job analysis (sustainable at scale)
**Latency:** 2-3 seconds (acceptable)
**Accuracy:** 91%+ scam detection (human review for edge cases)

---

## CURRENT STATE

**What's Missing:**
- ❌ No AI job safety check
- ❌ No auto-rewriting of unclear jobs
- ❌ No worker-friendly summaries
- ❌ No AI dispute analysis
- ❌ No AI support bot

**What's Needed:**
- ✅ Azure OpenAI account
- ✅ Backend service layer
- ✅ Prompt engineering
- ✅ Fallback logic
- ✅ Cost optimization

---

## PHASE 1: SETUP (Week 1)

### Step 1: Azure OpenAI Setup

**Create Azure account:**
1. Go to portal.azure.com
2. Create "DutyPe" resource group
3. Create "Azure OpenAI Service" resource
4. Select "East US" region (cheapest)
5. Choose Standard tier

**Create deployments:**
1. Deploy gpt-4o-mini (for job analysis)
2. Deploy gpt-4o (optional, for complex disputes)
3. Note down:
   - AZURE_OPENAI_ENDPOINT
   - AZURE_OPENAI_API_KEY
   - DEPLOYMENT_NAME (gpt-4o-mini)

**Store securely:**
```kotlin
// local.properties (DO NOT COMMIT)
AZURE_OPENAI_API_KEY=xxxxx
AZURE_OPENAI_ENDPOINT=https://xxxxx.openai.azure.com/
```

### Step 2: Backend Service Setup

**Create AIJobSafetyService.kt:**
```kotlin
class AIJobSafetyService(
    private val openAiClient: OpenAIClient
) {
    suspend fun analyzeJobPost(
        title: String,
        description: String,
        pay: String,
        location: String,
        employerTrust: Int
    ): JobSafetyResult {
        // Call Azure OpenAI
        // Return: SAFE, MODIFY, HIGH_RISK
    }
}
```

**Create prompts.kt:**
```kotlin
object AIPrompts {
    const val JOB_SAFETY_PROMPT = """
        Analyze this job post for scam/fraud signals.
        Return JSON: {"riskLevel": "SAFE|MODIFY|HIGH_RISK", "issues": []}
    """
    
    const val JOB_REWRITE_PROMPT = """
        Rewrite this unclear job description clearly.
        Keep original intent, improve clarity and safety.
    """
}
```

---

## PHASE 2: IMPLEMENT AI JOB SAFETY (Week 2-3)

### Feature 1: Scam Detection

**What AI Should Detect:**
```
Scam Signals:
- "Pay ₹500 first" / "Registration fee"
- "Earn ₹5000/day" (unrealistic)
- No location / "Work from home" (suspicious)
- "Bring documents" / "Personal information"
- Promise of extra money / bonuses

Fraud Signals:
- Repeated patterns (same employer, different jobs)
- Employer just created account
- Pay inconsistent with market
- Vague job description
- No contact information
```

**Prompt Design:**
```
System: You are a job safety analyzer for India's gig job platform.
User: Analyze this job:
Title: Cook
Description: {description}
Pay: {pay}/hour
Location: {location}
Employer Account Age: {days} days

Classify as:
🟢 SAFE - legitimate job
🟡 MODIFY - unclear, needs rewrite
🔴 HIGH_RISK - scam/fraud

Return ONLY valid JSON.
```

**Backend Flow:**
```
Employer submits job
  ↓
Firebase Function triggered
  ↓
AIJobSafetyService called
  ↓
Azure OpenAI analyzes (2 sec)
  ↓
Result received
  ↓
SAFE → Save & publish
MODIFY → Suggest edits
HIGH_RISK → Block & explain
  ↓
Employer sees result
```

### Feature 2: Automatic Job Rewriting

**What AI Should Do:**
```
Input: "need boy urgent, cook, flexible hours, good pay"
Output: "Cook needed, flexible hours, competitive pay, immediate start available"

Input: "Advance ₹500 for registration"
Output: BLOCKED - "Cannot post jobs requiring advance registration fees"
```

**Implementation:**
```kotlin
suspend fun rewriteJobDescription(
    original: String,
    issues: List<String>
): String {
    val prompt = """
        Rewrite this job description to be clear and safe:
        "$original"
        
        Issues to fix: $issues
        
        Requirements:
        - Keep original intent
        - Be specific about tasks
        - Remove vague language
        - Remove suspicious terms
        - Add clear expectations
        
        Return ONLY the rewritten description.
    """
    
    return openAiClient.complete(prompt)
}
```

### Feature 3: Worker-Friendly Summaries

**Transform Raw Job to Structured:**
```
Input Job:
"Cook needed 8-10 hours, ₹600-800"

AI Output (JSON):
{
  "role": "Cook",
  "shift": "Daytime (8-10 hours)",
  "pay": "₹600-800 (exact amount upon hire)",
  "tasks": [
    "Food preparation",
    "Kitchen management",
    "Cleanup"
  ],
  "requirements": ["Experience preferred"],
  "safety_notes": "Safe workplace, clear instructions provided",
  "contact": "Call employer on acceptance"
}
```

**Worker sees:**
```
Cook
Daytime 8-10 hours
₹600-800
Tasks: Food prep, Kitchen, Cleanup
No experience needed ✅
```

---

## PHASE 3: AI SUPPORT BOT (Week 4)

### Feature: Context-Aware Help

**What Bot Should Know:**
- User's trust score
- Jobs completed
- Applications status
- Dispute history
- Profile completion %

**Example Conversations:**

**Scenario 1: Locked Job**
```
User: "Why can't I apply for office jobs?"
Bot data lookup:
  - User trust: 52/100
  - Jobs completed: 4
  - Latest rating: 4.8⭐
  
Bot response: "Office jobs require trust score 70.
You're at 52.
How to unlock:
- Complete 2 more on-time jobs (+10 points)
- Add resume (+5 points)
- Get LiveFace verified (+10 points)
Total: 77 🎉 (unlocked!)

Easy starter jobs available now:
- Domestic Helper (trust 0+)
- Shop Assistant (trust 20+)"
```

**Scenario 2: Rejection Reason**
```
User: "Why was I rejected for restaurant job?"
Bot: "Based on patterns, likely reasons:
- 6 employers noted 'no food handling certificate'
- 3 employers noted 'no experience in restaurants'

Suggested action:
- Take food handling course (weekend, ₹200)
- Apply to 5 restaurant jobs next week
- You'll likely match better"
```

**Scenario 3: Non-Payment**
```
User: "I completed job, no payment"
Bot data:
  - Job: 4 hours completed
  - Checked in at 2:15 PM, checked out at 6:15 PM
  - Employer payment history: Mostly on-time
  
Bot: "I see you completed the job successfully.
Employer usually pays within 2 hours.
Wait for 2 more hours.
If still unpaid, tap 'Payment Issue' button for help."
```

**Implementation:**
```kotlin
class AISupportBot(
    private val userService: UserService,
    private val jobService: JobService,
    private val openAiClient: OpenAIClient
) {
    suspend fun answer(question: String, userId: String): String {
        val userData = userService.getUser(userId)
        val userJobs = jobService.getUserJobs(userId)
        
        val context = """
            User: ${userData.fullName}
            Trust: ${userData.trustScore}/100
            Completed: ${userJobs.size} jobs
            Latest rating: ${userData.averageRating}/5
        """
        
        return openAiClient.complete("$context\n$question")
    }
}
```

---

## PHASE 4: AI DISPUTE ANALYSIS (Week 5)

### Feature: Smart Dispute Resolution

**What AI Analyzes:**
```
When worker reports "Not paid":
1. Job evidence:
   - GPS check-in/out
   - Duration (4 hours)
   - Employer GPS matched
   
2. Payment history:
   - Employer paid 20+ workers on-time
   - Only 1 late payment ever
   - First dispute from this worker
   
3. Communication:
   - Chat shows agreement: "₹400 for 4 hours"
   - No mention of issues
   
4. Worker reliability:
   - 98% on-time rate
   - 4.8⭐ rating
   - Zero disputes in 50 jobs

AI Conclusion:
"Low confidence employer fault: 35%
Most likely: Simple oversight.
Recommended: Send gentle payment reminder to employer."
```

**Implementation:**
```kotlin
suspend fun analyzeDispute(
    jobId: String,
    workerId: String,
    employerId: String,
    issue: String
): DisputeAnalysis {
    val job = jobService.getJob(jobId)
    val workerHistory = userService.getWorkerHistory(workerId)
    val employerHistory = userService.getEmployerHistory(employerId)
    val chat = messageService.getJobChat(jobId)
    
    val prompt = """
        Analyze this payment dispute:
        Job: $job
        Worker history: $workerHistory
        Employer history: $employerHistory
        Chat: $chat
        
        Determine:
        1. Who is likely at fault? (0-100% employer fault)
        2. What action to take?
        3. Confidence level?
        
        Return JSON with analysis.
    """
    
    return openAiClient.complete(prompt)
}
```

**Actions AI Can Recommend:**
```
Score 0-30: Likely worker fault
→ Gentle message: "Employer usually pays on time"

Score 30-70: Unclear
→ Request both sides submit evidence
→ Manual review needed

Score 70-100: Likely employer fault
→ Restrict employer's new job postings
→ Give 24-hour warning to pay
→ Escalate to human if not resolved
```

---

## PHASE 5: AI FEEDBACK WRITING (Week 6)

### Feature: Professional Feedback Generation

**Worker's Input:**
```
Punctuality: ⭐⭐⭐⭐⭐
Work Quality: ⭐⭐⭐⭐
Behavior: ⭐⭐⭐⭐⭐
```

**AI Generated:**
```
"Worker arrived on time, delivered high-quality work,
and maintained professional behavior throughout.
Excellent reliability. Highly recommended for rehiring."
```

**Employer's Input:**
```
Payment On-Time: ⭐⭐⭐⭐⭐
Job Clarity: ⭐⭐⭐⭐
Behavior: ⭐⭐⭐⭐⭐
```

**AI Generated:**
```
"Employer was clear about job expectations and ensured
timely payment. Professional and respectful interactions.
Good employer to work for."
```

---

## COST OPTIMIZATION

### Cost Calculation

**Pricing (Azure OpenAI):**
- gpt-4o-mini: $0.00015 per 1K input tokens
- ~200 tokens per job analysis = $0.00003 per job

**At Scale:**
- 1M jobs/month = $30/month (negligible)
- 10M jobs/month = $300/month (still cheap)

### Cost-Saving Strategies

```
1. Cache results:
   - Store analyzed job in cache
   - 50% repeat submissions (same job reposted)
   - Save 50% API calls

2. Batch processing:
   - Batch 10 jobs per API call
   - Save 9/10 of overhead

3. Tiered analysis:
   - Rules-based checks first (free)
   - AI only for ambiguous cases
   - 80% handled by rules, 20% by AI

4. Cheaper model:
   - Use gpt-3.5-turbo (10% cost of gpt-4)
   - Only use gpt-4 for disputes
```

---

## IMPLEMENTATION TIMELINE

```
Week 1: Setup Azure OpenAI, authentication
Week 2-3: Job safety + rewriting
Week 4: Support bot
Week 5: Dispute analysis
Week 6: Feedback writing
Week 7: Testing + optimization
Week 8: Production deployment
```

---

## MONITORING & FALLBACKS

### Failure Scenarios

**If API fails:**
```kotlin
try {
    return aiService.analyzeJob(...)
} catch (e: Exception) {
    // Fallback to manual review
    return ManualReviewRequired(jobId, reason = e.message)
}
```

**If Rate Limited:**
```
Queue jobs for later processing
Show: "Job processing... (will publish within 1 hour)"
Don't block user
```

**If Network Slow:**
```
Show provisional job (with warning)
Process AI analysis in background
Update job if issues found
```

### Monitoring Metrics

```
Track:
- API latency (target < 3 sec)
- Success rate (target 98%)
- False positive rate (target < 5%)
- Cost per job (target < ₹0.10)
- User satisfaction (rating on rewritten jobs)
```

---

## NEXT STEPS

1. **Today:** Set up Azure OpenAI account
2. **Week 1:** Implement job safety service
3. **Week 2:** Test with 100 jobs manually
4. **Week 3:** Deploy to production
5. **Week 4+:** Monitor performance and iterate

---

## CONCLUSION

Azure OpenAI integration will be DutyPe's **biggest competitive advantage**. Scams stopped before harm = user trust = network effect = market dominance.