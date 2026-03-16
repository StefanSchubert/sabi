# Agentic Guidelines for Sabi

## Communication Preferences
**Language & Tone**: German (Deutsch), informal "Du" form
- User preference: Direct, peer-to-peer communication
- Use "Du" (not "Sie") in all responses
- Keep responses concise and action-oriented
- Technical discussions on equal footing

## Session Management (MANDATORY)
**Token Status Reporting**: ALWAYS end your response with token count in format:
```
[XXXk tokens used]
```

**Context Refresh Protocol**:
- Every 50k tokens: Re-read copilot-instructions.md explicitly
- At 80-100k tokens: Recommend session restart to user
- NEVER exceed 120k tokens without explicit user approval

**How to refresh context at 50k intervals**:
1. Pause current work
2. Say: "🔄 Context Refresh (50k tokens reached) - Re-reading copilot-instructions.md"
3. Use read_file tool to read .github/copilot-instructions.md (line 0-500)
4. Summarize: "✅ Key rules refreshed: [list top 3 critical rules]"
5. Continue work

---

## Development Environment Setup

- MacBook CLI (take care of special sed syntax etc.)
- IntelliJ Copilot plugin for code generation
- doccker for lokal testing
- deplyment to pro via ansible playbook
