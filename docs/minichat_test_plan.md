# Mini-Chat Functional Test Plan

**Date:** 2025-01-23
**Tester:** QA Expert Agent
**Feature:** Mini-Chat with RAG + Task Memory

## Test Environment

- **CLI Command:** `agent minichat`
- **RAG Index:** Should be indexed before testing
- **Test Project:** Simple_code_agent (current codebase)

## Test Scenarios

### Scenario 1: Technical Implementation Task

**Goal:** Implement a new feature with specific technical requirements

**User Story:** User wants to implement a new TaskContext feature and needs guidance through the process.

#### Messages (10-15):

1. **User:** "Помоги реализовать TaskContext feature для мини-чата с RAG"
   - **Expected:**
     - System extracts goal: "Implement TaskContext feature"
     - RAG search for existing TaskContext code
     - Sources displayed with relevance scores
     - Context stage: INITIALIZING → EXPLORING

2. **User:** "Нужен domain model с TaskContext, RagQueryHistory, ContextSummary"
   - **Expected:**
     - Clarification added: "domain model requirements"
     - Constraint added: TECHNOLOGY - Kotlin data classes
     - RAG search for similar patterns in codebase
     - Sources show existing domain models

3. **User:** "Как правильно спроектировать Room Entity для TaskContext?"
   - **Expected:**
     - RAG search shows existing Entity patterns
     - Sources: WorkingMemoryEntity, DocumentChunkEntity
     - Clarification added: "Room Entity design"

4. **User:** "Покажи пример Repository pattern в проекте"
   - **Expected:**
     - RAG search finds existing repositories
     - Sources show TaskStateRepository, ChatRepository
     - Constraint added: ARCHITECTURE - Repository pattern

5. **User:** "Нужны Use Cases для работы с TaskContext"
   - **Expected:**
     - RAG search for existing Use Cases
     - Sources show SendMessageUseCase, GetMemoryContextUseCase
     - Clarification added: "Use Cases requirements"

6. **User:** "Как интегрировать с Koin DI?"
   - **Expected:**
     - RAG search finds DI modules
     - Sources show FeatureRagModule, CliModule
     - Clarification added: "DI integration"

7. **User:** "Нужна CLI команда для мини-чата"
   - **Expected:**
     - RAG search shows existing CLI commands
     - Sources: ChatCommand, MemoryCommand
     - Constraint added: TECHNOLOGY - Clikt

8. **User:** "Как сохранить историю диалога?"
   - **Expected:**
     - RAG search for chat history
     - Sources show MessageEntity, ChatSessionEntity
     - Clarification added: "dialog history"

9. **User:** "Покажи как работает RAG в проекте"
   - **Expected:**
     - RAG search finds RagSearchService
     - Sources show RagSearchServiceImpl, OllamaEmbeddingClient
     - Clarification added: "RAG implementation"

10. **User:** "Как отобразить источники в CLI?"
    - **Expected:**
      - RAG search shows formatting examples
      - Sources show OutputFormatter, RagResponseFormatter
      - Clarification added: "source display"

11. **User:** "Что должно быть в /stats команде?"
    - **Expected:**
      - RAG search for stats examples
      - Sources show existing stats implementations
      - Clarification added: "stats command"

12. **User:** "Как обрабатывать ошибки в Use Cases?"
    - **Expected:**
      - RAG search for error handling
      - Sources show ResultWrapper pattern
      - Clarification added: "error handling"

#### **Expected Results:**

✅ **Task Context Preservation:**
- Goal remains consistent: "Implement TaskContext feature"
- All clarifications tracked (domain model, Room Entity, Repository, etc.)
- All constraints captured (Kotlin, Repository pattern, Clikt, etc.)
- Stage progression: INITIALIZING → EXPLORING → IMPLEMENTING

✅ **RAG Quality:**
- Every message triggers RAG search
- Sources displayed with relevance scores
- Relevant code snippets found
- Similarity scores appropriate (> 0.5 for relevant results)

✅ **Memory Consistency:**
- Assistant remembers previous messages
- References back to earlier clarifications
- Maintains context across 12 messages

✅ **Source Attribution:**
- Every response includes sources
- Sources are relevant to question
- File paths and line numbers correct

---

### Scenario 2: Exploratory/Research Task

**Goal:** Understand how a specific part of the system works

**User Story:** User wants to understand the Memory system architecture and components.

#### Messages (10-15):

1. **User:** "Как работает Memory система в проекте?"
   - **Expected:**
     - Goal: "Understand Memory system"
     - RAG search for Memory-related code
     - Sources show MemoryContext, WorkingMemory, ShortTermMemory
     - Stage: INITIALIZING → EXPLORING

2. **User:** "Какие уровни памяти есть?"
   - **Expected:**
     - RAG search finds memory levels
     - Sources show three-tier architecture
     - Clarification: "memory levels"

3. **User:** "Как WorkingMemory связана с ChatSession?"
   - **Expected:**
     - RAG search for relationships
     - Sources show foreign keys, Room relations
     - Clarification: "WorkingMemory-ChatSession relation"

