# Contrato de autenticação — Synergy One Android

> **Fonte:** código real de `dinotusk/synergy-one`, commit `b2ca4f8` (merge do PR #28, 2026-09-30).
> Arquivos analisados: `src/services/auth.functions.ts`, `src/lib/supabase/{sessao,usuario,server,empresa-ativa}.ts`,
> `src/routes/__root.tsx`, `supabase/migrations/20260920000000_synergy_one_base.sql` e migrações seguintes
> que alteram `synergy_one.usuarios`.
>
> Este documento substitui a proposta anterior (`POST /api/v1/auth/login`, `GET /api/v1/me`, etc.).
> **Esses endpoints não existem no backend** e não devem ser implementados no app.

---

## 1. Como a autenticação funciona hoje (web)

| Peça | Realidade no código |
|---|---|
| Provedor de identidade | **Supabase Auth** (e-mail + senha). O login é compartilhado com outros sistemas do ecossistema; ter conta no Supabase **não** dá acesso ao Synergy One. |
| Liberação no Synergy One | Linha em `synergy_one.usuarios` com `ativo = true`. Sem ela → "Esta conta não tem acesso ao Synergy One." |
| Sessão na web | Server functions do TanStack Start (`entrar`, `obterSessao`, `sair`) guardam a sessão Supabase em cookie httpOnly `sb-synergy-one-auth`. |
| Leitura de dados | PostgREST com o JWT da pessoa, schema `synergy_one`, filtrado por **RLS**. |
| Escrita de dados | **Só** server functions com `service_role`, depois de `exigirEquipe` / `exigirAdministrador`. Não existe nenhuma policy de INSERT/UPDATE/DELETE para `authenticated`. |
| API HTTP pública | Só `GET /api/v1/leads`, autenticada por **chave de empresa** (`syn_live_…`), não por usuário. Não serve para o app. |

**Consequência para o Android:** as server functions (`/_serverFn/...`) são RPC interno do framework,
baseadas em cookie, sem contrato estável — **não chamar**. O caminho que já existe e é seguro para um
cliente nativo é falar **direto com o Supabase Auth e com o PostgREST**, usando a chave pública
(anon/publishable) + JWT do usuário. Isso reproduz exatamente o que a web faz em `montarUsuario()`,
sob as mesmas regras de RLS.

---

## 2. Configuração do app

| Valor | Origem | Observação |
|---|---|---|
| `SUPABASE_URL` | mesmo valor de `VITE_SUPABASE_URL` da Vercel | `https://<ref>.supabase.co` |
| `SUPABASE_ANON_KEY` | mesmo valor de `VITE_SUPABASE_ANON_KEY` | Pública por design (já vai para o navegador). Toda proteção é RLS. |

- Injetar via `BuildConfig` a partir de `local.properties` / variáveis de CI. **Não commitar** valores.
- **Proibido no app:** `SUPABASE_SERVICE_ROLE_KEY`, `INGESTAO_N8N_SECRET`, `WEBHOOK_*`,
  `RATE_LIMIT_HASH_SALT`, chaves `syn_live_*`, string de conexão Postgres. Nada disso pode existir no
  APK, em `BuildConfig`, em recursos ou em logs.
- Não existe modo demonstração no app. (`AUTH_ATIVA=false` é recurso só da web.)

Cabeçalhos comuns:

```
apikey: <SUPABASE_ANON_KEY>
Content-Type: application/json
Authorization: Bearer <access_token>        # nas chamadas autenticadas
```

---

## 3. Login

### 3.1 Autenticar no Supabase Auth — **existe**

```
POST {SUPABASE_URL}/auth/v1/token?grant_type=password
apikey: <SUPABASE_ANON_KEY>

{ "email": "pessoa@empresa.com", "password": "..." }
```

Validação local antes de enviar (igual a `entrar` na web):
- `email`: `trim()`, `lowercase()`, formato de e-mail, até 254 caracteres.
- `password`: 1 a 200 caracteres.

`200 OK` (campos usados pelo app):

```json
{
  "access_token": "<JWT>",
  "token_type": "bearer",
  "expires_in": 3600,
  "expires_at": 1790000000,
  "refresh_token": "<opaco>",
  "user": { "id": "<uuid>", "email": "pessoa@empresa.com" }
}
```

Erros: `400` com `error_code` (ex.: `invalid_credentials`, `email_not_confirmed`), `429`
(`over_request_rate_limit`). **Mapeamento obrigatório:** qualquer 400/401/422 → mensagem única
**"E-mail ou senha incorretos."** (a web não revela se o e-mail existe). `429` → "Muitas tentativas.
Tente novamente em instantes." Falha de rede/5xx → mensagem genérica de conexão.

### 3.2 Conferir acesso ao Synergy One — **existe** (mesma consulta de `montarUsuario`)

Sem esta etapa o login **não está completo**. Logo após 3.1:

```
GET {SUPABASE_URL}/rest/v1/usuarios?select=nome,email,tipo,papel,ativo&user_id=eq.<user.id>
apikey: <SUPABASE_ANON_KEY>
Authorization: Bearer <access_token>
Accept-Profile: synergy_one
```

Resposta: array com 0 ou 1 item.

```json
[{ "nome": "Maria", "email": "maria@x.com", "tipo": "cliente", "papel": "cliente", "ativo": true }]
```

- **Sempre filtrar por `user_id=eq.<id>`**: a RLS deixa a equipe ler todos os usuários.
- Array vazio **ou** `ativo = false` → chamar logout (seção 5) e exibir
  **"Esta conta não tem acesso ao Synergy One."** Não persistir tokens.
- Selecionar só essas 5 colunas. Colunas como `cargo`, `telefone`, `notif_*`, `capacidade_semanal_horas`
  vêm de migrações marcadas como rascunho; podem não existir em todos os ambientes.

### 3.3 Empresas do cliente — **existe**

Somente se `tipo == "cliente"`:

```
GET {SUPABASE_URL}/rest/v1/usuario_empresa?select=empresa_id&user_id=eq.<user.id>
(mesmos cabeçalhos, Accept-Profile: synergy_one)
```

→ `[{ "empresa_id": 3 }, ...]`. Para a equipe, `empresas = []` (a equipe vê todas).

Dados das empresas visíveis (cliente: as dele; equipe: todas — a RLS decide):

```
GET {SUPABASE_URL}/rest/v1/empresa_crm?select=id,nome,slug,plano,status,modulos&order=nome
```

`modulos` é um objeto JSON (`{"leads":true,"preAnalise":true,"midias":true,"concorrentes":false,"api":false}`)
que diz quais módulos o portal do cliente mostra para aquela empresa.

---

## 4. Modelo de usuário e permissões

Equivalente exato de `UsuarioSessao` (`src/lib/supabase/usuario.ts`):

```kotlin
data class SessionUser(
    val id: String,            // auth user id (uuid)
    val nome: String,
    val email: String,
    val tipo: Tipo,            // "equipe" | "cliente"
    val papel: Papel,          // "admin" | "gestao" | "operacao" | "leitor" | "cliente"
    val empresas: List<Long>,  // só cliente; vazio para equipe
)
```

Restrições do banco: `tipo = "cliente"` ⇔ `papel = "cliente"`; equipe nunca tem papel `cliente`.
Valor desconhecido em `tipo`/`papel` → tratar como **sem acesso** (não cair em padrão permissivo).

Mapeamento para o app atual: `tipo == "equipe"` → `UserRole.TEAM` → área de equipe;
`tipo == "cliente"` → `UserRole.CLIENT` → área de cliente (na web: `/` vs `/cliente`). Uma conta tem
**um único** tipo; não existe conta com acesso às duas áreas.

Capacidades (regras reais do servidor — use apenas para **esconder/mostrar UI**; a autorização
verdadeira continua no backend/RLS):

| Capacidade | Regra (função no backend) |
|---|---|
| Ver área de equipe | `tipo == equipe` (`exigirMembroEquipe`) |
| Gravar (equipe) | equipe e `papel != leitor` (`exigirEquipe("escrita")`) |
| Ações "admin" (atribuir origens, gerir acessos) | `papel in (admin, gestao)` (`exigirEquipe("admin")`) |
| Configuração sensível / gestão de acessos | `papel == admin` (`exigirAdministrador`) |
| Ver área de cliente | `tipo == cliente`, restrito às `empresas` e aos `modulos` de cada empresa |

> A matriz por módulo em `src/data/equipe-config.ts` (`permissoesIniciais`) é **dado estático de tela**,
> não é aplicada em lugar nenhum. Não usar como fonte de permissão.

---

## 5. Renovação de sessão — **existe**

```
POST {SUPABASE_URL}/auth/v1/token?grant_type=refresh_token
apikey: <SUPABASE_ANON_KEY>

{ "refresh_token": "<refresh_token atual>" }
```

Resposta igual à do login (novo `access_token` **e novo `refresh_token`**).

Regras:
- Renovar quando faltar ~60 s para `expires_at`, ou ao receber `401` / `PGRST301` (JWT expirado) do
  PostgREST — uma única tentativa, depois refazer a chamada original.
- **Sempre substituir** o refresh token armazenado pelo novo. Serializar a renovação (mutex): duas
  renovações em paralelo com o mesmo token podem invalidar a sessão.
- `400` com `refresh_token_not_found` / `refresh_token_already_used` / `session_not_found` →
  sessão encerrada: limpar armazenamento e voltar ao login.
- **Revalidar acesso:** ao abrir o app e após cada renovação, refazer 3.2. Se a pessoa foi desativada
  (`ativo = false`) ou removida, encerrar a sessão. (O JWT continua válido até expirar; a RLS já
  bloqueia os dados porque `eh_equipe()`/`empresas_do_usuario()` checam `ativo`.)

---

## 6. Logout — **existe**

```
POST {SUPABASE_URL}/auth/v1/logout?scope=local
apikey: <SUPABASE_ANON_KEY>
Authorization: Bearer <access_token>
```

→ `204`. Em seguida (ou mesmo se a chamada falhar por rede/token expirado), apagar tokens e dados de
usuário do armazenamento local e voltar ao login.

- `scope=local` encerra só a sessão do aparelho. **Atenção:** a web chama `signOut()` sem escopo
  (supabase-js 2.x ⇒ `global`), então **sair na web revoga também a sessão do app**. O app precisa
  lidar com isso pela regra de refresh da seção 5.

---

## 7. Consulta do usuário atual — **existe** (composição, não endpoint único)

Não há `GET /me`. "Usuário atual" = `GET {SUPABASE_URL}/auth/v1/user` (opcional, valida o token)
+ consultas 3.2 e 3.3. Isso é o mesmo que `obterSessao()` faz na web.

---

## 8. Armazenamento no aparelho

- Guardar `access_token`, `refresh_token`, `expires_at` cifrados (Android Keystore — ex.: DataStore +
  Tink, ou EncryptedSharedPreferences).
- Nunca logar tokens, senha ou corpo das respostas de auth. Desativar logging de corpo HTTP em release.
- `android:allowBackup` / regras de backup devem excluir o armazenamento de sessão.

---

## 9. Situação: o que existe × o que falta

### Já existe no backend (nada a criar para login básico)
- Login, refresh, logout e leitura de usuário via Supabase Auth.
- Tabelas `synergy_one.usuarios`, `usuario_empresa`, `empresa_crm` legíveis pelo próprio usuário via RLS;
  schema `synergy_one` acessível pelo PostgREST (a web já usa `.schema("synergy_one")` com o JWT da pessoa).
- Mensagens de erro e regras de permissão descritas acima.

### Precisa ser implementado no app (Codex Android)
1. `SupabaseAuthRepository` implementando `AuthRepository` (seções 3, 5, 6, 7), substituindo
   `PendingAuthRepository` em `AppContainer`. Estender a interface com `refresh()`, `logout()`,
   `currentUser()`.
2. `AuthenticatedUser` passa a carregar `email`, `papel`, `empresas` (seção 4); `UserRole` é derivado de `tipo`.
3. Cliente HTTP (OkHttp/Retrofit ou `supabase-kt`) com interceptor de `apikey`/`Authorization`,
   `Authenticator` para refresh serializado e `Accept-Profile: synergy_one` nas chamadas REST.
4. Armazenamento seguro (seção 8) e restauração de sessão ao abrir o app (refresh + 3.2).
5. `SUPABASE_URL` / `SUPABASE_ANON_KEY` por ambiente via `BuildConfig`, fora do git.

### Pendências/limitações do backend (decisão do time, não bloqueiam o login)
1. **Recuperação de senha:** o fluxo real é server function (`solicitarRedefinicao`) que só envia
   e-mail para contas ativas e redireciona para `{APP_URL}/redefinir-senha` (web). Não há endpoint
   público para o app. Até existir, o botão "Esqueci minha senha" deve **abrir a página web `/entrar`**
   (Custom Tab). Não chamar `/auth/v1/recover` direto do app: pularia a checagem de conta ativa.
2. **Gravações a partir do app:** toda escrita hoje é server function com cookie. Para o app gravar
   qualquer coisa, o backend precisa expor rotas HTTP (`/api/...`) que aceitem
   `Authorization: Bearer <JWT do usuário>`, validem com `supabase.auth.getUser(jwt)` e reapliquem
   `exigirEquipe`/`exigirAdministrador`. Nada disso existe ainda.
3. **Opcional:** uma função SQL `synergy_one.meu_acesso()` (security definer, só `auth.uid()`)
   retornando usuário + empresas em uma chamada reduziria 3.2/3.3 para um único `rpc`. Não existe.
4. **Confirmar no painel do Supabase** (não está no repositório): duração do JWT, rotação/reuso de
   refresh token, rate limits de auth, exigência de confirmação de e-mail e se há MFA. O código atual
   não trata MFA.
5. **Confirmar quais migrações "RASCUNHO" foram aplicadas** em produção antes de o app ler colunas
   além das listadas em 3.2.

---

## Identificador do aplicativo — decisão pendente

O código atual usa `br.com.synergyone.android` como `namespace`, `applicationId` e pacote Kotlin
raiz. O identificador inicialmente combinado é `br.com.synergyecosys.one`.

Esta divergência está registrada e **não foi renomeada nesta revisão**. A alteração deve ser
aprovada antes de uma etapa própria, pois afeta o identificador do APK, o pacote de todas as
classes Kotlin, o Manifest, testes, configurações de serviços externos e futuras publicações na
Play Store.
