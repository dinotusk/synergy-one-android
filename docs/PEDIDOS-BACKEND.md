# Pedidos ao backend (propostas, não contratos)

> Data: 16/10/2026. Tudo abaixo é **proposta** para o backend avaliar. O app **não** chama nada disso hoje e não inventa
> rotas. Padrão pedido: `/api/app/v1/...`, Bearer do Supabase, leitura sob a RLS (cliente só vê a própria empresa),
> paginação como na seção 3 de `API-APP.md`, erros como na seção 2, `404` para empresa que a pessoa não vê, datas em Brasília.

## 1. Publicar `GET /site/metricas` e esclarecer o contrato
- A rota está no `main` do backend (`f4f62c6`) e responde **404 em produção**. O app só a integra depois de publicada.
- Confirmar a **unidade de `vendas.valor`** (o exemplo mostra 4800: reais ou centavos?). Enquanto isso o app **não exibe valor**.
- `origensDeAcesso.utmSource/utmCampaign` usam `{ valor, paginasVistas }` no código e `host` só em `referencias`; manter assim no doc.
- Confirmar que a coleta só começa a ter números depois que o site e o n8n enviarem os eventos (os leads atuais seguem "Origem não identificada").

## 2. Indicadores do cliente
- Contrato final e endpoint do que hoje o app calcula de `GET /leads` (teto de 1.000 registros; funil pela etapa atual, não acumulado).
- Pedido: totais agregados por período (`desde`/`ate`) com leads captados, ganhos, perdidos, conversão da coorte, vendas fechadas no período, valor, tempo até a venda, funil acumulado (`etapaMaxima`) e origem; sempre com `null` para "sem dado".
- Dizer se o campo `indicadores` de `GET /termometro` (sem avaliação) **substitui** o cálculo local e se aceita período.

## 3. Entregas do cliente (`/cliente/entregas`)
Hoje só em server function do site. Proposta (leitura): `GET /entregas?empresaId=&cursor=&limite=`
```json
{ "entregas": [{ "id": 1, "titulo": "…", "etapa": "em_andamento", "progressoPct": 40, "prazo": "2026-10-30", "responsavel": "…" }], "playbook": { "diaAtual": 12, "total": 30 }, "total": 8, "proximoCursor": null }
```
Só leitura; editar continua da equipe.

## 4. Biblioteca do cliente
Hoje a web filtra `visivelCliente` em server function. Proposta: `GET /biblioteca?empresaId=&cursor=` só com itens liberados ao cliente:
`{ "itens": [{ "id", "titulo", "tipo", "area", "url" ou "arquivoUrl" (assinada, com validade), "atualizadoEm" }] }`. O arquivo precisa de URL assinada de curta duração; o app não deve receber chave de storage.

## 5. Mídias e relatórios do cliente
Proposta: `GET /midias?empresaId=` com métricas por rede (seguidores, publicações recentes, período) e `GET /relatorios?empresaId=` com título, período e link. Só para empresas com `modulos.midias`.

## 6. Agenda e Atendimento do cliente
No site, `/agenda` e `/atendimento` são **da equipe**; não há página de cliente. **Decisão de produto** antes de qualquer rota: o cliente deve ver prazos e chamados? Se sim, propor `GET /agenda` (prazos das entregas da empresa) e `GET /atendimento` (chamados da empresa, com abertura em rota própria, `POST`, só se for requisito).

## 7. Outros já listados
`GET /equipe/jornada/{id}` devolver estado e contato do lead; demais módulos da equipe ("Na web") conforme prioridade do produto.
