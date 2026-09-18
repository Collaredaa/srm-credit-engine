# AI_USAGE.md

## Uso de Inteligência Artificial no desenvolvimento

Durante o desenvolvimento do SRM Credit Engine, utilizei ferramentas de IA como apoio para acelerar tarefas de implementação, revisão de código, geração de testes e análise de possíveis problemas.

A IA foi utilizada como uma ferramenta de suporte ao desenvolvimento, e não como fonte única de decisão. As alterações geradas foram revisadas antes de serem aceitas e os principais fluxos foram validados através de testes automatizados.

---

## 1. Estratégia de uso

O uso de IA foi dividido principalmente em quatro tipos de atividade:

- geração inicial de estruturas de código a partir de requisitos bem definidos;
- revisão técnica do código gerado;
- criação e ampliação de testes automatizados;
- auditorias do projeto antes da entrega.

Procurei fornecer prompts com escopo fechado para evitar que a ferramenta adicionasse abstrações ou tecnologias que não fossem necessárias para o desafio.

Por exemplo, durante a implementação do Pricing Engine, defini explicitamente:

- Java 21;
- uso obrigatório de `BigDecimal`;
- `RoundingMode.HALF_EVEN`;
- Strategy Pattern;
- proibição de `double`, `float` e `Math.pow`;
- ausência de banco, controller e frontend naquela etapa;
- Golden Cases que deveriam ser atendidos.

Isso permitiu utilizar a IA para acelerar a implementação sem deixar que ela definisse sozinha as regras financeiras do sistema.

---

## 2. Exemplos de prompts estratégicos

### Pricing Engine

Um dos prompts utilizados definiu que o motor deveria:

- aplicar a fórmula de valor presente;
- utilizar taxa base de 1% a.m.;
- utilizar spread de 1,5% para Duplicata Mercantil;
- utilizar spread de 2,5% para Cheque Pré-datado;
- utilizar Strategy Pattern;
- trabalhar exclusivamente com `BigDecimal`;
- arredondar resultados monetários com `HALF_EVEN`;
- validar os Golden Cases C1 e C2.

Também determinei explicitamente que a IA não deveria implementar câmbio, banco de dados ou controllers nessa etapa.

---

### Currency Engine

Na implementação do câmbio, o prompt separou a responsabilidade do Pricing Engine e definiu que:

- a taxa `5.4321` representa `1 USD = 5.4321 BRL`;
- a conversão de BRL para USD deveria ocorrer por divisão;
- o Golden Case C3 deveria resultar em `17094.67 USD`;
- a lógica financeira não deveria ser duplicada;
- nenhuma persistência deveria ser implementada naquele momento.

---

### Settlement e idempotência

Para o fluxo de liquidação, o prompt determinou:

- uso de `@Transactional`;
- persistência do snapshot financeiro;
- `idempotencyKey`;
- constraint `UNIQUE` no banco;
- proteção para impedir mais de um settlement do mesmo recebível;
- liquidação em BRL e USD;
- reutilização de `PricingService` e `CurrencyConversionService`;
- ausência de lógica financeira em controllers.

---

### Auditoria final

Antes da entrega, também utilizei IA como ferramenta de revisão.

A auditoria procurou especificamente por:

- bugs financeiros;
- inconsistências de prazo;
- uso indevido de `double` ou `float`;
- problemas de idempotência;
- duplicação de lógica;
- arquivos gerados ou secrets commitados;
- problemas de CORS;
- inconsistências entre frontend e backend;
- cobertura dos Golden Cases;
- documentação ausente.

A auditoria identificou um problema real no cálculo de prazo, que foi corrigido e coberto por novos testes.

---

## 3. Casos em que a IA produziu algo inadequado

### 3.1. Validação incorreta de data de vencimento

O cálculo inicial de prazo utilizava:

```java
ChronoUnit.MONTHS.between(currentDate, dueDate)
```

e verificava apenas se o resultado era negativo.

O problema é que uma data já vencida dentro do mesmo mês pode resultar em `0` meses.

Exemplo:

```text
Data atual: 18/09
Vencimento: 17/09
ChronoUnit.MONTHS = 0
```

Isso permitiria precificar um recebível já vencido.

O problema foi identificado durante a auditoria final.

A solução adotada foi validar explicitamente:

```java
if (dueDate.isBefore(currentDate)) {
    throw new IllegalArgumentException(
        "dueDate must not be before the current date");
}
```

Também foram adicionados testes para:

- vencimento ontem;
- vencimento anterior no mesmo mês;
- vencimento hoje;
- vencimento futuro no mesmo mês;
- vencimento em três meses.

---

### 3.2. Implementação REST sobre base desatualizada

