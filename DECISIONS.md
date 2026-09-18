# DECISIONS.md

## Decisões técnicas — SRM Credit Engine

Este documento registra as principais decisões tomadas durante o desenvolvimento do desafio, incluindo escolhas arquiteturais, regras financeiras, simplificações de escopo e limitações conhecidas.

---

## 1. Arquitetura: monólito modular

### Decisão

A aplicação foi implementada como um monólito modular, com separação por responsabilidades de negócio:

- pricing;
- currency;
- receivable;
- settlement;
- configuração e tratamento de erros;
- frontend separado em React.

### Motivo

O escopo do desafio não exige distribuição física dos componentes.

Uma arquitetura de microserviços adicionaria complexidade operacional e de consistência sem benefício proporcional para este caso.

A prioridade foi manter:

- baixo acoplamento;
- responsabilidades claras;
- facilidade de execução local;
- código simples de testar e explicar.

---

## 2. Stack principal

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Gradle

### Frontend

- React
- TypeScript
- Vite

### Infraestrutura local

- Docker Compose
- PostgreSQL 16 Alpine

---

## 3. Representação de valores monetários

### Decisão

Todo cálculo financeiro no backend utiliza `BigDecimal`.

Não são utilizados `double`, `float` ou `Math.pow` para valores monetários.

### Motivo

Tipos binários de ponto flutuante podem introduzir erros de representação em operações financeiras.

As taxas também são construídas a partir de strings, por exemplo:

```java
new BigDecimal("0.01")
```

---

## 4. Arredondamento

### Decisão

O arredondamento monetário final utiliza:

```java
RoundingMode.HALF_EVEN
```

com escala de duas casas decimais.

Durante os cálculos intermediários, a divisão utiliza alta precisão com `MathContext`.

A potência inteira do fator mensal é calculada com `BigDecimal.pow(int)`.

### Motivo

O objetivo é evitar arredondamentos intermediários desnecessários e manter o comportamento definido pelos Golden Cases.

---

## 5. Fórmula de precificação

O valor presente segue a regra:

```text
PV = FaceValue / (1 + BaseRate + Spread)^Term
```

Taxa base:

```text
1% ao mês
```

Spreads:

```text
Duplicata Mercantil = 1,5% ao mês
Cheque Pré-datado  = 2,5% ao mês
```

A taxa base permanece centralizada no Pricing Engine e o spread é fornecido por estratégia.

---

## 6. Strategy Pattern para tipo de recebível

### Decisão

Cada tipo de recebível possui uma implementação de `PricingStrategy`.

Exemplos:

- `DuplicataPricingStrategy`
- `ChequePricingStrategy`

### Motivo

A regra específica do tipo não fica concentrada em um grande `if` ou `switch`.

Isso permite adicionar novos tipos de recebíveis sem alterar a fórmula central do Pricing Engine.

---

## 7. Prazo em meses inteiros

### Decisão

O prazo é calculado em meses inteiros através de `ChronoUnit.MONTHS`.

Antes do cálculo, uma data de vencimento anterior à data atual é rejeitada explicitamente.

### Comportamento

- vencimento no passado: inválido;
- vencimento hoje: `0`;
- vencimento futuro dentro do mesmo mês: `0`;
- vencimento em três meses: `3`.

### Motivo

Os Golden Cases fornecidos pelo desafio trabalham com prazo inteiro em meses.

A regra foi centralizada em `TermCalculator` e reutilizada tanto na simulação quanto na liquidação.

---

## 8. Direção da taxa de câmbio

### Decisão

A cotação utilizada é modelada como:

```text
USD -> BRL
```

Exemplo:

```text
1 USD = 5.4321 BRL
```

Quando um valor presente calculado em BRL precisa ser pago em USD:

```text
USD = BRL / taxa
```

### Motivo

Essa representação mantém o significado econômico da cotação separado da direção da conversão solicitada pelo usuário.

---

## 9. Política de vigência do câmbio

### Decisão

Para uma operação em USD, é utilizada a taxa mais recente cuja vigência satisfaça:

```text
effectiveAt <= instante da operação
```

### Motivo

Uma liquidação não deve utilizar uma taxa futura.

A taxa utilizada também é registrada no snapshot do settlement para permitir auditoria posterior.

---

## 10. Conversão ocorre após a precificação em BRL

### Decisão

O Pricing Engine primeiro calcula o valor presente em BRL.

Somente depois esse valor é convertido para USD quando necessário.

### Motivo

Essa ordem é a utilizada pelo Golden Case C3 e evita misturar responsabilidades entre Pricing Engine e Currency Engine.

---

## 11. Settlement como snapshot financeiro

### Decisão

O settlement armazena os valores relevantes utilizados na operação:

- `faceValue`;
- `presentValueBrl`;
- `paymentAmount`;
- `paymentCurrency`;
- `baseRate`;
- `spread`;
- `exchangeRate`;
- `exchangeRateEffectiveAt`;
- `settledAt`.

### Motivo

Consultar uma liquidação antiga não deve depender do estado atual das tabelas de câmbio ou das regras de pricing.

O snapshot melhora rastreabilidade e auditabilidade.

---

## 12. Settlement não possui fluxo de atualização

### Decisão

Não existe endpoint de atualização de settlement.

A entidade também não expõe setters públicos para alteração posterior da operação.

### Motivo

Uma liquidação representa um fato financeiro concluído e deve ser tratada como registro imutável na aplicação.

---

## 13. Transação ACID

### Decisão

