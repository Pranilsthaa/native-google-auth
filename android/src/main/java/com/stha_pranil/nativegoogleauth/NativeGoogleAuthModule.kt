package com.stha_pranil.nativegoogleauth

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NativeGoogleAuthModule : Module() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun definition() = ModuleDefinition {
    Name("NativeGoogleAuth")

    AsyncFunction("signIn") { webClientId: String, promise: Promise ->
      val activity = appContext.currentActivity
      if (activity == null) {
        promise.reject("E_NO_ACTIVITY", "No foreground activity available", null)
        return@AsyncFunction
      }

      scope.launch {
        try {
          val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

          val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

          val result = CredentialManager.create(activity).getCredential(activity, request)
          val credential = result.credential

          if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
          ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            promise.resolve(
              mapOf(
                "idToken" to googleCred.idToken,
                "email" to googleCred.id,
                "displayName" to googleCred.displayName,
                "photoUrl" to googleCred.profilePictureUri?.toString()
              )
            )
          } else {
            promise.reject("E_UNSUPPORTED", "Unexpected credential type", null)
          }
        } catch (e: GetCredentialCancellationException) {
          promise.reject("E_CANCELLED", "User cancelled sign-in", e)
        } catch (e: NoCredentialException) {
          promise.reject("E_NO_CREDENTIAL", "No Google account available on this device", e)
        } catch (e: GoogleIdTokenParsingException) {
          promise.reject("E_PARSE", "Invalid Google ID token", e)
        } catch (e: GetCredentialException) {
          promise.reject("E_SIGN_IN", e.message ?: "Sign-in failed", e)
        }
      }
    }

    OnDestroy {
      scope.cancel()
    }
  }
}