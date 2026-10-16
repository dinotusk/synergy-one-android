# Cobertura funcional: site × app Android

> **Inventário de 16/10/2026** · **Backend comparado:** `origin/main` de `SynergyEcosystem/synergy-one` no commit `f4f62c6`
> (`docs/API-APP.md`, `docs/SITE-COLETA.md` e as rotas em `src/routes/api.app.v1.*`) · **Produção testada sem login** em
> `www.synergyecosystem.com.br` só para saber se a rota existe (404 = não publicada, 401 = publicada).
> **Nada aqui foi validado com contas de teste de equipe**; a conta de cliente real foi usada só para leitura (ver `ACABAMENTO.md`).

## Classes

| Classe | Significado |
|---|---|
| **A. Nativo e funcional** | Funciona dentro do app, com API real e as permissões do servidor |
| **B. API existente, integração pendente** | Há rota no contrato, mas o app ainda não a usa (ou ela ainda não está publicada) |
| **C. Sem API** | O backend não tem rota `/api/app/v1`; depende do backend. **Link para o site não conta como módulo nativo** |
| **D. Só pelo site** | Abre no navegador (rótulo "Na web"/"Site" no app); o login do site é separado |

## Visão do cliente (prioridade)

| Módulo | Classe | Backend hoje | O que o app faz | Próximo passo |
|---|---|---|---|---|
| Termômetro publicado (avaliação, metas, congelados) | **A** | `GET /termometro` (publicado) | Nativo | Validar com mais contas |
| Indicadores do período (leads, ganhos, conversão, funil, origem) | **A, provisório** | `GET /leads` (o app calcula) e, **novo**, `indicadores` em `GET /termometro` sem avaliação | Cálculo local com teto de 1.000 leads | **Em revisão no backend**: integrar só após contrato final e endpoint; não ampliar o cálculo local |
| Acessos e cliques do site | **B** | `GET /site/metricas`, no `main` do backend e **404 em produção** (não publicada) | Aviso "Cliques ainda indisponíveis" | Integração por etapas (abaixo): modelos, repositório e seção; só aparece com dados |
| Leads (lista e detalhe) | **A** | `GET /leads` | Nativo; o cliente abre um **detalhe somente leitura** (contato, mensagem, origem, etapa e venda do que a lista já traz; sem histórico, notas nem recebimentos, que são rotas da equipe), origem real, "Entrou N×"; filtro por etapa e **por janela de captura** (7/30/90 dias, via `desde`, validado no aparelho com conta real) | Busca por nome (a API não tem) e data final (a API só aceita `desde`) |
| Entregas (playbook de 30 dias) | **C** | Nenhuma rota; no site vem de server function interna (`cliente.entregas`) | Aba explica e abre `/cliente/entregas` (**Site**) | Pedido ao backend, item 3 |
| Biblioteca (materiais liberados) | **C** | Nenhuma; o site filtra `visivelCliente` em server function | **Site** (`/cliente`) | Pedido ao backend, item 4 |
| Mídias e relatórios | **C** | Nenhuma | **Site**, só se a empresa tem o módulo `midias` | Pedido ao backend, item 5 |
| Agenda | **C**, sem versão para cliente | Nenhuma; `/agenda` do site é da **equipe**, não há página de agenda para cliente | Não aparece para o cliente | **Decisão de produto**: cliente verá agenda? (item 6) |
| Atendimento | **C**, sem versão para cliente | Nenhuma; `/atendimento` é da **equipe** | Não aparece para o cliente | **Decisão de produto** (item 6) |
| Pré-análise, Concorrentes, API e webhooks, Configurações da empresa | **D** | Nenhuma | **Site**, respeitando `empresa.modulos` | Fora do escopo desta etapa |
| Conta (perfil, tema, sair, recuperar senha, termos) | **A** / **D** | `GET /me`; recuperação e termos abrem o site | Nativo; senha e legais abrem o navegador | n/a |

## Visão da equipe