O fluxo de liquidação é executado em método anotado com:

```java
@Transactional
```

Dentro da mesma operação são realizadas:

- validações;
- precificação;
- obtenção do câmbio;
- criação do settlement;
- alteração do recebível para `SETTLED`.

### Motivo

Uma falha durante o fluxo não deve deixar apenas parte da operação persistida.

### Limitação

A implementação depende do comportamento transacional do Spring/JPA, mas a entrega atual não possui um teste de integração completo com PostgreSQL/Testcontainers validando rollback real.

---

## 14. Idempotência

### Decisão

O endpoint de liquidação exige:

```text
Idempotency-Key
```

A aplicação consulta uma liquidação existente antes de criar uma nova e o banco também possui constraint única para a chave.

Também existe proteção para que um mesmo recebível não possua dois settlements.

### Motivo

Uma repetição da mesma requisição não deve gerar uma segunda liquidação.

### Limitação conhecida

Em duas requisições realmente simultâneas, ambas podem passar pela consulta inicial antes da gravação.

As constraints do banco ainda impedem duplicidade, mas a requisição perdedora pode receber `409 Conflict` em vez do replay transparente do resultado vencedor.

Para o escopo atual, foi priorizada integridade dos dados.

---

## 15. Simulação sem persistência

### Decisão

Foi criado um endpoint específico de simulação:

```text
POST /api/v1/pricing/simulations
```

A simulação:

- reutiliza `PricingService`;
- reutiliza `CurrencyConversionService`;
- reutiliza `TermCalculator`;
- não cria recebível;
- não cria settlement;
- não altera estado do banco.

### Motivo

O frontend precisa exibir o valor líquido antes da liquidação sem duplicar regras financeiras em JavaScript.

---

## 16. Frontend sem regra financeira

### Decisão

O React não replica:

- fórmula de valor presente;
- juros;
- spread;
- regra de prazo;
- conversão cambial;
- arredondamento financeiro.

O frontend apenas envia dados à API e apresenta os resultados retornados.

### Motivo

Manter uma única fonte de verdade financeira reduz risco de divergência entre frontend e backend.

---

## 17. Tipos monetários na API frontend

### Decisão

Os campos retornados pelo backend são tipados como `number` no TypeScript, pois o Jackson serializa `BigDecimal` como JSON number na configuração atual.

Os inputs permanecem como `string` enquanto o usuário digita.

### Limitação

Para sistemas financeiros com requisitos de precisão também no cliente, uma alternativa futura seria definir o contrato JSON monetário como string decimal.

---

## 18. Paginação server-side

### Decisão

O histórico utiliza `Pageable` e `Page<SettlementResult>` no backend.

O frontend navega pelas páginas fornecidas pela API.

### Motivo

Evita carregar todo o histórico na memória do navegador e atende à evolução esperada para volumes maiores.

---

## 19. CORS

### Decisão

Durante desenvolvimento local, o backend permite apenas:

```text
http://localhost:5173
```

com os métodos e headers necessários ao frontend.

### Motivo

Foi preferida uma configuração centralizada em vez de espalhar `@CrossOrigin` pelos controllers.

Essa configuração é específica para desenvolvimento local.

---

## 20. PostgreSQL e Docker Compose

### Decisão

O PostgreSQL roda localmente via Docker Compose.

A porta externa utilizada é:

```text
5433
```

enquanto o container utiliza a porta padrão `5432`.

### Motivo

A porta `5432` já estava ocupada no ambiente de desenvolvimento.

A porta externa não altera o comportamento interno do PostgreSQL.

---

## 21. Credenciais locais

Para facilitar a execução do desafio, o ambiente local utiliza credenciais simples de desenvolvimento.

Essas credenciais não representam configuração adequada para produção.

Em ambiente real seriam utilizados secrets e configuração externa por ambiente.

---

## 22. Gerenciamento de schema

### Decisão

A entrega utiliza:

```properties
spring.jpa.hibernate.ddl-auto=update
```

### Motivo

Foi uma simplificação consciente para reduzir setup no desafio.

### Limitação

Em produção, a preferência seria por migrations versionadas, por exemplo com Flyway ou Liquibase.

---

## 23. Tratamento de erros

Foi adotado tratamento global para os principais erros de negócio e validação.

Mapeamentos principais:

```text
input inválido       -> 400
conflito de estado   -> 409
constraint violada   -> 409
```

O objetivo é evitar retornar `200 OK` em cenários de erro e não expor stack traces ao cliente.

---

## 24. OpenAPI

A API disponibiliza documentação através do Springdoc/OpenAPI.

Em desenvolvimento:

```text
http://localhost:8080/swagger-ui/index.html
```

Isso facilita inspeção e teste manual dos contratos REST.

---

## 25. Itens deliberadamente fora do escopo

Não foram priorizados nesta entrega:

- autenticação/autorização;
- Kafka;
- Redis;
- Kubernetes;
- microserviços;
- migrations de produção;
- tracing distribuído;
- métricas avançadas;
- circuit breaker;
- retry distribuído;
- lock otimista completo;
- testes de concorrência avançados;
- Testcontainers.

Esses itens podem ser relevantes em uma solução de produção, mas não foram necessários para demonstrar corretamente o núcleo do desafio.

---

## 26. Critério utilizado nas decisões

As decisões priorizaram:

1. correção financeira;
2. integridade dos dados;
3. aderência aos requisitos;
4. código que possa ser explicado em uma revisão técnica;
5. simplicidade operacional;
6. evolução futura sem abstração prematura.
