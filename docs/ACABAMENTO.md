# Acabamento e experiência (revisão de UX/UI)

Capturas em `docs/capturas/` (celular real, moto g53, **dados de demonstração** da `DemoActivity` debug).
Nomes: `Dark_*`/`Light_*` = tema; `_f20_` = fonte 2×.

## Direção visual aplicada (Início e Configurações, depois estendida)
- **Cabeçalho compacto:** marca + título + avatar; sem o "Synergy One" repetido em cada tela.
- **Cartões planos:** raio de 16 dp, borda fina do tema, sem sombra (no claro a sombra formava um anel duplo).
- **Listas agrupadas** (Mais e Configurações): rótulo de grupo discreto + um cartão com linhas e divisores finos.
  Removidos os selos "No app"/"Site" repetidos: o ícone (seta ou "abrir fora") já diz o destino.
- **Chips de filtro** com o lilás do tema quando selecionados (no claro o cinza sutil quase não aparecia).
- **Marca:** o chip da logo usa a borda do tema (antes `Color.White` fixo, que só funcionava no escuro).

## Inventário de defeitos
| # | Defeito | Situação |
|---|---|---|
| 1 | Trocar de aba descartava rolagem, empresa, período e filtros | Corrigido: cada aba guarda o estado e não refaz a consulta |
| 2 | Não havia "seguir o sistema" | Corrigido (Escuro padrão, Claro, Sistema) |
| 3 | Configurações em cartões soltos, sem grupos, com "Acesso" redundante | Corrigido: Conta, Aparência, Segurança e acesso, Ajuda e privacidade, Sobre |
| 4 | "Esqueci minha senha" só mostrava um aviso | Corrigido: abre `/entrar` no navegador (contrato `MOBILE-AUTH.md` §7); também em Configurações |
| 5 | "Sair" sem confirmação | Corrigido: confirma e explica que outros aparelhos não são afetados |
| 6 | Cabeçalho repetia "Synergy One"; rótulo "Módulos" repetido em cada bloco | Corrigido |
| 7 | Cartões de lead altos e hierarquia invertida (origem maior que a mensagem) | Compactado |
| 8 | Seletor de tema cortava "Sistema" em 2× | Trocado por lista com marca de seleção, estável em qualquer escala |
| 9 | Valores longos (host do serviço) quebravam no meio da palavra em 2× | Descem para baixo do título |
| 10 | Marca com borda branca fixa | Token do tema |

## Segunda rodada (teclado, estados, acessibilidade, atualização)

Legenda: **[medido]** = número lido de ferramenta; **[visto]** = captura de tela conferida por mim no celular;
**[inferido]** = conclusão a partir de outra evidência; **[não validado]** = ainda não feito.
Tudo abaixo foi no moto g53, build **debug**, com **dados de demonstração** (nada contra o servidor real).

### 1. Teclado
- **[visto]** Detalhe do lead: o bloco "Nova nota" (campo + contador + **Adicionar nota**) sobe e fica inteiro acima do teclado.
  Causa do defeito: o pedido de rolagem acontecia no foco, antes do teclado encolher a área visível; agora refaz o pedido
  enquanto a altura do teclado muda e a cada letra.
- **[visto]** Folha "Atualizar jornada": campo numérico em foco com **Salvar** alcançável rolando a folha. Todas as folhas com
  texto (jornada, recebimento, avaliação) receberam margem do teclado e rolagem.
- **[não validado]** Folha de avaliação (Termômetro) e de recebimento com teclado: mesma correção aplicada, sem captura.

### 2. Estados, folhas e diálogos (claro/escuro, 1× e 2×)
- **[visto]** Carregamento, erro e vazio (Início da equipe, `--es state slow|error|empty`), folha de período, diálogo "Sair
  deste aparelho" e seletor de empresa, no claro e no escuro; erro, folha e diálogo também em 2× (claro).
