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

## Verificado no celular
Início, Clientes/Leads, Mais, Configurações, detalhe do lead e teclado nos temas claro e escuro; fonte 1,3× e 2×
(Configurações e Início); preservar rolagem de "Mais" ao trocar de aba; preservar filtro ao voltar do detalhe;
botão Voltar. Movimento reduzido: a troca de aba usa fade de 120 ms, que respeita a escala de animações do sistema.

## Controles que não existem (de propósito)
Notificações, biometria e gerenciamento de sessões **não aparecem**: não estão implementados nem têm API.
"Sair deste aparelho" encerra só a sessão local; "sair de todos os dispositivos" depende do backend.

## O que ainda falta
- Auditoria completa de claro/escuro nas telas de carregamento/erro e nas folhas (sheets) de edição e de período.
- TalkBack: cabeçalhos marcados (`heading`) nas barras e grupos; falta um passe completo com o leitor ligado.
- Medição de desempenho (jank) com dados reais; só foi observada fluidez na demonstração.
- `imePadding` nas folhas de edição (o botão "Adicionar nota" fica sob o teclado até rolar).
- Canais de ajuda (contato) em "Ajuda e privacidade": só existem as páginas de termos e privacidade.
