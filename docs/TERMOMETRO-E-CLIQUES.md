# Termômetro — especificação compartilhada web e Android

## Termômetro e cliques — requisito aprovado em 02/10/2026

**Status: planejado, ainda não implementado.** Deve ser a primeira visão da equipe e do cliente na web e no aplicativo Android.

### Metas e avaliação
- Mostrar empresa, período, indicador, meta e resultado no mesmo período, progresso, feedback e próximos passos.
- Equipe acompanha empresas autorizadas e filtra por empresa/período. Cliente vê somente avaliações publicadas das empresas a que tem acesso.
- Responsável pela avaliação preenche formulário: empresa, período, indicador, meta, resultado, classificação, justificativa, feedback e próximos passos. Registrar autoria e data; permitir rascunho e publicação.
- Manter histórico. Ausência de avaliação ou dado é estado neutro ("Ainda não avaliado"/"Dados indisponíveis"), nunca resultado zero ou classificação vermelha por padrão.
- Permissão de escrita deve ser confirmada pelo backend; esconder ações na interface não substitui autorização.

| Cor | Significado |
| --- | --- |
| Vermelha | Perigo ou resultado ruim |
| Amarela | Atenção; próximo de alcançar a meta |
| Verde | Meta alcançada; resultado bom |
| Azul | Meta ultrapassada; resultado muito bom |
| Lilás | Resultado excelente e oportunidade de inovar |

Usar texto e ícone junto à cor. Limites numéricos ainda não foram definidos; não inventar faixas. Lilás depende de avaliação e justificativa humana, não apenas de um percentual.

### Cliques, origem e evolução
- Cliques no link do Instagram, com país de origem quando a fonte fornecer.
- Cliques nos links de contato (WhatsApp, telefone, e-mail ou outros).
- Cliques no link do site oficial.
- Visitas e interações dentro do site são métricas separadas dos cliques que levam ao site. Não somar indicadores distintos como um único total.
- Apresentar período, origem, atualização do dado e evolução temporal; sem histórico, não fabricar gráfico.
- País desconhecido deve aparecer como "Não informado". Trabalhar preferencialmente com contagens agregadas, sem expor IP ou identificação pessoal.
- Dependência: confirmar com Yuri o fluxo, a fonte existente no Lovable, a identificação estável de cada link/empresa, a definição de clique (total ou único), fuso, deduplicação e disponibilidade do histórico. Essa confirmação ainda não foi realizada.
- Integração futura via backend/serviços do Synergy One. Nenhum segredo de n8n, service_role ou consulta ao schema central no aplicativo.

## Próxima tarefa para o Codex Android (UX/UI)

Trabalhe no projeto Android local existente. Antes de editar, confira git status e preserve alterações de terceiros. O Claude cuida da autenticação e do backend; esta etapa é exclusivamente UX/UI.

1. Transforme a primeira aba dos dashboards da equipe e do cliente em "Termômetro", preservando as outras três abas e o tema existente.
2. Crie componentes compartilhados de classificação (cinco cores com texto/ícone), meta versus resultado, feedback e histórico temporal.
3. Equipe: seleção de empresa/período, resumo e detalhe de avaliação. Cliente: somente leitura dos resultados publicados da própria empresa.
4. Prepare formulário de avaliação da equipe com os campos desta especificação. Sem API confirmada, ele deve permanecer em demonstração debug; não mostrar sucesso de gravação real nem permitir publicação fictícia na área de produção.
5. Isole exemplos em fixtures de debug/previews com selo "Demonstração". A interface de produção deve mostrar estado não configurado quando não houver serviço real. Não altere PendingAuthRepository nem invente endpoints para alcançar os dashboards.
6. Trate carregamento, vazio, erro, sem acesso e sem histórico. Não converta ausência em zero. Gráficos devem mostrar período/unidade e ter resumo acessível.
7. Preserve a logo oficial bitmap, sem redesenhar. Use animações discretas, sem atraso artificial. Confirme contraste, fonte ampliada, alvos de toque e celular pequeno.
8. Não altere autenticação, contratos existentes, applicationId, backend ou migrações. Não use service_role, segredos ou consultas ao central.
9. Valide testes unitários, lint e assembleDebug no ambiente Windows. Abra no emulador e apresente capturas dos dois perfis via demonstração debug. Relate exatamente o que foi validado.
10. Não faça merge nem push automático nesta etapa; apresente as alterações para revisão.

Esta especificação não define contrato HTTP do Termômetro. O backend ainda precisa fornecer esse contrato e as permissões de avaliação/publicação.

## Jornada do lead até a venda — requisito aprovado em 02/10/2026

