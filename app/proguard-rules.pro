# Add project specific ProGuard rules here.
# https://developer.android.com/studio/build/shrink-code

# kotlinx.serialization: mantém os serializadores gerados dos modelos do app (reflexão em tempo de execução).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class br.com.synergyone.android.** {
    *** Companion;
}
-keepclasseswithmembers class br.com.synergyone.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class br.com.synergyone.android.**$$serializer { *; }