- Corrigido: esqueleto de carregamento mais estreito que o conteúdo (recuo duplo) e quase invisível no claro; título do erro
  desalinhado do resto; seletor mostrando "Sem empresa" enquanto ainda carrega (agora "Carregando…"); "Ainda não avaliado"
  repetido três vezes na mesma tela.
- **[não validado]** Combinações não capturadas: erro/vazio no escuro em 2×; folha de período no claro em 1×.

### 3. TalkBack
- **[não validado de ouvido]** Liguei o TalkBack real do aparelho, mas o adb não entrega o texto falado; não foi possível
  conferir o áudio. O TalkBack foi desligado em seguida (configuração original restaurada).
- **[visto]** Em lugar disso, inspecionei a árvore de acessibilidade (a mesma que o TalkBack lê) com `uiautomator`. Achados e
  correções: cada indicador era 3 paradas ("Captado", "120", "• 100%") → agora 1 ("Captado: 120, 100% do total"), o mesmo
  para métricas, origem e linhas de apoio; eixos do gráfico não são mais lidos soltos (o gráfico tem descrição própria, agora
  com máximo e último valor); marca decorativa não é mais anunciada em cada tela; avatar lia "Conta de…" e "CL";
  opções de aparência agora anunciam "selecionado" (papel de rádio) em vez de um ícone solto; linhas informativas
  das Configurações leem "Papel, Gestão" como um item; títulos de tela e de seção marcados como cabeçalho; carregamento
  e aviso de falha na atualização anunciados (região "ao vivo").
- **[não validado]** Ordem de leitura e foco ao abrir/fechar diálogos e folhas com o TalkBack falando: **precisa de você
  ouvindo** (roteiro abaixo).

### 4. Animações desativadas
- **[visto]** Com a escala de animações do sistema em 10×, a troca de aba capturada a ~450 ms estava no meio do fade (as duas
  telas sobrepostas). Com escala 0 a mesma captura já mostrava a aba final. **[inferido]** que o fade acompanha a
  configuração do sistema (tween escalado); o instante exato em escala 0 não foi cronometrado.
- Observação: o indicador da barra de navegação e o esqueleto pulsante não foram medidos em escala 0.

### 5. Dados antigos × atualização
- **[visto]** Puxar para atualizar no Termômetro da equipe: o indicador aparece e os dados continuam na tela.
- **Atualização manual:** puxar para atualizar em Termômetro (equipe e cliente), Empresas, Leads, ficha da empresa e detalhe do
  lead. A atualização é em segundo plano: o dado atual fica na tela; se falhar, ele permanece e aparece "Não foi possível
  atualizar… Tentar".
- **Após alterações:** qualquer gravação bem-sucedida na API (jornada, nota, recebimento, avaliação, publicação) marca como
  velho tudo o que foi lido antes (`DataFreshness`); ao reabrir a tela/aba, só o que ficou velho é atualizado.
- **Sem consulta desnecessária:** o dado também fica velho após **5 minutos** (TTL) ao reabrir a tela/aba ou voltar ao app;
  trocar de aba dentro de 5 min sem gravação **não** consulta.
- **Sair da conta / sessão expirada:** os dados em memória são descartados (`clearSessionData`: cache de leads e versão);
  as telas e seus carregadores morrem junto com a pilha de navegação (que é esvaziada no logout).
- **[testado, unitário]** `DataFreshnessTest` e `LoaderViewModelTest`: TTL, invalidação por gravação, manter dado durante a
  atualização, falha mantendo o dado, sessão inválida não escondida, gravação com falha não invalida, descarte de cache.
- **[não validado]** O fluxo ponta a ponta (editar um lead e ver a lista/Termômetro atualizarem) só roda com a API real: a
  demonstração grava em memória, fora do cliente HTTP que dispara a invalidação.

