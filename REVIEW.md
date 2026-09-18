Revisão crítica do Anexo A

O código apresentado no Anexo A foi tratado como uma implementação propositalmente problemática.

A revisão abaixo prioriza riscos financeiros, segurança, consistência transacional, idempotência, auditabilidade e qualidade da API.

1. SQL Injection — criticidade alta

O código monta consulta SQL através de interpolação/concatenação de valores recebidos externamente.

Risco

Um valor malicioso pode modificar a consulta executada pelo banco.

Além de comprometer dados, esse tipo de falha pode permitir leitura ou alteração indevida de informações.

Correção proposta

Utilizar queries parametrizadas ou, no contexto desta solução, Spring Data JPA/repositories.

Nenhum valor recebido pela API deve ser concatenado diretamente em SQL.

2. Uso de ponto flutuante para dinheiro — criticidade alta

A implementação utiliza tipos numéricos binários para cálculos financeiros.

Risco

number, double e float não representam todos os valores decimais exatamente.

Pequenas diferenças podem se acumular em:

juros;

descontos;

conversão cambial;

arredondamentos;

reconciliação.

Correção proposta

No backend financeiro, utilizar BigDecimal com escala e arredondamento explicitamente definidos.

3. Interpretação incorreta de percentuais — criticidade alta

Taxas como 1.0 e 1.5 podem ser interpretadas diretamente como fatores numéricos.

Para uma taxa de 1%, o valor correto utilizado na fórmula é:

0.01

e para 1,5%:

0.015

Risco

Interpretar 1.0 como 1% transforma a taxa em 100%, alterando completamente o resultado da operação.

Correção proposta

Representar percentuais em forma decimal e centralizar as taxas em componentes de domínio.

4. Fórmula financeira sem política explícita de precisão — criticidade alta

O Anexo A utiliza operações matemáticas sem uma política financeira clara para precisão e arredondamento.

Risco

Mesmo quando a fórmula conceitual está correta, arredondamentos diferentes podem produzir resultados divergentes dos Golden Cases.

Correção proposta

Definir explicitamente:

tipo decimal;

precisão intermediária;

momento do arredondamento;

escala monetária;

modo de arredondamento.

Nesta solução foi utilizado BigDecimal e HALF_EVEN.

5. Falta de transação ACID na liquidação — criticidade alta

A implementação executa etapas da liquidação sem garantir uma única transação de banco.

Risco

Pode ocorrer situação como:

settlement salvo;

falha ao atualizar o recebível;

banco fica parcialmente atualizado.

Também pode ocorrer o inverso.

Correção proposta

Executar todo o caso de uso dentro de uma transação única.

Na implementação atual, o serviço de settlement utiliza @Transactional.

6. Ausência de idempotência — criticidade alta

O Anexo A não protege adequadamente contra repetição da mesma requisição.

Risco

Retries de rede, duplo clique ou repetição de chamada podem criar duas liquidações para a mesma operação.

Correção proposta

Utilizar:

Idempotency-Key;

consulta da chave antes da criação;

constraint única no banco;

proteção adicional de unicidade por recebível.

7. Ausência de proteção de concorrência — criticidade alta

Mesmo com uma validação em memória, duas requisições simultâneas podem observar o mesmo estado antes de qualquer uma persistir a mudança.

Risco

Liquidação duplicada.

Correção proposta

A integridade final não deve depender apenas de if.

Constraints de banco são necessárias.

Para cenários mais avançados também poderiam ser considerados lock otimista/pessimista, dependendo dos requisitos.

8. Uso da taxa de câmbio “mais recente” sem respeitar vigência — criticidade alta

Buscar simplesmente o último câmbio cadastrado não garante que ele era válido no instante da operação.

Risco

Uma liquidação pode utilizar uma taxa futura ou diferente da que estava vigente no momento correto.

Correção proposta

Buscar a taxa mais recente com:

effectiveAt <= instante da operação

e persistir a taxa efetivamente utilizada no snapshot.

9. Falta de snapshot financeiro — criticidade alta

O Anexo A depende de dados mutáveis para reconstruir uma operação.

Risco

Se a taxa cambial ou alguma regra mudar depois, a consulta histórica pode deixar de reproduzir o valor que realmente foi utilizado.

Correção proposta

Persistir no settlement os dados relevantes da operação:

valor de face;

valor presente;

valor pago;

moeda;

taxa base;

spread;

câmbio;

vigência do câmbio;

instante da liquidação.

10. Exceções engolidas — criticidade alta

Capturar uma exceção e continuar o fluxo sem propagação adequada esconde falhas reais.

Risco

A aplicação pode aparentar sucesso mesmo após falhar parcialmente.

Também dificulta diagnóstico e monitoramento.

Correção proposta

Não engolir exceções.

Erros devem provocar rollback quando necessário e ser transformados em respostas HTTP coerentes na borda da aplicação.

11. Retorno HTTP 200 em cenários de erro — criticidade alta