4. **User:** "Что такое ShortTermMemory?"
   - **Expected:**
     - RAG search for STM implementation
     - Sources show ShortTermMemoryRepository
     - Clarification: "STM definition"

5. **User:** "Как работает LongTermMemory?"
   - **Expected:**
     - RAG search for LTM
     - Sources show KnowledgeEntry, UserProfile
     - Clarification: "LTM implementation"

6. **User:** "Как использовать GetMemoryContextUseCase?"
   - **Expected:**
     - RAG search shows use case
     - Sources show usage examples
     - Clarification: "GetMemoryContextUseCase usage"

7. **User:** "Как оптимизировать контекст памяти?"
   - **Expected:**
     - RAG search for optimization
     - Sources show MemoryContextOptimizer
     - Clarification: "memory optimization"

8. **User:** "Как сохраняются контекстные якоря?"
   - **Expected:**
     - RAG search for anchors
     - Sources show ContextAnchorEntity, ContextAnchorDao
     - Clarification: "context anchors"

9. **User:** "Как работает база знаний?"
   - **Expected:**
     - RAG search for knowledge base
     - Sources show KnowledgeEntryEntity
     - Clarification: "knowledge base"

10. **User:** "Как интегрирована память с RAG?"
    - **Expected:**
      - RAG search for integration
      - Sources show GetMemoryContextUseCase calling RagSearchService
      - Clarification: "Memory-RAG integration"

11. **User:** "Как оптимизировать контекст для LLM?"
    - **Expected:**
      - RAG search shows optimization strategies
      - Sources show MemoryOptimizationStrategy enum
      - Clarification: "LLM context optimization"

12. **User:** "Покажи пример использования MemoryContext"
    - **Expected:**
      - RAG search finds usage examples
      - Sources show CliChatController usage
      - Clarification: "MemoryContext usage example"

#### **Expected Results:**

✅ **Task Context Preservation:**
- Goal consistent: "Understand Memory system"
- All clarifications tracked (levels, STM, LTM, optimization, etc.)
- Progressive understanding built through conversation
- Stage: EXPLORING throughout

✅ **RAG Quality:**
- Each question finds relevant Memory components
- Sources show appropriate files
- Progressive deepening into topic
- Similarity scores high for targeted questions

✅ **Memory Consistency:**
- Assistant builds on previous answers
- References earlier concepts
- Creates coherent mental model for user

✅ **Source Attribution:**
- Sources show Memory feature files
- Relevant code snippets
- Correct file references

---

## Test Execution Checklist

### Pre-Test Setup:
- [ ] Ensure RAG index is built (`agent index run`)
- [ ] Verify Ollama is running
- [ ] Check database is accessible
- [ ] Clear any existing mini-chat sessions

### Scenario 1 Execution:
- [ ] Send message 1, verify goal extraction
- [ ] Send message 2, verify clarification added
- [ ] Send message 3, verify RAG sources
- [ ] Continue through all 12 messages
- [ ] Run `/stats` after each message
- [ ] Verify context preservation
- [ ] Check RAG relevance scores

### Scenario 2 Execution:
- [ ] Send message 1, verify goal extraction
- [ ] Send message 2, verify RAG sources
- [ ] Continue through all 12 messages
- [ ] Run `/stats` periodically
- [ ] Verify progressive understanding
- [ ] Check source relevance

### Post-Test:
- [ ] Run `/stats` final check
- [ ] Verify all clarifications captured
- [ ] Verify all constraints recorded
- [ ] Check RAG query history
- [ ] Test `/clear` command
- [ ] Test `exit` command

---

## Success Criteria

### Scenario 1 Success:
1. ✅ Goal extracted correctly: "Implement TaskContext feature"
2. ✅ At least 5 clarifications captured
3. ✅ At least 3 constraints identified
4. ✅ RAG sources relevant to each question
5. ✅ Context maintained across 12 messages
6. ✅ Sources displayed with scores > 0.5
7. ✅ No hallucinations (all sources exist)

### Scenario 2 Success:
1. ✅ Goal extracted correctly: "Understand Memory system"
2. ✅ Progressive understanding demonstrated
3. ✅ RAG finds relevant Memory components
4. ✅ Context built incrementally
5. ✅ Sources show correct Memory files
6. ✅ No contradictions in answers
7. ✅ Coherent mental model created

---

## Known Issues & Limitations

1. **LLM Dependency:** Responses depend on LLM quality
2. **RAG Index Quality:** Results depend on indexed code
3. **Token Limits:** Very long conversations may hit token limits
4. **Context Window:** Limited memory for very long sessions

---

## Test Results Template

**Scenario:** [1 or 2]
**Date:** [date]
**Tester:** [name]

### Messages Tested: [count]

### Context Preservation:
- Goal: [preserved/lost]
- Clarifications: [count] captured
- Constraints: [count] captured
- Stage progression: [stages]

### RAG Quality:
- Average similarity: [score]
- Relevant sources: [percentage]%
- Hallucinations: [count]

### Issues Found:
1. [description]
2. [description]

### Overall Result: [PASS/FAIL]

---

## Notes

- Record any unexpected behaviors
- Note areas for improvement
- Track any bugs found
- Document edge cases discovered