### 6. Desempenho: medido × impressão
- **[medido]** Partida a frio (`am start -W`, 5 execuções, build debug): 1307–1352 ms (mediana 1326 ms).
- **[medido]** Troca de abas na demonstração (`dumpsys gfxinfo`, 32 trocas): 1324 quadros, 34 lentos (2,57%), p50 9 ms,
  p90 29 ms, p95 40 ms, p99 150 ms. Os quadros lentos coincidem com a primeira composição de cada aba. Build debug costuma ser
  mais lento que release; **não é um número de produção**.
- **[impressão visual]** A troca de aba e a rolagem pareceram fluidas; sem medição de release.
- **[não validado]** Carregamento com dados reais. Para medir com as suas contas (build debug), com o celular no cabo:
  `adb logcat -s SynergyPerf` mostra, por requisição, `MÉTODO /rota -> status tempo tamanho` (ids trocados por `N`; nunca
  token, query ou corpo) e, por tela, `carga <chave> tempo ok|erro`.

## Roteiro para validar com contas reais (somente leitura)
1. Entrar com a conta de **cliente** e depois de **equipe** (build debug). Não editar leads nem publicar avaliações.
2. Em cada aba: abrir, rolar, trocar de aba e voltar (deve voltar na mesma posição, sem esqueleto).
3. Puxar para atualizar em Termômetro, Leads e Empresas; conferir o aviso de falha com o modo avião ligado.
4. Anotar os tempos do `logcat -s SynergyPerf` (primeira carga × troca de aba × puxar para atualizar).
5. TalkBack ligado: cabeçalho → abas ("Termômetro, guia, selecionada") → filtros (selecionado/não) → gráfico (descrição) →
   folha "Período" e diálogo "Sair" (o foco deve ir para o título ao abrir e voltar ao botão que abriu ao fechar).

## Terceira rodada (verificação após reescrita do histórico)

**Medido** no commit `106c30f` (histórico reescrito só para tirar a linha de coautoria; o conteúdo dos arquivos não mudou):
- 87 testes unitários, 0 falhas; `assembleDebug` ok; lint com 0 erros.
- Lint caiu de 30 para 12 avisos. Corrigidos 18: 10 recursos sem uso (cores e textos de telas antigas) e 8 de estilo KTX
  (`toUri()`, `prefs.edit {}`), sem mudança de comportamento.
- Avisos restantes, deliberadamente não tratados:
  - 7 de versão de dependência/Gradle (`NewerVersionAvailable`, `GradleDependency`, `AndroidGradlePluginVersion`): atualizar
    exige validar contra o app inteiro; fora desta etapa.
  - 1 `OldTargetApi`: subir o `targetSdk` muda comportamento do sistema; é decisão separada.
  - 3 `ModifierParameter` (`DateField`, `MoneyField`, `ScreenHeader`): só convenção de ordem de parâmetros.

**Validado no aparelho (moto g53 5G, conta real de cliente, empresa ForLife, só leitura, build debug):**
- [visto] Restauração da sessão: o app abriu direto no Termômetro, sem pedir login, e carregou empresas, termômetro e leads.
- [medido, logcat `SynergyPerf`] `GET /termometro` devolveu 200 com 65 B (`atual: null`) para a empresa sem avaliação: o app
  mostra "Ainda sem avaliação publicada" e os indicadores dos leads, não erro.
- [medido] Primeira carga: `/empresas` 628 ms, `/termometro` 692 ms, `/leads` 570–575 ms; telas prontas entre 620 e 770 ms.
- [medido] Puxar para atualizar: 802–952 ms por tela, com os dados antigos na tela durante a atualização.
- [visto] Ir de Termômetro a Leads e voltar: a aba volta igual e não faz nova requisição (cache dentro dos 5 min).
- [visto] Sem rede (Wi-Fi e dados desligados, religados no fim): puxar para atualizar mantém os números e mostra
  "Não foi possível atualizar. Mostrando o que já estava carregado." com "Tentar"; as 3 cargas falharam em 49–101 ms.
