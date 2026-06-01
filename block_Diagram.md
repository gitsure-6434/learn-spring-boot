# ContextHub — System Block Diagram

Automated WebEx bot that answers questions from a Confluence knowledge base using RAG (Retrieval-Augmented Generation).

---

## 1. High-Level System Architecture

```mermaid
flowchart TB
    subgraph External["External Systems"]
        WX[WebEx Platform]
        CF[Confluence / Atlassian]
        NG[Ngrok / Public URL]
    end

    subgraph App["ContextHub — Spring Boot 3.5 (port 8082)"]
        direction TB
        CTRL[REST Controllers]
        SVC[Services Layer]
        CFG[Configuration]
        CON[WebhookConsumer]
    end

    subgraph Infra["Infrastructure"]
        RMQ[(RabbitMQ)]
        REDIS[(Redis)]
        CASS[(Cassandra)]
        QDR[(Qdrant Vector Store)]
        OLL[Ollama LLM]
    end

    WX -->|webhook POST| NG
    NG --> CTRL
    CF -->|webhook / REST API| CTRL
    CTRL --> SVC
    SVC --> CON
    CON --> RMQ
    SVC --> REDIS
    SVC --> CASS
    SVC --> QDR
    SVC --> OLL
    SVC -->|reply| WX
    SVC -->|fetch pages| CF
```

---

## 2. Application Layer (Java Packages)

```mermaid
flowchart LR
    subgraph controllers["controller"]
        WEC[WebExController]
        CC[ConfluenceController]
        CCC[ConfluenceClientController]
    end

    subgraph consumer["consumer"]
        WC[WebhookConsumer]
    end

    subgraph config["configuration"]
        AI[AiConfig]
        RC[RabbitConfig]
        ARC[AdvancedRabbitConfig]
    end

    subgraph confluence["confluence.service"]
        CIG[ConfluenceIngestionService]
        CES[ConfluenceEmbeddingService]
        CC2[ConfluenceClient]
        CS[ChunkingService]
        QVS[QdrantVectorService]
        WXS[WebExWebhookService]
        WXC[WebExClient]
        MS[MarkdownService]
    end

    subgraph search["confluence.service.search"]
        RS[RagService]
        RET[RetrievalService]
        HS[HybridSearchService]
    end

    subgraph app["ContexthubApplication"]
        FP[File Pipeline Functions]
    end

    WEC --> WXS
    WXS --> RMQ2[(RabbitMQ)]
    WC --> WXC
    CC --> CIG
    CC --> CES
    CCC --> RS
    AI --> RS
    CIG --> CC2
    CIG --> CS
    CIG --> QVS
    WXC --> RS
    RS --> RET
    RS --> HS
    RET --> QDR2[(Qdrant)]
    FP --> QDR2
```

---

## 3. WebEx Chat Bot Flow (Async + Queue)

```mermaid
sequenceDiagram
    participant User as WebEx User
    participant WX as WebEx API
    participant Ctrl as WebExController
    participant Svc as WebExWebhookService
    participant RMQ as RabbitMQ
    participant Cons as WebhookConsumer
    participant Redis as Redis
    participant Client as WebExClient
    participant RAG as RagService
    participant Ollama as Ollama
    participant Cass as Cassandra Chat Memory
    participant Qdrant as Qdrant

    User->>WX: Send message in room
    WX->>Ctrl: POST /webexwebhook
    Note over Ctrl: Ignore bot's own messages
    Ctrl->>Svc: processPayloadAsync (async)
    Ctrl-->>WX: 200 OK (immediate)
    Svc->>RMQ: Publish WebexEvent

    RMQ->>Cons: @RabbitListener processWebhook
    Cons->>Redis: isDuplicate(messageId)?
    alt Duplicate
        Cons-->>Cons: Skip processing
    else New message
        Redis->>Redis: SET key TTL 24h
        Cons->>Client: fetchMessage(messageId)
        Client->>WX: GET /v1/messages/{id}
        WX-->>Client: Question text
        Cons->>Client: processMessage(conversationId, roomId, question)
        Client->>RAG: askWebExQuestion
        RAG->>Qdrant: semanticSearch (via RAG advisor)
        RAG->>Ollama: Generate answer + memory
        RAG->>Cass: Store/retrieve conversation (ChatMemory)
        RAG-->>Client: Answer + references
        Client->>WX: POST /v1/messages (reply)
        WX-->>User: Bot response
    end
```

---

## 4. Confluence Knowledge Ingestion Flow

```mermaid
flowchart TD
    A[Confluence Webhook POST /webhook] --> B[ConfluenceIngestionService.processWebhook]
    B --> C[ConfluenceClient.fetchPage]
    C --> D[HtmlParserUtil.extractStructuredText]
    D --> E[ChunkingService.chunkDocument]
    E --> F{updateTrigger = edit_page?}

    F -->|Yes — Edit| G[QdrantVectorService.pageBasedSimilaritySearch]
    G --> H[Compare chunk hashes]
    H --> I[deleteChunks — removed content]
    H --> J[storeChunks — new content only]

    F -->|No — New page| K[QdrantVectorService.storeChunks]

    I --> QDR[(Qdrant)]
    J --> QDR
    K --> QDR

    subgraph ChunkMetadata["Document Metadata"]
        M1[pageId, title, url]
        M2[heading, chunkIndex, hash]
    end

    E --> ChunkMetadata
```

---

## 5. Manual / REST Query Flow

