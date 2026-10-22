package com.miflix.native2.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local credentials are encrypted and scoped to the signed-in account, never packaged in the APK. */
class NativeSourceStore(context: Context) {
    private val prefs=context.getSharedPreferences("brunio_native_sources",Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val alias="brunio_native_sources_v1"
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun enabled(owner: String?)=owner!=null && prefs.getBoolean("enabled_$owner",false)
    fun hasKey(owner: String?)=owner!=null && prefs.contains("key_$owner")
    fun onlyNative(owner:String?)=owner!=null && prefs.getBoolean("only_native_rp75_$owner",false)
    fun setOnlyNative(owner:String,value:Boolean) {prefs.edit().putBoolean("only_native_rp75_$owner",value).apply()}
    fun offerSetup(owner:String?)=owner!=null && !hasKey(owner) && !prefs.getBoolean("setup_done_rp75_$owner",false)
    fun dismissSetup(owner:String) {prefs.edit().putBoolean("setup_done_rp75_$owner",true).apply()}
    fun token(owner: String?): String {
        if(owner==null) return ""
        val value=prefs.getString("key_$owner",null) ?: return ""
        return try {
            val parts=value.split(':');val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8)
        } catch(_: Exception) { "" }
    }
    fun save(owner: String,token: String) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val encrypted=cipher.doFinal(token.trim().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("key_$owner",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply()
    }
    fun enable(owner: String,value: Boolean) { prefs.edit().putBoolean("enabled_$owner",value).apply() }
    fun clear(owner: String) { prefs.edit().remove("key_$owner").remove("enabled_$owner").remove("only_native_rp75_$owner").apply() }
}
