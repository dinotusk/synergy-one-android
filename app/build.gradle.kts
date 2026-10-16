import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Configuração por ambiente: local.properties (não versionado) ou variável de ambiente (CI).
// Nunca commitar valores. A anon key é pública por design (protegida por RLS); service_role jamais.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun config(propKey: String, envKey: String): String =
    (localProps.getProperty(propKey) ?: System.getenv(envKey) ?: "").trim()

android {
    namespace = "br.com.synergyone.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.synergyone.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Endereço público do backend dos apps (não é segredo). Local: http://10.0.2.2:5173 no emulador.
        buildConfigField("String", "API_BASE_URL", "\"${config("api.baseUrl", "API_BASE_URL").ifBlank { "https://www.synergyecosystem.com.br" }}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${config("supabase.url", "SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${config("supabase.anonKey", "SUPABASE_ANON_KEY")}\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "PERF_LOG", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "PERF_LOG", "false")
        }
        // Só para MEDIR desempenho: R8 e recursos reduzidos como num release otimizado, mas assinado com a chave de debug
        // (a assinatura de publicação não existe aqui e não é tocada). Mesmo applicationId do debug: instala por cima e
        // mantém a sessão para medir sem pedir login. Nunca publicar este tipo.
        create("medicao") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            buildConfigField("boolean", "PERF_LOG", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