- [visto] Coerência interna: a lista tem 7 leads; no período 17/09–16/10 o Termômetro mostra 5 captados, e a lista tem
  exatamente 5 com data nesse intervalo (os outros 2 são de agosto ou anteriores). **Comparar com o site continua a cargo do
  responsável** (mesmo período, fuso America/Sao_Paulo).
- [visto, demonstração] Capturas novas: erro e "sem avaliação" no escuro com fonte 2× (`docs/capturas/erro-escuro-2x.png`,
  `sem-avaliacao-escuro-2x.png`) e folha "Nova avaliação" com o teclado aberto (`avaliacao-teclado-escuro.png`): o campo
  focado, "Nota interna", fica visível acima do teclado. Com 2× os rótulos da barra inferior somem e ficam só os ícones.

**Não validado nesta rodada:**
- Conta de **equipe** real, troca de empresa (a conta de cliente testada só tem uma), troca de período em dados reais,
  logout e entrada com outra conta sem restos da anterior: precisam das credenciais do responsável.
- TalkBack de ouvido; folha de recebimento com teclado (precisa de um lead ganho de teste).
- Conta de **equipe**, troca de empresa e de período com dados de equipe: o responsável executou o roteiro e **informou que está tudo certo** [declarado, sem evidência minha]. O `logcat` gravado nesse intervalo só tem rotas de cliente (`/me`, `/empresas`, `/termometro`, `/leads`) e nenhuma `/equipe/*`, então não confirma que a conta de equipe foi usada; repetir com o log aberto se for preciso evidência.
- Logout [medido]: depois de sair, o armazenamento do app ficou só com o tema (`Dark`) e a sessão vazia, sem token nem dados da conta anterior.
- Comparação dos números com o site no mesmo período: a cargo do responsável, ainda sem resultado registrado.

## Desempenho: debug × build otimizado [medido]
Perfil `medicao` (`./gradlew assembleMedicao`): R8 + redução de recursos, `isDebuggable = false`, assinado com a **chave de
debug** (a assinatura de publicação não existe no projeto e não foi tocada). Mesmo `applicationId` do debug, então instala por
cima e mantém a sessão. **Nunca publicar este tipo.** O `release` continua sem R8 e sem alteração. `PERF_LOG` (log
`SynergyPerf`, sem token/corpo/query) fica ligado em `debug` e `medicao` e **desligado no `release`**. Regras de serialização
em `app/proguard-rules.pro`; com R8 o app restaurou a sessão e leu a API normalmente.

Mesmo aparelho (moto g53 5G), mesma sessão real de cliente, só leitura, mesmo roteiro nos dois:

| | debug | medicao (R8) |
|---|---|---|
| APK | 21,1 MB | 2,3 MB |
| Partida a frio até o primeiro quadro (`am start -W`, 5 medições) | 1293–1367 ms (mediana 1315) | 243–292 ms (mediana 257) |
| Troca de abas (72 trocas, ~3,5 mil quadros): quadros lentos | 4,47% | 1,08% |
| p50 / p90 / p95 / p99 do quadro | 10 / 15 / 22 / 61 ms | 10 / 13 / 14 / 31 ms |
| Dados prontos depois da partida (`logcat`) | — | termômetro 680 ms, leads 632–646 ms |

Leitura: o ganho é do R8/não-debuggável, não de mudança de código. A partida mede até o primeiro quadro; o conteúdo real
chega ~0,6 s depois porque depende da rede. Medição de um aparelho só, sem repetição em outros; as medições antigas da
demonstração (1307–1352 ms) são do debug com dados fictícios e não são comparáveis linha a linha.

## Validação com a sessão real do cliente (16/10/2026, backend `bc65361`, migração 035 aplicada) [medido/visto]
Conta de cliente real (empresa ForLife), build debug, **somente leitura**, aparelho moto g53 5G.
- `GET /indicadores` → **200**, 5 682 B, 1,5 s na atualização; `GET /termometro` → 200 (531 B, já com o bloco `indicadores` do servidor);
  `GET /biblioteca` → **200** (387 B com 1 material; 43 B com o filtro de uma área vazia). Resposta 401 sem token **não** foi tratada como validação.
