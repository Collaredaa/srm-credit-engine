# SRM Credit Engine — Especificação Inicial

## 1. Objetivo

O SRM Credit Engine tem como objetivo permitir a precificação e liquidação de recebíveis de forma segura, precisa e auditável, suportando inicialmente pagamentos em BRL e USD.

O fluxo principal da aplicação será:

1. Cadastro ou seleção de um recebível.
2. Simulação do valor presente.
3. Aplicação da estratégia de precificação correspondente ao tipo do recebível.
4. Conversão cambial, quando a moeda de pagamento for diferente de BRL.
5. Liquidação do recebível.
6. Persistência dos dados utilizados no cálculo para fins de auditoria.

---

## 2. Premissas adotadas

### 2.1 Prazo

Para a primeira versão da aplicação, o prazo utilizado na fórmula de precificação será expresso em **meses inteiros**.

A decisão foi adotada para manter consistência com os casos de aferição fornecidos no desafio.

Em um cenário real, seria necessário definir com a área de negócio como tratar períodos incompletos, por exemplo, um recebível com vencimento em 45 dias.

Os Golden Cases sempre utilizarão exatamente as premissas definidas no enunciado.

### 2.2 Taxa base

A taxa base inicial será de:

`1,00% ao mês`

A aplicação não terá este valor espalhado ou hardcoded nas strategies.

A taxa deverá ser centralizada em configuração própria do domínio de precificação, permitindo futura alteração ou externalização para banco de dados ou serviço de configuração.

### 2.3 Spread por tipo de recebível

Inicialmente serão suportados:

* Duplicata Mercantil: `1,50% a.m.`
* Cheque Pré-datado: `2,50% a.m.`

Cada tipo será implementado através de uma `PricingStrategy`, evitando condicionais centralizadas baseadas no tipo do recebível.

A criação de novos tipos deverá exigir preferencialmente a implementação de uma nova Strategy, sem alteração do núcleo do motor de cálculo.

### 2.4 Fórmula

A fórmula utilizada será:

`Valor Presente = Valor de Face / (1 + Taxa Base + Spread)^Prazo`

As taxas serão representadas em formato decimal.

Exemplo:

* 1% = `0.01`
* 1,5% = `0.015`

### 2.5 Política de câmbio

Quando o recebível estiver denominado em BRL e o pagamento ocorrer em USD, o sistema utilizará a **taxa de câmbio válida mais recente no momento da liquidação**.

Uma taxa será considerada válida quando:

`effectiveAt <= settlementTimestamp`

A taxa utilizada será persistida junto à liquidação, incluindo:

* valor da taxa;
* moedas de origem e destino;
* data/hora de vigência;
* identificador do registro de câmbio.

Uma liquidação já concluída nunca será recalculada caso a taxa de câmbio seja posteriormente alterada.

Para os Golden Cases será utilizada exatamente a taxa de câmbio informada no teste.

### 2.6 Precisão monetária

Valores monetários não utilizarão tipos binários de ponto flutuante como `float` ou `double`.

Na aplicação Java será utilizado:

`BigDecimal`

No PostgreSQL será utilizado:

`NUMERIC`

Taxas também serão representadas utilizando `BigDecimal`.

Os valores serão construídos preferencialmente a partir de strings, por exemplo:

`new BigDecimal("0.015")`

evitando conversões originadas de `double`.

### 2.7 Arredondamento

A política padrão será:

`RoundingMode.HALF_EVEN`

Valores monetários finais terão duas casas decimais.

O arredondamento deverá acontecer somente ao final do cálculo financeiro, mantendo maior precisão durante as operações intermediárias.

No caso de conversão BRL → USD utilizado pelos Golden Cases:

1. Calcula-se o valor presente em BRL.
2. Arredonda-se o valor em BRL para duas casas utilizando HALF_EVEN.
3. Converte-se o valor já arredondado pela taxa BRL/USD.
4. Arredonda-se o resultado em USD para duas casas.

### 2.8 Liquidação

Uma liquidação será uma operação transacional.

Dentro da mesma transação deverão ocorrer, no mínimo:

* validação do recebível;
* validação de que o recebível ainda pode ser liquidado;
* criação do registro da liquidação;
* alteração do estado do recebível.

