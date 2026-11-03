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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NativeGoogleAuthModule : Module() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private fun errorMessage(summary: String, error: Throwable): String {
    val details = mutableListOf<String>()
    val visited = mutableSetOf<Throwable>()
    var current: Throwable? = error

    while (current != null && visited.add(current)) {
      val type = if (current is GetCredentialException) current.type else current.javaClass.simpleName
      val message = current.message?.takeIf { it.isNotBlank() }
      details.add(if (message != null) "$type: $message" else type)
      current = current.cause
    }

    return "$summary. ${details.joinToString("; caused by: ")}"
  }

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
            promise.reject("E_UNSUPPORTED", "Unexpected credential type: ${credential.type}", null)
          }
        } catch (e: GetCredentialCancellationException) {
          promise.reject("E_CANCELLED", errorMessage("User cancelled sign-in", e), e)
        } catch (e: NoCredentialException) {
          promise.reject("E_NO_CREDENTIAL", errorMessage("No Google credential available", e), e)
        } catch (e: GoogleIdTokenParsingException) {
          promise.reject("E_PARSE", errorMessage("Invalid Google ID token", e), e)
        } catch (e: GetCredentialException) {
          promise.reject("E_SIGN_IN", errorMessage("Google sign-in failed", e), e)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          promise.reject("E_SIGN_IN", errorMessage("Google sign-in failed", e), e)
        }
      }
    }

    OnDestroy {
      scope.cancel()
    }
  }
}
