# SRM Credit Engine

Plataforma simplificada para cadastro, precificação, simulação e liquidação de recebíveis em BRL e USD.

O projeto foi desenvolvido como desafio técnico com foco em:

- precisão financeira;
- separação de responsabilidades;
- consistência transacional;
- idempotência;
- auditabilidade;
- API REST;
- painel web para operação.

---

## Tecnologias

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Gradle
- Springdoc OpenAPI

### Frontend

- React
- TypeScript
- Vite

### Infraestrutura

- Docker
- Docker Compose
- PostgreSQL 16 Alpine

---

## Estrutura do projeto

```text
srm-credit-engine/
├── backend/
├── frontend/
├── docker-compose.yml
├── SPEC.md
├── REVIEW.md
├── AI_USAGE.md
├── DECISIONS.md
└── README.md