| Módulo | Classe | Backend hoje | O que o app faz |
|---|---|---|---|
| Termômetro (resultados ao vivo, avaliações, publicar) | **A** | `GET /equipe/termometro`, `POST/PUT …/avaliacoes`, `…/publicar` | Nativo, com `podeEditar`/`podePublicar` |
| Empresas e ficha | **A** (parcial) | `GET /empresas`, `/termometro`, `/leads` | Lista, ficha e leads; carteira/Cliente 360 completos só no site |
| Leads e vendas, jornada, notas, recebimentos | **A** | `GET /leads`, `/equipe/jornada*`, `PUT`, `POST …/notas`, `…/recebimentos` | Nativo, com filtro por janela de captura (7/30/90 dias) |
| Acessos e cliques do site (por empresa) | **B** | `GET /site/metricas` (não publicada) | Aviso; seção após publicação |
| Atendimento, Agenda, Projetos e entregas, Biblioteca, Métodos, Meu dia, Inbox, A.U.R.A., SNI, Plataformas/Instagram/YouTube | **C** (aparecem como **D**) | Nenhuma | "Na web": abre o site; não é módulo nativo |
| Gestão, Financeiro, Equipe, Sistemas e automações, API e chaves, Configurações | **C** (aparecem como **D**) | Nenhuma | "Na web" |

## Primeira integração desta etapa
`GET /site/metricas` é a única rota nova do contrato, mas **não está publicada** (404 em produção), então não dá para
integrá-la nem validá-la com dados reais agora; fica para quando o backend publicar (pedido 1). A primeira integração foi, então,
a que o contrato já oferece e ainda não tinha tela: o parâmetro **`desde` de `GET /leads`**, como filtro "Captados: Sempre,
7 dias, 30 dias, 90 dias" na lista de Leads (cliente e equipe). Conferido no aparelho com a conta de cliente (ForLife, só
leitura): Sempre = 7 leads, 30 dias = 5 (igual ao Termômetro de 17/09–16/10), 7 dias = 4. Sem `ate` na API, não há data final.

## Detalhes da rota nova: `GET /api/app/v1/site/metricas`
Parâmetros `empresaId`, `siteId`, `de`, `ate` (`AAAA-MM-DD`, máx. 366 dias) e `agrupar=dia|total`; sem datas, últimos 30 dias em
Brasília. Erros: 404 empresa/site que a pessoa não vê, 422 parâmetros, 403 sem cadastro, 503 leitura indisponível. Mesma
autenticação e isolamento por empresa. Regras que o app respeita: `null` = **sem dado** (nunca zero); sessões não são pessoas
únicas; mostrar `coleta.observacao` quando `parcial`; "Origem não identificada" exatamente como vem; não existe "leads por
clique"; `pais.disponivel=false` → "indisponível". **A coleta ainda não recebe dados**: nenhum workflow do n8n foi alterado e os
leads atuais seguem "Origem não identificada" (`SITE-COLETA.md`, seção 6).

## Solicitação objetiva ao backend (sem inventar endpoints)
Detalhe e formato em `docs/PEDIDOS-BACKEND.md`. Resumo:
1. **Publicar** `GET /site/metricas` (404 em produção) e confirmar a unidade de `vendas.valor` (reais ou centavos).
2. **Indicadores do cliente**: contrato final e endpoint (inclui `etapaMaxima` para o funil acumulado); dizer se `indicadores` em `/termometro` substitui o cálculo local.
3. **Entregas do cliente**: `GET /cliente/entregas` (somente leitura, RLS do cliente) com etapa, andamento e prazo.
4. **Biblioteca do cliente**: `GET /biblioteca` só com `visivelCliente`, com título, tipo, link/arquivo e data.
5. **Mídias e relatórios do cliente**: `GET /midias` e relatórios (métricas por rede) por empresa, respeitando `modulos.midias`.
6. **Agenda e Atendimento do cliente**: decisão de produto sobre existirem para o cliente; se sim, `GET /agenda` e `GET /atendimento` (chamados abertos pela empresa).
7. `GET /equipe/jornada/{id}` devolver estado e contato do lead; endpoints da equipe para os módulos "Na web" conforme a prioridade.

## Como ler