A implementação não diferencia corretamente sucesso e falha através do protocolo HTTP.

Risco

Clientes podem interpretar uma operação rejeitada como concluída.

Correção proposta

Utilizar status adequados, por exemplo:

200 -> consulta/simulação
201 -> criação
400 -> requisição inválida
409 -> conflito de estado ou integridade

12. Ausência de validação de entrada — criticidade alta

Campos financeiros e de domínio precisam ser validados antes da execução.

Exemplos:

valor de face nulo ou negativo;

taxa cambial menor ou igual a zero;

moeda ausente;

tipo de recebível inválido;

vencimento no passado;

identificadores ausentes.

Risco

Estados inválidos podem chegar ao domínio ou ao banco.

Correção proposta

Utilizar Bean Validation nos DTOs e validações adicionais na camada de domínio/service quando a regra depende do contexto.

13. Fallback silencioso para tipo de recebível — criticidade alta

Uma entrada desconhecida não deve automaticamente utilizar a regra de outro produto.

Risco

Um erro de entrada pode gerar precificação válida sintaticamente, porém financeiramente errada.

Correção proposta

Tipos desconhecidos devem ser rejeitados explicitamente.

Cada tipo suportado deve possuir sua própria estratégia registrada.

14. Mistura de responsabilidades — criticidade média/alta

O Anexo A concentra acesso a dados, cálculo financeiro, câmbio e controle HTTP em um mesmo fluxo.

Risco

Isso aumenta acoplamento e dificulta:

testes;

evolução;

auditoria;

reutilização da regra financeira.

Correção proposta

Separar responsabilidades em componentes claros:

Pricing Engine;

Currency Engine;

persistência;

Settlement Service;

Controllers.

15. Regra cambial acoplada ao pricing — criticidade média

Precificação e conversão de moeda representam responsabilidades diferentes.

Risco

Novas moedas ou políticas cambiais passam a exigir alteração do motor de pricing.

Correção proposta

Calcular primeiro o valor presente na moeda-base e converter apenas depois através de um serviço específico.

16. Ausência de auditabilidade — criticidade alta

Sem registro imutável da decisão financeira, não existe evidência suficiente para explicar uma operação histórica.

Risco

Dificuldade em responder:

qual taxa foi usada?

qual spread foi aplicado?

qual câmbio estava vigente?

qual valor presente foi calculado?

quando a operação ocorreu?

Correção proposta

Persistir snapshots e evitar atualização do settlement concluído.

17. Uso de toFixed como solução financeira — criticidade alta

Formatar um número com duas casas não resolve problemas de precisão anteriores.

Risco

O valor pode já ter acumulado erro binário antes da formatação.

Correção proposta

Precisão deve ser garantida durante o cálculo financeiro, não apenas na apresentação final.

18. Falta de separação entre simulação e liquidação — criticidade média

Uma simulação não deve produzir efeitos colaterais.

Risco

Um usuário apenas consultando o valor poderia alterar estado ou gerar registros financeiros.

Correção proposta

Criar um caso de uso separado para simulação, reutilizando os mesmos motores financeiros, porém sem persistir recebível ou settlement.

19. Política de prazo não explicitada — criticidade média

O cálculo financeiro depende diretamente do prazo.

Sem definição clara de unidade e comportamento de datas, diferentes implementações podem gerar valores diferentes.

Correção proposta

Documentar a regra e centralizá-la.

Nesta solução:

prazo = meses inteiros

e datas anteriores ao dia atual são rejeitadas.

20. Falta de testes de referência — criticidade alta

Sem casos conhecidos, alterações de implementação podem modificar resultados financeiros sem serem detectadas.

Correção proposta

Manter Golden Cases automatizados para os cenários fornecidos pelo desafio.

Os casos C1, C2 e C3 são tratados como contratos de regressão do motor financeiro.

Priorização das correções

P0 — integridade e dinheiro

Devem ser corrigidos antes de qualquer evolução:

SQL Injection;

ponto flutuante em dinheiro;

percentuais incorretos;

ausência de transação;

ausência de idempotência;

ausência de constraints contra duplicidade;

câmbio sem vigência;

exceções engolidas;

HTTP 200 em erro;

ausência de snapshot.

P1 — qualidade do domínio

Em seguida:

validação de entrada;

Strategy por tipo;

separação Pricing/Currency;

regra de prazo;

simulação sem side effect;

Golden Cases automatizados.

P2 — evolução

Depois da correção do núcleo:

observabilidade;

migrations;

testes de integração adicionais;

estratégia de concorrência mais sofisticada;

resiliência externa.

Conclusão

O principal problema do Anexo A não é estilo de código.

Os riscos mais relevantes afetam diretamente:

valor financeiro calculado;

integridade da liquidação;

segurança do banco;

repetição de operações;

capacidade de auditoria.

A solução desenvolvida para o desafio prioriza exatamente esses pontos através de BigDecimal, Strategy Pattern, transação, idempotência, constraints de banco, vigência cambial e snapshot financeiro.