**Status: planejado, ainda não implementado.** Mapear a jornada completa na web e no Android, conectando aquisição, atendimento, oportunidade, venda e eventual ativação do cliente.

### Etapas e histórico
- Fluxo de referência para validar com a operação: origem/captura → primeiro contato → qualificação → proposta → negociação → venda ganha ou perdida → ativação, quando aplicável.
- Reaproveitar e mapear os estados reais dos serviços existentes antes de definir novos estados. Esta sequência é uma referência de produto, não um contrato de API nem uma alteração das etapas atuais.
- Cada lead deve ter identificador estável, empresa vinculada quando conhecida, canal/fonte, campanha e link de origem quando disponíveis, responsável, data da captura e etapa atual.
- Linha do tempo com eventos, mudanças de etapa, contatos, propostas e resultado final; registrar quando aconteceu e quem ou qual integração registrou.
- Preservar origem e histórico ao converter o lead em cliente. Relacionar lead, oportunidade, contrato/venda e empresa por IDs; não por nomes.
- Distinguir empresa dona do lead de empresa criada/vinculada na conversão. Não trocar o vínculo de propriedade para representar uma venda.
- Registrar valor e data da venda quando disponíveis, motivo de perda e tempo em cada etapa. Venda ganha não significa pagamento recebido; recebimentos pertencem ao financeiro.
- Prever leads já convertidos, duplicados, reabertura e correções auditadas. Uma pessoa pode ter mais de uma oportunidade; evitar contar cada evento como um novo lead ou nova venda.

### Visão e indicadores
- Exibir funil e linha do tempo do lead, com filtros por empresa, origem, responsável e período.
- Mostrar leads recebidos, qualificados, propostas, vendas ganhas/perdidas, conversão e tempo até a venda; receita vendida apenas quando houver valores reais.
- Definir denominadores e distinguir conversão da mesma coorte de leads de vendas ocorridas no período, para não gerar taxas enganosas.
- Relacionar ao Termômetro metas de aquisição e conversão, resultados e feedback, com evolução temporal baseada em histórico real.
- Equipe vê dados conforme suas permissões. Cliente vê apenas os leads e informações autorizadas da própria empresa; notas internas não devem ser publicadas automaticamente.

### Dependências e limites
- Confirmar os eventos e identificadores disponíveis nas fontes, no n8n e no backend, incluindo a identificação de uma venda concluída.
- Cliques agregados por país não identificam pessoas. Só atribuir clique/campanha a um lead quando existir vínculo técnico verificável e coleta adequada; caso contrário, apresentar "Origem não identificada".
- Documentar a regra de atribuição (primeiro/último contato), retenção, minimização de dados e deduplicação antes da integração.
- Backend deve definir os contratos, autorização e auditoria. Esta atualização só registra requisitos: não cria endpoints, migrações nem telas.

## Acessos e cliques: decisão de arquitetura (atualização de 15/10/2026)

- O site da ForLife está no **Lovable**. A investigação e a integração da coleta de acessos e cliques são do
  responsável pelo backend/web do Synergy One.
- O app Android **só consome a API autenticada do Synergy One** (`/api/app/v1`). Não se conecta ao Lovable, ao n8n
  nem a qualquer outra fonte, e não guarda segredo de nenhuma delas.
- Enquanto a coleta não existir, `resultados.cliques.disponivel = false` e o app mostra o aviso da `pendencia`,
  nunca zero.

### Indicadores já exibidos (dados que a API entrega hoje)

| Indicador | Campo da API | Tela |
|---|---|---|
| Leads, vendas, conversão | `totalLeads`, `ganhos`, `conversao` | Resultados |
| Vendido, recebido, % recebido | `vendidoCentavos`, `recebidoCentavos` | Resultados |
| Tempo até a venda (média, mediana), perdidas, em andamento | `diasAteVenda`, `perdidos`, `emAndamento` | Resultados |
| Funil por etapa | `funil[]` | Resultados |
| Evolução de leads captados | `evolucao[].captados` | Gráfico "Leads captados" |
| Evolução de vendas ganhas | `evolucao[].ganhos` | Gráfico "Vendas ganhas" |
| Evolução do valor vendido | `evolucao[].vendidoCentavos` | Gráfico "Valor vendido" |
| Metas × realizado | `aplicacao.metas[]` | Metas |

Gráficos só são desenhados com **dois ou mais pontos** reais; sem isso aparece o aviso, não uma linha inventada.

### Dados que o backend precisa fornecer para os gráficos de acessos e cliques

Proposta de bloco `cliques` em `ResultadosDto` (e no `GET /equipe/termometro`), **sem dados pessoais** (só contagens
agregadas). O app passa a desenhar assim que `disponivel = true`:

