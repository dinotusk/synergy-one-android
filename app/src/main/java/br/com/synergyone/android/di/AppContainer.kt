package br.com.synergyone.android.di

import android.content.Context
import br.com.synergyone.android.BuildConfig
import br.com.synergyone.android.data.auth.AuthRepository
import br.com.synergyone.android.data.auth.PendingAuthRepository
import br.com.synergyone.android.data.auth.SupabaseAuthRepository
import br.com.synergyone.android.data.api.AppApi
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.OkHttpAppApi
import br.com.synergyone.android.data.empresa.ApiEmpresaRepository
import br.com.synergyone.android.data.freshness.DataFreshness
import br.com.synergyone.android.data.empresa.EmpresaRepository
import br.com.synergyone.android.data.jornada.ApiJornadaRepository
import br.com.synergyone.android.data.jornada.JornadaRepository
import br.com.synergyone.android.data.lead.ApiLeadRepository
import br.com.synergyone.android.data.lead.LeadRepository
import br.com.synergyone.android.data.remote.OkHttpSupabaseApi
import br.com.synergyone.android.data.session.KeystoreSessionStore
import br.com.synergyone.android.data.settings.ThemeSettings
import br.com.synergyone.android.data.termometro.ApiTermometroRepository
import br.com.synergyone.android.data.termometro.TermometroRepository

/** Pequeno contêiner manual de dependências (sem framework de DI por enquanto). */
interface AppContainer {
    val authRepository: AuthRepository
    val empresaRepository: EmpresaRepository
    val termometroRepository: TermometroRepository
    val leadRepository: LeadRepository
    val jornadaRepository: JornadaRepository
    val themeSettings: ThemeSettings

    /** Marca dados lidos como velhos após gravações e decide quando atualizar ao reabrir telas. */
    val freshness: DataFreshness

    /** Logout, sessão expirada ou troca de conta: descarta tudo o que pertencia à sessão anterior. */
    fun clearSessionData()
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    // Só HTTPS: o app nunca deve mandar credenciais em texto claro.
    private val configured = supabaseUrl.startsWith("https://") && anonKey.isNotBlank()

    private val appApi: AppApi by lazy { OkHttpAppApi(BuildConfig.API_BASE_URL) }

    override val authRepository: AuthRepository by lazy {
        if (configured) {
            SupabaseAuthRepository(
                api = OkHttpSupabaseApi(baseUrl = supabaseUrl, anonKey = anonKey),
                appApi = appApi,
                store = KeystoreSessionStore(context),
            )
        } else {
            PendingAuthRepository()
        }
    }

    // Sem sessão (build sem configuração) o cliente devolve "sessão expirada": nunca inventa dados.
    override val freshness = DataFreshness()
    private val client: AppApiClient by lazy { AppApiClient(appApi, authRepository, onMutation = freshness::invalidate) }

    override val empresaRepository: EmpresaRepository by lazy { ApiEmpresaRepository(client) }
    override val termometroRepository: TermometroRepository by lazy { ApiTermometroRepository(client) }
    override val leadRepository: LeadRepository by lazy { ApiLeadRepository(client) }
    override val jornadaRepository: JornadaRepository by lazy { ApiJornadaRepository(client) }
    override val themeSettings: ThemeSettings by lazy { ThemeSettings(context) }

    override fun clearSessionData() {
        leadRepository.clear()
        freshness.invalidate()
    }
}