- **Perfil/permissão** é o que o **servidor** exige (`exigirMembroEquipe`, `exigirEquipe("escrita"|"admin")`,
  `exigirAdministrador`, ou RLS para o cliente), não o que o menu do site mostra. O filtro de menu do site
  (`canAccess`) usa uma matriz de demonstração e **não vale como regra**.
  - `membro` = qualquer pessoa ativa da equipe (inclui `leitor`) · `escrita` = equipe exceto `leitor` ·
    `admin` = `admin` ou `gestao` · `administrador` = só `admin` · `cliente` = só as empresas dele (RLS).
- **API existente** = rota em `/api/app/v1/*`. Tudo o que não tem rota ali **não pode** ser feito pelo app
  sem o backend criar o endpoint (as *server functions* do site são internas e instáveis: proibidas).
- **Android** descreve o estado real (leitura, ações, detalhe, filtros e permissões), não a existência de aba.
- Telas do site que só redirecionam (`/acoes`, `/funil`, `/leads`, `/war-room`, `/studio`, `/social`,
  `/signals`, `/usuarios`, `/contratos`, `/contabil`, `/alc`, `/afiliados`, `/concorrentes`,
  `/convergencia`, `/ecossistema`, `/esteiras`, `/eventos`, `/gargalos`, `/portal`) **não são módulos**.
  O "Portal do afiliado" (`/portal/*`) não tem perfil real no banco (só existem `equipe` e `cliente`): fora de escopo.

## Rotas de API disponíveis

`GET /me`, `/empresas`, `/leads`, `/termometro`, `/site/metricas` (todos os perfis; `/site/metricas` ainda não publicada em produção) · `GET /equipe/jornada`, `/equipe/jornada/{id}`,
`/equipe/resultados`, `/equipe/termometro` (equipe) · `PUT /equipe/jornada/{id}`, `POST …/notas`, `POST …/recebimentos`,
`POST/PUT /equipe/termometro/avaliacoes`, `POST …/{id}/publicar` (equipe com escrita; publicar = admin/gestão).

## Navegação do app

- **Equipe:** Termômetro · Clientes (Empresas | Leads e vendas) · Operação · Mais.
- **Cliente:** Termômetro · Leads · Entregas · Mais.
- **Mais:** perfil e configurações, "Minha empresa" (cliente) e todos os módulos por categoria, filtrados pelo
  papel (equipe) ou pelos módulos que a empresa habilitou (cliente). Cada item faz algo: abre a aba nativa
  ou a página do site ("Site"). O perfil também abre pelo avatar.
- **Operação** (equipe) e **Entregas** (cliente) não têm API: as abas explicam isso e abrem o módulo no site.
  Se preferir esconder essas abas até o backend publicar as rotas, é uma mudança de uma linha.

## 1. Clientes e vendas