```mermaid
flowchart LR
  subgraph Endpoints
    E1["GET /asktowiki?query="]
    E2["GET /getPage?pageId="]
  end

  E1 --> RAG[RagService.askQuestion]
  RAG --> RET[RetrievalService.semanticSearch]
  RET --> VS[(VectorStore / Qdrant)]
  RAG --> CTX[Build enriched context]
  RAG --> LLM[ChatClient + prompt templates]
  LLM --> OLL[(Ollama llama3.2)]
  RAG --> MD[MarkdownService — HTML refs]
  RAG --> OUT[Response with references]

  E2 --> CES[ConfluenceEmbeddingService]
  CES --> CF[(Confluence REST API)]
  CES --> OUT2[Plain text page content]
```

---

## 6. File-Based Document Ingestion Pipeline

Runs at application startup via Spring Cloud Function composition.

```mermaid
flowchart LR
    FS[fileSupplier<br/>PDF / DOCX / TXT directory] --> DR[documentReader<br/>TikaDocumentReader]
    DR --> SP[splitter<br/>TokenTextSplitter]
    SP --> TD[titleDeterminer<br/>ChatClient + DocLabel]
    TD --> VSC[vectorStoreConsumer]
    VSC --> QDR[(Qdrant context-hub collection)]

    TD -.->|UNKNOWN label| SKIP[Skip — not indexed]
    TD -->|Valid label| META[metadata: docLabel]
    META --> VSC
```

**Pipeline definition** (`application.properties`):

`fileSupplier | documentReader | splitter | titleDeterminer | vectorStoreConsumer`

---

## 7. RAG & AI Configuration

```mermaid
flowchart TB
    subgraph AiConfig["AiConfig Beans"]
        CC1[chatClient — Primary]
        RCC[ragChatClient — RAG + Memory]
        CM[ChatMemory — MessageWindow max 10]
        RT[RestTemplate]
    end

    subgraph ragChatClientAdvisors["ragChatClient Default Advisors"]
        RAA[RetrievalAugmentationAdvisor]
        MCA[MessageChatMemoryAdvisor]
    end

    RAA --> VDR[VectorStoreDocumentRetriever]
    RAA --> QT[RewriteQueryTransformer]
    RAA --> TT[TranslationQueryTransformer → English]
    RAA --> MQE[MultiQueryExpander — 5 queries]
    MCA --> CMR[(Cassandra ChatMemoryRepository)]

    VDR --> QDR[(Qdrant)]
    CC1 --> OLL[(Ollama)]
    RCC --> OLL

    subgraph RagServicePaths["RagService Usage"]
        P1[askWebExQuestion → ragChatClient + advisors]
        P2[askQuestion → chatClient + manual context]
        P3[askQuestion + chunks → HybridSearchService]
    end

    RCC --> P1
    CC1 --> P2
    HS[HybridSearchService] --> P3
```

---

## 8. Data Stores — Responsibilities

```mermaid
flowchart TB
    subgraph Qdrant["Qdrant (Vector Store)"]
        Q1[Collection: context-hub]
        Q2[Embeddings: mxbai-embed-large via Ollama]
        Q3[Confluence chunks + file documents]
    end

    subgraph Redis["Redis"]
        R1[Key: webhook:{messageId}]
        R2[TTL: 24 hours]
        R3[Purpose: deduplicate WebEx webhooks]
    end

    subgraph RabbitMQ["RabbitMQ"]
        M1[Exchange: webex.exchange]
        M2[Queue: webex.queue]
        M3[Routing: webex.routing.key]
        M4[Concurrency: 5–20 consumers]
    end

    subgraph Cassandra["Cassandra"]
        C1[Keyspace: contexthub]
        C2[Table: chat_memory]
        C3[Column: chat_messages]
        C4[TTL: 30 days]
        C5[Purpose: multi-turn WebEx conversation memory]
    end

    subgraph Ollama["Ollama"]
        O1[Chat: llama3.2:latest]
        O2[Embeddings: mxbai-embed-large]
    end
```

---

## 9. REST API Surface

| Method | Endpoint | Handler | Purpose |
|--------|----------|---------|---------|
| POST | `/webexwebhook` | WebExController | Receive WebEx message events |
| POST | `/webhook` | ConfluenceController | Confluence page create/update webhook |
| GET | `/getPage` | ConfluenceController | Fetch Confluence page text by `pageId` |
| GET | `/asktowiki` | ConfluenceClientController | Ask RAG question via HTTP (`query` param) |

---

## 10. Technology Stack Summary

```mermaid
mindmap
  root((ContextHub))
    Runtime
      Java 21
      Spring Boot 3.5.14
      Spring AI 1.1.5
    Messaging
      RabbitMQ
      Redis dedup
    Storage
      Qdrant vectors
      Cassandra chat memory
    AI
      Ollama LLM
      Spring AI RAG advisors
      Tika document reader
    Integrations
      WebEx REST API
      Confluence REST API
      Ngrok tunnel
    Observability
      SLF4J / Logback
      logs/application.log
```

---

## Component Dependency Matrix

| Component | Depends On |
|-----------|------------|
| WebExController | WebExWebhookService |
| WebExWebhookService | RabbitTemplate, Redis |
| WebhookConsumer | WebExClient, WebExWebhookService (dedup) |
| WebExClient | RestTemplate, RagService, WebEx API |
| RagService | ChatClient, ragChatClient, RetrievalService, HybridSearchService, MarkdownService |
| RetrievalService | VectorStore (Qdrant) |
| ConfluenceIngestionService | ConfluenceClient, ChunkingService, QdrantVectorService |
| QdrantVectorService | VectorStore |
| AiConfig | ChatModel, VectorStore, ChatMemoryRepository |
| ContexthubApplication | FunctionCatalog, VectorStore, ChatClient |

---

*Generated from codebase analysis. Render Mermaid diagrams in GitHub, VS Code (Mermaid extension), or [mermaid.live](https://mermaid.live).*
