
# Synergy One — Android

App nativo Android (Kotlin + Jetpack Compose) do Synergy One. Atende equipe e clientes de acordo
com as permissões da conta, usando o backend existente do Synergy One.

## Status desta etapa

Esta etapa entrega **estrutura**, não o produto final:

- [x] Projeto Android configurado (Gradle Kotlin DSL + version catalog, Compose, Material3).
- [x] Tema visual (paleta/tipografia placeholder, suporte a cor dinâmica no Android 12+ e modo
      escuro) — ver `ui/theme/`.
- [x] Tela de login com campo de senha mostrar/ocultar — ver `ui/login/`.
- [x] Estrutura de navegação com destinos separados para equipe e cliente — ver `ui/navigation/`.
- [x] Contrato de autenticação (`AuthRepository`) desacoplado da UI, sem simular login bem-sucedido
      e sem endpoint inventado — ver `data/auth/` e `docs/API_CONTRACTS.md`.
- [x] Contrato real de autenticação levantado do código do backend (Supabase Auth + PostgREST com
      RLS) — ver `docs/API_CONTRACTS.md`.
- [x] Autenticação real (`SupabaseAuthRepository`): login, checagem de acesso em
      `synergy_one.usuarios`, empresas do cliente, refresh serializado, logout e restauração de
      sessão ao abrir o app — ver `data/auth/`, `data/remote/`.
- [x] Sessão cifrada com AES-GCM (chave no Android Keystore), fora de backups — ver `data/session/`.
- [x] Cliente da API dos apps (`/api/app/v1`) com Bearer, renovação de token em 401 e tradução dos
      erros do contrato — ver `data/api/`.
- [x] Telas com dados reais: Termômetro (cliente e equipe), Leads (filtro + paginação), Empresas e
      Conta (com sair). Design alinhado ao site (tokens, Sora/Manrope) — ver `ui/theme/`.
- [x] Login validado contra o Supabase real.
- [ ] **Pendente:** gravar avaliações do Termômetro (criar rascunho, publicar) e jornada do lead.
- [ ] **Pendente:** cliques (o backend ainda não coleta; o app mostra aviso, nunca zero inventado).
- [ ] **Pendente:** conteúdo real das áreas de equipe e cliente (hoje são telas placeholder).
- [ ] **Pendente:** identidade visual oficial (cores/logo atuais são placeholder).

## Configuração do backend

Em `local.properties` (não versionado) ou nas variáveis de ambiente `SUPABASE_URL` /
`SUPABASE_ANON_KEY` (CI):

```
supabase.url=https://<ref>.supabase.co
supabase.anonKey=<mesma VITE_SUPABASE_ANON_KEY da web>
api.baseUrl=https://www.synergyecosystem.com.br   # opcional (padrão); emulador + site local: http://10.0.2.2:5173
```

Sem esses valores (ou com URL que não seja `https://`), o app usa `PendingAuthRepository`, que
sempre retorna erro e nunca simula sucesso. **Nunca** colocar `service_role` ou segredos do n8n.

## Arquitetura (resumo)

```
app/src/main/java/br/com/synergyone/android/
├── SynergyOneApplication.kt      # ponto de entrada da DI manual (AppContainer)
├── MainActivity.kt
├── core/result/ApiResult.kt      # Success/Error genérico
├── data/auth/                    # AuthRepository (porta) + SupabaseAuthRepository (+ Pending se sem config)
├── data/remote/                  # SupabaseApi (OkHttp): Auth + PostgREST (schema synergy_one)
├── data/session/                 # SessionStore cifrado (Keystore)
├── di/AppContainer.kt            # DI manual, sem framework por enquanto
└── ui/
    ├── theme/                    # Color.kt, Type.kt, Theme.kt
    ├── components/PasswordField.kt
    ├── login/                    # LoginScreen, LoginViewModel, LoginUiState
    ├── navigation/                # NavRoutes, SynergyNavGraph
    ├── team/TeamHomeScreen.kt     # placeholder pós-login (perfil equipe)
    └── client/ClientHomeScreen.kt # placeholder pós-login (perfil cliente)
```

Sem framework de DI (Hilt) nem biblioteca de rede ainda — adicionados apenas quando houver
contrato de API confirmado, para não carregar o projeto com infraestrutura sem uso real.

## Requisitos para build local

- JDK 17+ (testado com Temurin 25).
- Android SDK com `compileSdk`/`platforms;android-37` e `build-tools;36.0.0` instalados
  (gerenciado pelo Android Studio).
- `local.properties` com `sdk.dir` apontando para o SDK local — **não é versionado** (está no
  `.gitignore`); cada máquina/desenvolvedor gera o seu.

## Pré-visualização sem login (debug)

A `DemoActivity` existe só no build de debug e mostra as telas reais com dados fictícios:

```bash
adb shell am start -n br.com.synergyone.android/.ui.demo.DemoActivity --es view team   # ou client
```

## Build e testes

```bash
./gradlew assembleDebug   # compila o APK de debug
./gradlew test            # testes unitários (inclui LoginViewModelTest)
```

## Segurança

- Nenhuma credencial, token ou URL de produção está hardcoded no código-fonte.
- `local.properties`, keystores (`*.jks`/`*.keystore`) estão no `.gitignore`.
- Quando a URL base da API real for definida, ela deve vir de configuração por ambiente
  (ex.: `BuildConfig` alimentado por `local.properties` ou variável de CI), nunca commitada.