| Recurso (site) | Perfil/permissão | API existente | Android hoje | Trabalho pendente |
|---|---|---|---|---|
| Carteira de clientes (`/clientes`) | leitura `membro` | só `GET /empresas` (nome, plano, status, módulos) | **Parcial.** Aba Clientes → Empresas: lista com status, plano e módulos (nomes reais) e abre a ficha | **Backend:** endpoint da carteira (saúde, prazos, desempenho) |
| Cliente 360 (`/clientes/{id}`) | leitura `membro` | parcial: `/equipe/termometro?empresaId`, `/leads?empresaId` | **Parcial.** Ficha da empresa: Termômetro e resultados, últimos leads, módulos autorizados; atalho "Cliente 360 completo" abre o site | **Backend:** projetos, entregas, prazos e histórico |
| Leads e vendas (`/leads-clientes`): lista | `membro` / cliente (RLS) | `GET /leads` (+`origem`, `entradas`) | **Completo no que a API permite.** Lista, filtro por etapa e por empresa, paginação, **origem real** ("legado" nunca aparece como canal) e "Entrou N×" | Busca por nome e filtro por período (a API aceita `desde`; falta UI) |
| Leads: etapa, notas, recebimentos | `escrita` | `PUT /equipe/jornada/{id}`, `POST …/notas`, `…/recebimentos` | **Implementado (equipe):** detalhe do lead com contato (ligar, WhatsApp, e-mail), jornada, edição de etapa com as regras do servidor, notas internas, recebimentos e histórico. Ações só com `podeEditar` e papel ≠ `leitor` | Validar com contas reais. **Sugestão ao backend:** `GET /equipe/jornada/{id}` devolver também o estado do lead (hoje o app o localiza pela listagem do dia) e o contato |
| Leads: tarefas do lead, status do lead capturado, vincular empresa | `escrita` | **Nenhuma** | Não existe | **Backend:** rotas de tarefas e de status/vínculo |
| Leads: atribuir origem | `admin` | **Nenhuma** (a jornada tem `atribuicao`, mas só leitura) | Não existe | **Backend** |
| Leads: ativar cliente (ativações pendentes) | `administrador` | **Nenhuma** | Não existe | **Backend** |
| Jornada do lead (`/jornada`) | leitura `membro`; grava `escrita` | `/equipe/jornada` (+ detalhe, PUT, notas, recebimentos), `/equipe/resultados` | **Parcial.** Detalhe e edição por lead (ver linha acima) | Tela de lista da jornada com filtros por canal, responsável, campanha e período (API ok); resultados avulsos já aparecem no Termômetro |
| Campanhas (criar/editar) | `escrita` | **Nenhuma** (doc: "só na web") | Não existe | **Backend** |
| Métodos e aplicações (`/metodos`) | leitura equipe; grava `escrita`; automações `admin` | **Nenhuma** (metas só aparecem dentro do Termômetro) | Metas lidas no Termômetro | **Backend** para listar/criar métodos e aplicações |
| Atendimento (`/atendimento`) | leitura `membro`; atualizar chamado `escrita` | **Nenhuma** | Não existe | **Backend** |
| Desempenho por cliente / Instagram / YouTube (`/plataformas`, `/instagram`, `/youtube`) | leitura `membro`; cliente via RLS (mídias) | **Nenhuma** | Não existe | **Backend** (métricas sociais) |
| Empresas e acessos (`/empresas`, `/convidar-cliente`) | leitura `membro`; convidar, editar, links de senha `administrador` | só leitura básica em `GET /empresas` | Lista de empresas e ficha (leitura). Convites e acessos: item "Na web" em Mais | **Backend** para convidar cliente, editar Instagram/YouTube, remover acesso |

## 2. Termômetro e visão geral

| Recurso | Perfil/permissão | API existente | Android hoje | Pendente |
|---|---|---|---|---|
| Termômetro do cliente (publicado) | cliente / qualquer perfil | `GET /termometro`, `GET /leads?desde=` | **Completo no que a API permite.** Avaliação publicada (escala, metas, resultados congelados) **ou**, sem avaliação ou em período livre (7/30/90 dias, mês, personalizado), indicadores calculados dos leads: leads, ganhos, conversão, vendido, tempo até a venda, funil, origem e gráficos de leads, vendas e valor vendido. Sem "recebido" (a API do cliente não entrega) | Cliques: o backend ainda não coleta (aviso honesto). Contrato proposto em `TERMOMETRO-E-CLIQUES.md` |
| Painel do Termômetro (equipe) | leitura `membro`; grava `escrita`; publica `admin`/`gestao` | `GET /equipe/termometro`, `POST/PUT …/avaliacoes`, `POST …/publicar` | **Completo.** Empresa, período (presets e personalizado), resultados ao vivo, metas, avaliações; criar/editar rascunho, corrigir publicada (novo rascunho) e publicar com confirmação | Validar com contas reais |
| Visão geral (`/`): central operacional (clientes, leads abertos, projetos, prioridades) | `membro` | **Nenhuma** | Item "Na web" em Mais (a aba Termômetro cobre a parte de avaliações) | **Backend** |
| Meu dia (`/meu-dia`) | pessoal | **Nenhuma** | Não existe | **Backend** |

## 3. Operação

