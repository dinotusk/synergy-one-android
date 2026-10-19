package br.com.synergyone.android.data.empresa

import br.com.synergyone.android.core.result.ApiResult
import br.com.synergyone.android.data.api.AppApiClient
import br.com.synergyone.android.data.api.EmpresasResponse

interface EmpresaRepository {
    suspend fun listar(): ApiResult<List<Empresa>>
}

/** `GET /api/app/v1/empresas`: o cliente vê só as dele; a equipe vê todas (RLS no servidor). */
class ApiEmpresaRepository(private val client: AppApiClient) : EmpresaRepository {
    override suspend fun listar(): ApiResult<List<Empresa>> =
        when (val r = client.get<EmpresasResponse>("/api/app/v1/empresas")) {
            is ApiResult.Error -> r
            is ApiResult.Success -> ApiResult.Success(
                r.data.empresas.map { Empresa(it.id, it.nome, it.slug, it.plano, it.status, it.modulos) },
            )
        }
}