Em uma etapa, a IA informou que não encontrou classes como:

- `Receivable`;
- `Settlement`;
- `ExchangeRateEntity`;
- repositories relacionados.

Essas classes já haviam sido desenvolvidas em outra feature.

A causa era o contexto do workspace/branch utilizado naquele momento.

Antes de aceitar a implementação, comparei o código atual com o commit histórico da feature de settlement.

A revisão confirmou que as regras importantes haviam sido preservadas:

- `@Transactional`;
- snapshot financeiro;
- idempotência;
- constraints de banco;
- validação de recebível `OPEN`;
- conversão cambial;
- reutilização dos serviços existentes.

Esse caso mostrou que o código gerado pela IA não deve ser aceito somente porque compila ou porque os testes passam.

---

### 3.3. Representação da direção da taxa de câmbio

Em uma versão inicial, a taxa `5.4321` foi representada conceitualmente como:

```text
BRL -> USD
```

Porém o significado correto da taxa utilizada é:

```text
1 USD = 5.4321 BRL
USD -> BRL
```

Apesar de a operação matemática de divisão produzir o resultado esperado, a representação semântica estava incorreta.

A modelagem foi ajustada para representar a taxa como `USD -> BRL`, mantendo a conversão:

```text
USD = BRL / taxa
```

Isso tornou o modelo mais coerente e mais fácil de auditar.

---

### 3.4. Teste de contexto sem valor

Em uma etapa da implementação REST, o teste padrão do Spring teve o `@SpringBootTest` removido e passou a verificar apenas que:

```java
CreditEngineApplication.class != null
```

Embora o teste passasse, ele deixou de validar o contexto Spring.

Durante a revisão, considerei esse teste sem valor técnico e optei por removê-lo em vez de manter um teste artificial apenas para deixar a suíte verde.

---

## 4. Decisões que não foram delegadas à IA

Algumas decisões foram deliberadamente mantidas sob revisão manual.

### Regras financeiras

A definição final das regras de:

- taxa base;
- spreads;
- fórmula de valor presente;
- arredondamento;
- direção da taxa cambial;
- Golden Cases;

foi conferida contra a especificação do desafio.

A IA foi utilizada para implementar essas regras, mas não para decidir quais deveriam ser.

---

### Arquitetura

A decisão de manter o projeto como um monólito modular também não foi delegada.

Apesar de microserviços serem uma possibilidade, eles adicionariam complexidade desnecessária para o escopo do desafio.

Foram priorizados:

- baixo acoplamento entre componentes;
- separação de responsabilidades;
- código simples de executar e explicar.

---

### Escopo

Também optei por não implementar nesta entrega:

- Kafka;
- Redis;
- Kubernetes;
- autenticação;
- observabilidade avançada;
- circuit breaker;
- migrations de produção;
- arquitetura distribuída.

Esses itens poderiam ser relevantes em uma evolução do produto, mas não eram necessários para entregar corretamente o escopo atual.

---

## 5. Processo de validação do código gerado

O fluxo utilizado durante o desenvolvimento foi:

```text
Requisito
   ↓
Prompt com escopo definido
   ↓
Código sugerido pela IA
   ↓
Revisão manual
   ↓
Execução de testes
   ↓
Correções
   ↓
Commit
```

Não fiz commits automaticamente a partir das sugestões da IA.

Antes dos commits principais foram executados testes como:

```bash
.\\gradlew.bat test
```

No frontend:

```bash
npm.cmd run build
npm.cmd run lint
```

Os três Golden Cases também possuem testes automatizados.

---

## 6. Limitações conhecidas

A revisão final também identificou pontos que seriam relevantes para uma aplicação de produção, mas foram mantidos fora do escopo desta entrega:

- ausência de testes de integração completos com banco real para validar rollback;
- `ddl-auto=update` em vez de migrations versionadas;
- concorrência idempotente pode resultar em conflito de constraint em uma condição de corrida;
- credenciais simples de PostgreSQL utilizadas apenas no ambiente local;
- observabilidade limitada.

Essas limitações foram mantidas conscientemente para priorizar simplicidade, clareza e entrega do escopo solicitado.

---

## 7. Conclusão

O uso de IA permitiu acelerar principalmente:

- boilerplate;
- criação de DTOs;
- implementação inicial de services;
- geração de testes;
- revisão técnica.

Por outro lado, os casos encontrados durante o desenvolvimento mostraram que uma resposta da IA não deve ser considerada correta apenas porque compila.

A principal abordagem adotada foi utilizar IA para gerar e revisar hipóteses de implementação, mantendo validação humana sobre regras financeiras, arquitetura, integridade dos dados e escopo final.