| Recurso | Perfil/permissão | API existente | Android hoje | Pendente |
|---|---|---|---|---|
| Projetos e entregas (`/producao`) | leitura `membro`; criar/mover `escrita` | **Nenhuma** | Aba Operação lista o módulo com "Abrir no site" (sem funcionalidade nativa) | **Backend** |
| Agenda e prazos (`/agenda`) | `membro` | **Nenhuma** | Não existe | **Backend** |
| Aprovações de posts | `membro` | **Nenhuma** | Não existe | **Backend** |
| Entregas do cliente (`/cliente/entregas`: playbook de 30 dias) | leitura cliente (RLS); editar `escrita` | **Nenhuma** | Aba Entregas explica e abre o site (sem funcionalidade nativa) | **Backend** |

## 4. Inteligência

| Recurso | Perfil/permissão | API existente | Android hoje | Pendente |
|---|---|---|---|---|
| A.U.R.A. (`/aura`) | `membro` | **Nenhuma** | Não existe | **Backend** |
| Caixa de entrada (`/inbox`) | `membro` | **Nenhuma** | Não existe | **Backend** |
| Estratégia e narrativas (`/sni`) | leitura `membro`; grava `escrita` | **Nenhuma** | Não existe | **Backend** |
| Pré-análise do cliente (`/cliente/pre-analise`) | leitura cliente; criar `escrita` | **Nenhuma** | Não existe | **Backend** |

## 5. Conteúdo

| Recurso | Perfil/permissão | API existente | Android hoje | Pendente |
|---|---|---|---|---|
| Biblioteca (`/biblioteca`) | leitura equipe e cliente (só `visivel_cliente`); gravar `escrita` | **Nenhuma** | Não existe | **Backend** |
| Mídias e relatórios (cliente) | cliente (RLS) | **Nenhuma** | Não existe | **Backend** |

## 6. Gestão

| Recurso | Perfil/permissão | API existente | Android hoje | Pendente |
|---|---|---|---|---|
| Visão da gestão (`/gestao`) | `admin` | **Nenhuma** | Não existe | **Backend** |
| Financeiro (`/financeiro`) | `admin` (leitura e gravação) | **Nenhuma** | Não existe | **Backend** |
| Equipe (`/equipe`) | leitura `membro` | **Nenhuma** | Não existe | **Backend** |
| Sistemas e automações (`/automacoes`): webhooks, mapeamentos, eventos, incidentes | leitura `membro`; webhooks/mapeamentos `administrador`; incidentes `escrita` | **Nenhuma** | Não existe | **Backend** (não expor segredos) |
| API e chaves (`/api-chaves`) | `administrador` | **Nenhuma** | Não existe | **Backend** e decisão de segurança (chaves não devem aparecer no app) |

## 7. Configurações e conta

| Recurso | Perfil/permissão | API existente | Android hoje | Pendente |
|---|---|---|---|---|
| Perfil (nome, e-mail, papel, empresas) | próprio | `GET /me` | Leitura em "Perfil e configurações" (avatar e Mais) | Editar perfil: **backend** |
| Aparência (escuro/claro/sistema) | local | n/a | Funciona (padrão escuro) | n/a |
| Sair | próprio | Supabase Auth (`logout`) | Funciona | n/a |
| Recuperar senha | público | n/a (web) | Abre `/entrar` no navegador (login e Configurações) | n/a |
| Alterar senha, sair de todos os dispositivos | próprio | **Nenhuma** | Não existe | **Backend** (não contornar a checagem do site) |
| Alertas/notificações (preferências) | próprio | **Nenhuma** | Não existe | **Backend** |
| Equipe e permissões, convites, perfil da Synergy, auditoria | `administrador` | **Nenhuma** | Não existe | **Backend** |
| Configurações do cliente: empresa, equipe, captação, integrações, API, plano, segurança | cliente (RLS) | **Nenhuma** | Não existe | **Backend** |
| Termos de uso / Privacidade | público | n/a (páginas web) | Em Mais → Legal, abrem no navegador | n/a |

## Validação

O contrato exige a validação com **duas contas de teste** (equipe e cliente) seguindo o roteiro da seção 9.1 do
`API-APP.md`. O app já foi autenticado com sucesso numa conta real, mas **cada fluxo novo só é dado como
validado com dados reais depois que alguém o executa no aparelho com essas contas**. A demonstração em debug
(`DemoActivity`) valida a interface, não o servidor.
