import ExpoModulesCore
import GoogleSignIn
import UIKit

public class NativeGoogleAuthModule: Module {
  private var isSigningIn = false

  public func definition() -> ModuleDefinition {
    Name("NativeGoogleAuth")

    AsyncFunction("signIn") { (webClientId: String, promise: Promise) in
      guard !self.isSigningIn else {
        promise.reject("E_IN_PROGRESS", "Google sign-in is already in progress")
        return
      }

      guard let iosClientId = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String,
        self.isValidClientId(iosClientId) else {
        promise.reject(
          "E_CONFIG",
          "Missing or invalid GIDClientID. Configure the native-google-auth plugin with your iOS OAuth client ID and rebuild the app."
        )
        return
      }

      guard self.isValidClientId(webClientId) else {
        promise.reject("E_CONFIG", "Pass a valid Web application OAuth client ID to signIn(webClientId)")
        return
      }

      let reversedClientId = iosClientId.components(separatedBy: ".").reversed().joined(separator: ".")
      let urlTypes = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
      let hasCallbackScheme = urlTypes.contains {
        ($0["CFBundleURLSchemes"] as? [String])?.contains(reversedClientId) == true
      }
      guard hasCallbackScheme else {
        promise.reject(
          "E_CONFIG",
          "Missing Google callback URL scheme: \(reversedClientId). Apply the native-google-auth config plugin and rebuild the app."
        )
        return
      }

      guard let viewController = self.appContext?.utilities?.currentViewController(),
        viewController.viewIfLoaded?.window != nil else {
        promise.reject("E_NO_ACTIVITY", "No foreground view controller available to present Google sign-in")
        return
      }

      GIDSignIn.sharedInstance.configuration = GIDConfiguration(
        clientID: iosClientId,
        serverClientID: webClientId
      )

      self.isSigningIn = true
      GIDSignIn.sharedInstance.signIn(withPresenting: viewController) { result, error in
        defer { self.isSigningIn = false }

        if let error = error as NSError? {
          let isCancelled = error.domain == kGIDSignInErrorDomain &&
            error.code == GIDSignInErrorCode.canceled.rawValue
          promise.reject(
            isCancelled ? "E_CANCELLED" : "E_SIGN_IN",
            self.errorMessage(isCancelled ? "User cancelled sign-in" : "Google sign-in failed", error: error)
          )
          return
        }

        guard let user = result?.user,
          let idToken = user.idToken?.tokenString, !idToken.isEmpty else {
          promise.reject("E_PARSE", "Google sign-in returned no ID token")
          return
        }

        guard let email = user.profile?.email, !email.isEmpty else {
          promise.reject("E_PARSE", "Google sign-in returned no email address")
          return
        }

        // Explicit NSNull values keep optional fields consistent with Android.
        let response: [String: Any] = [
          "idToken": idToken,
          "email": email,
          "displayName": user.profile?.name as Any? ?? NSNull(),
          "photoUrl": user.profile?.imageURL(withDimension: 256)?.absoluteString as Any? ?? NSNull()
        ]
        promise.resolve(response)
      }
    }.runOnQueue(.main)
  }

  private func isValidClientId(_ value: String) -> Bool {
    value.trimmingCharacters(in: .whitespacesAndNewlines) == value &&
      value.range(of: "^[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com$", options: .regularExpression) != nil
  }

  private func errorMessage(_ summary: String, error: NSError) -> String {
    var details: [String] = []
    var visited = Set<ObjectIdentifier>()
    var current: NSError? = error

    while let cause = current, visited.insert(ObjectIdentifier(cause)).inserted {
      details.append("\(cause.domain) (\(cause.code)): \(cause.localizedDescription)")
      current = cause.userInfo[NSUnderlyingErrorKey] as? NSError
    }

    return "\(summary). \(details.joined(separator: "; caused by: "))"
  }
}