Caso qualquer uma dessas operações falhe, toda a transação deverá sofrer rollback.

Não poderá existir uma situação onde a liquidação foi registrada, mas o recebível permaneceu disponível para nova liquidação.

### 2.9 Idempotência

O endpoint responsável pela liquidação deverá aceitar uma chave de idempotência.

Exemplo:

`Idempotency-Key: UUID`

A repetição da mesma solicitação com a mesma chave não poderá criar duas liquidações.

Além da verificação na aplicação, deverá existir uma restrição `UNIQUE` no banco de dados para impedir duplicidade mesmo em cenários de concorrência.

### 2.10 Imutabilidade e auditoria

Liquidações concluídas serão consideradas registros imutáveis.

Não será disponibilizada operação de atualização de uma liquidação.

Cada liquidação armazenará um snapshot dos principais dados utilizados no cálculo, incluindo:

* valor de face;
* valor presente;
* valor efetivamente pago;
* moeda;
* taxa base;
* spread;
* taxa de câmbio utilizada, quando aplicável;
* data/hora da liquidação.

Caso seja necessário corrigir uma operação em um sistema real, deverá existir um fluxo explícito de cancelamento ou estorno, em vez da alteração direta do histórico.

---

## 3. Perguntas para a área de negócio

Em um projeto real, antes da implementação definitiva, eu validaria as seguintes questões:

1. Como deve ser calculado o prazo quando o vencimento não corresponde a um número inteiro de meses?
2. A taxa base é fixa, configurável pela mesa ou proveniente de algum indicador externo?
3. A taxa base pode variar conforme produto, cedente ou data da operação?
4. Qual fornecedor ou fonte oficial deve fornecer as taxas de câmbio?
5. Devemos utilizar a taxa vigente no momento da simulação, aprovação ou liquidação?
6. Existe alguma janela de validade para uma cotação cambial?
7. Uma simulação deve garantir a taxa de câmbio apresentada ao operador por determinado período?
8. Quais outras moedas deverão ser suportadas futuramente?
9. É permitido cancelar ou estornar uma liquidação?
10. Existem limites mínimos ou máximos para valores de recebíveis?
11. Um recebível pode ser parcialmente liquidado?
12. Existem regras adicionais de risco de acordo com o cedente?
13. Qual volume médio e máximo de liquidações esperado em produção?
14. Por quanto tempo os registros de auditoria devem ser mantidos?

---

## 4. Critérios de aceite

### 4.1 Corretude

Os três Golden Cases fornecidos no desafio devem ser reproduzidos exatamente ao centavo através de testes automatizados.

Valores monetários não poderão utilizar `float` ou `double`.

### 4.2 Usabilidade

O operador deverá conseguir:

* informar os dados necessários do recebível;
* visualizar o valor líquido antes da liquidação;
* visualizar o deságio;
* visualizar a moeda de pagamento;
* identificar claramente erros de validação;
* consultar o histórico das liquidações.

A interface deverá impedir múltiplos envios acidentais sempre que possível, embora a segurança contra duplicidade não dependa exclusivamente do frontend.

### 4.3 Segurança e integridade

A aplicação deverá:

* validar todos os inputs recebidos;
* utilizar queries parametrizadas;
* impedir liquidações duplicadas;
* garantir atomicidade através de transações;
* não retornar sucesso quando uma liquidação falhar;
* preservar o histórico financeiro;
* não expor detalhes internos desnecessários em mensagens de erro.

### 4.4 Desempenho

As consultas de histórico deverão ser paginadas no servidor.

Filtros frequentemente utilizados deverão possuir índices adequados quando necessário.

O fluxo síncrono de liquidação não deverá executar operações externas desnecessárias depois do início da transação de banco.

Integrações externas, como o provedor de câmbio, deverão possuir timeout definido.

### 4.5 Manutenibilidade

A lógica financeira deverá permanecer independente de controllers e persistência.

Novos tipos de recebíveis deverão poder ser adicionados através de novas estratégias de precificação sem alterar significativamente o motor existente.

As principais decisões arquiteturais e simplificações serão documentadas no `DECISIONS.md`.