- Indicadores 17/09–16/10 (servidor, calculados às 13:52 de Brasília): captados **5**, ganhos **0**, conversão **0%** (0 ÷ 5; não é nulo
  porque há leads), novos 5, vendas fechadas **0**, vendido **R$ 0,00**, recebido **R$ 0,00**, origem "Origem não identificada" 5,
  leads **+400%** sobre o período anterior (1 lead). Vendido e recebido mostram "sem período anterior para comparar", não 0%.
- Coerência com outra fonte do próprio backend: a lista `GET /leads` tem 5 leads com captura nesse intervalo e 1 no período anterior
  (18/08), igual a captados 5 e +400%.
- Acessos e cliques: o servidor devolveu "Nenhum site configurado para esta empresa" → o app mostra **"ainda sem dados"**, não zero
  (coleta não configurada ≠ zero ≠ erro).
- Biblioteca: 1 material ("O Coração Como Estratégia", área Criação, tipo E-book); filtro **Comercial** → vazio legítimo ("Nenhum material
  nesta área"), **Criação** → 1; o toque abriu o link HTTPS no visualizador do Drive. Paginação **não** exercitada com dado real (só 1
  material); coberta por teste unitário.
- **Defeito encontrado e corrigido:** as telas fora das abas (Biblioteca, perfil, ficha, leads da empresa, detalhe do lead) ficavam sob a
  barra de status e com título em cor escura (ilegível no tema escuro). Agora usam um invólucro com fundo, cor do texto e recuos
  (`Standalone` em `SynergyNavGraph`). Conferido na Biblioteca; as outras usam o mesmo invólucro, mas não foram reabertas uma a uma.
- Texto do estado vazio com filtro de área ajustado ("Não há material liberado nesta área…").

**Comparação com o site:** o app e o `GET /leads` concordam, mas a comparação com o **portal web** no mesmo intervalo continua a cargo do
responsável (não tenho login no site). Valores a conferir lá: captados 5, ganhos 0, conversão 0%, vendas fechadas 0, vendido R$ 0,00,
recebido R$ 0,00, para 17/09–16/10/2026 (horário de Brasília).

**Pendente:** troca para a conta de equipe, troca de empresa e retorno ao cliente sem restos da sessão anterior; o log da tentativa
anterior não mostrou rotas `/equipe`. Detalhe do lead de cliente e paginação da Biblioteca com mais de um material.

## Decisões do responsável (19/10/2026)
- Identificador do app: **`br.com.synergyone.android`** (o atual; não muda).
- Testes de aceite feitos pelo responsável no aparelho, que informou estar tudo certo [declarado].
- PR #1 integrado à `main` no GitHub. **Publicação na loja: não feita**; depende de decisão com a chefia e de chave de assinatura de
  publicação, que não existe no projeto. O perfil `medicao` (chave de debug) nunca deve ser publicado.

## Dependência do backend: indicadores
A API central de indicadores está em revisão no backend. O app **não amplia os cálculos locais** a partir de `GET /leads`
(teto de 1.000 registros, funil pela etapa atual). A integração será feita depois do contrato final e da disponibilização
do endpoint; até lá, a Home do cliente continua com o comportamento descrito acima, inclusive o aviso quando passa do teto.

## O que ainda falta
- Validação com contas reais (itens acima) e conferência auditiva com TalkBack.
- Capturas faltantes da seção 2 e das folhas de avaliação/recebimento com teclado.
- Medição em outros aparelhos; o p99 caiu de 61 ms (debug) para 31 ms no build otimizado, sem investigação adicional.
- Canais de contato em "Ajuda e privacidade": só existem as páginas de termos e privacidade.
- 12 avisos de lint restantes (ver terceira rodada).
- Integração dos indicadores com o endpoint do backend, quando o contrato final existir.
