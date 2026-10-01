
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
- [ ] **Pendente:** implementação real de rede (Retrofit/OkHttp) assim que o contrato acima for
      validado.
- [ ] **Pendente:** armazenamento seguro de token/sessão.
- [ ] **Pendente:** conteúdo real das áreas de equipe e cliente (hoje são telas placeholder).
- [ ] **Pendente:** identidade visual oficial (cores/logo atuais são placeholder).

## Por que o login não "funciona"

De propósito. `LoginViewModel` está ligado a `PendingAuthRepository`, que sempre retorna um erro
explicando que a integração com o backend ainda não foi configurada — nunca simula sucesso. Isso
evita que naveguemos para as áreas de equipe/cliente com dados falsos antes de o contrato de API
ser validado com o time de backend (ver `docs/API_CONTRACTS.md`).

## Arquitetura (resumo)

```
app/src/main/java/br/com/synergyone/android/
├── SynergyOneApplication.kt      # ponto de entrada da DI manual (AppContainer)
├── MainActivity.kt
├── core/result/ApiResult.kt      # Success/Error genérico
├── data/auth/                    # AuthRepository (porta) + PendingAuthRepository (implementação atual)
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
