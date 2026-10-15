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

## O que ainda falta
- Validação com contas reais (itens acima) e conferência auditiva com TalkBack.
- Capturas faltantes da seção 2 e das folhas de avaliação/recebimento com teclado.
- Medição de desempenho em build release e com dados reais; investigar o p99 de 150 ms na primeira composição de cada aba.
- Canais de contato em "Ajuda e privacidade": só existem as páginas de termos e privacidade.
- 30 avisos de lint (0 erros) anteriores a esta etapa, não tratados aqui.
