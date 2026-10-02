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