```json
"cliques": {
  "disponivel": true,
  "atualizadoEm": "2026-10-15T12:00:00Z",
  "fuso": "America/Sao_Paulo",
  "definicao": "total | unico",
  "serie": [
    { "chave": "2026-10-01", "rotulo": "01/10",
      "instagram": 0, "contato": 0, "site": 0 }
  ],
  "porPais": [ { "pais": "BR", "instagram": 0, "contato": 0, "site": 0 } ],
  "porLink": [ { "linkId": "…", "tipo": "instagram|contato|site", "rotulo": "…", "total": 0 } ]
}
```

Requisitos e decisões que dependem do **usuário** (não do Yuri) e do backend, ainda em aberto:
1. Definição de clique: total ou único (e janela de unicidade) e o fuso do dia.
2. Deduplicação (robôs, recarga, mesmo visitante) e fonte da coleta no Lovable.
3. Identificação estável de cada link por empresa (`linkId`) e de qual empresa é cada clique.
4. Histórico disponível (a partir de quando) e frequência de atualização (`atualizadoEm`).
5. País desconhecido = "Não informado". Visitas ao site e cliques para o site são métricas **separadas**; não somar.
6. Permissões: o cliente vê só a própria empresa; equipe conforme o papel. Sem IP nem identificação pessoal.
7. Atribuir clique a lead só com vínculo técnico verificável; senão "Origem não identificada".

Para **acessos** (visitas ao site) pede-se o mesmo formato (`acessos.serie`, `porPais`), com `definicao` própria
(visita × sessão × visitante único).

## Indicadores atuais do cliente (sem avaliação publicada): definições

O cliente não tem rota de resultados ao vivo (`/equipe/resultados` é só da equipe). Por isso, fora de uma avaliação
publicada, o app calcula os indicadores a partir de `GET /api/app/v1/leads`. Funciona para **qualquer empresa
autorizada** do usuário (sem regra por empresa); o período é independente da avaliação.

### Campos de `/leads` usados

| Campo | Uso |
|---|---|
| `capturadoEm` (instante ISO) | base dos leads captados no período, funil, origem e gráfico de captação |
| `etapa` | classificação atual (`ganho`, `perdido`, em andamento) |
| `vendaFechadaEm` (`AAAA-MM-DD`) | **data real do fechamento**: base das vendas do período |
| `vendaCentavos` | valor vendido (soma só o que existe; vendas sem valor são avisadas) |
| `origem`, `canal` | origem real; "legado" nunca é exibido como canal; sem origem = "Origem não informada" |

### Duas bases, nunca misturadas (mesma regra de `calcularResultados` do site)

1. **Leads captados no período** (por `capturadoEm`): leads, já ganhos, perdidos, em andamento, funil, origem,
   gráfico de captação e **conversão = ganhos desses leads ÷ leads captados no período**. O tempo até a venda
   também é dos ganhos dessa coorte (captura → fechamento).
2. **Vendas fechadas no período** (por `vendaFechadaEm`): quantidade, valor vendido e gráficos por data de
   fechamento, **inclusive de leads captados antes do período**. Para não perder essas vendas, o app lê todos os
   ganhos (`etapa=ganho`, sem `desde`) e filtra pela data de fechamento. Ganho sem `vendaFechadaEm` não entra em
   nenhum período: aparece em aviso.

A conversão **não** é "vendas do período ÷ leads do período".

Diferença conhecida em relação ao site: o funil do site é acumulado (`etapaMaxima`), que `/leads` não entrega;
no app o funil mostra a **etapa atual** de cada lead, com legenda explícita.

Fuso: `America/Sao_Paulo` em todas as datas (captura convertida do instante UTC; fechamento já é data).

### Limite de leitura e agregação no backend

O app lê no máximo **10 páginas de 100 (1.000 registros)** por consulta. Se a lista não couber, **nenhum total,
percentual ou gráfico é exibido** para aquele bloco, só o aviso da limitação (nunca números parciais como
completos). **Dependência do backend:** para períodos longos e carteiras grandes, é preciso uma rota agregada para o
cliente (ex.: `GET /api/app/v1/resultados?empresaId&desde&ate`, no formato de `/equipe/resultados`, e com `etapaMaxima`
no funil), calculada no servidor com a mesma regra acima.

### Títulos na interface

- Equipe: **Resultados ao vivo do período** (calculado pelo servidor).
- Cliente com avaliação: **Números congelados na publicação** (do momento da publicação).
- Cliente sem avaliação ou em período livre: **Indicadores atuais do período**, com aviso "calculados agora, não congelados".
