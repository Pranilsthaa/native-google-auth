# @stha_pranil/native-google-auth

Native Google sign-in for Expo on Android (Credential Manager) and iOS (Google Sign-In SDK).

Requires a development or production build. Expo Go and web are not supported.
iOS requires iOS 16.4 or later; tvOS is not supported.

## Installation

```bash
bunx expo install @stha_pranil/native-google-auth
```

## Google Cloud setup

In the [Google Cloud Console](https://console.cloud.google.com/), select one project
for all your clients and configure the Google Auth Platform branding, audience,
and OAuth consent settings. If the app is in testing, add your Google account as
a test user when required by your audience configuration.

Create these OAuth clients:

| Client type     | Used for                                                               | Configuration                                                                                                                                          |
| --------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Web application | The `webClientId` argument on both platforms and the ID token audience | Use this client ID in `signIn()`.                                                                                                                      |
| iOS             | Identifies the installed iOS app                                       | Match your Expo `ios.bundleIdentifier` exactly.                                                                                                        |
| Android         | Identifies the installed Android app                                   | Match `android.package` and the signing certificate SHA-1. Register the appropriate debug, release, and Play App Signing certificates for your builds. |

Only create the platform clients you need. OAuth client IDs are public identifiers;
never put a client secret in the app. Firebase and `GoogleService-Info.plist` are
not required by this package.

See [Google's iOS setup guide](https://developers.google.com/identity/sign-in/ios/start-integrating)
for creating the iOS and Web application clients.

## iOS setup for Expo apps

### 1. Add the config plugin

Add the package to `plugins` in your app's `app.json`. Use the **iOS OAuth client
ID** here, and your own bundle identifier:

```json
{
  "expo": {
    "ios": {
      "bundleIdentifier": "com.example.myapp"
    },
    "plugins": [
      [
        "@stha_pranil/native-google-auth",
        {
          "iosClientId": "1234567890-iosclient.apps.googleusercontent.com"
        }
      ]
    ]
  }
}
```

The plugin writes `GIDClientID` and registers the reversed iOS client ID as a
callback URL scheme. It preserves existing URL schemes. The package automatically
registers an Expo AppDelegate subscriber to forward Google's authentication
callback; you do not need to edit AppDelegate or install the Google SDK separately.

The **Web application client ID** is supplied at runtime to `signIn(webClientId)`;
it is used as Google's `serverClientID`. Do not pass the iOS client ID to `signIn()`.

### 2. Build and install the native app

On macOS with Xcode and CocoaPods configured:

```bash
bunx expo run:ios
```

This generates the native project when needed, installs pods (including the Google
Sign-In SDK), and builds the app. If your project already has an `ios/` directory,
apply the plugin before rebuilding:

```bash
bunx expo prebuild --platform ios
bunx expo run:ios
```

Alternatively, build using EAS with a development profile configured in your project:

```bash
bunx eas-cli build --platform ios --profile development
```

For an EAS development client, install `expo-dev-client` with
`bunx expo install expo-dev-client`, install the resulting build, then start Metro
with `bunx expo start --dev-client`. Physical-device EAS development builds require
Apple signing credentials and device registration.

Install a new native build after adding the package or changing `iosClientId`.
A Metro reload or an over-the-air update cannot apply native setup changes.

### Existing native projects that do not use prebuild

The app must have Expo Modules integrated and an AppDelegate that inherits from
`ExpoAppDelegate`. Install pods after adding the package. In the app target's
Info.plist, set `GIDClientID` to the iOS client ID and add its reversed value to
`CFBundleURLTypes` → `CFBundleURLSchemes`. For example,
`1234567890-iosclient.apps.googleusercontent.com` becomes
`com.googleusercontent.apps.1234567890-iosclient`. Preserve other URL schemes.
The package's AppDelegate subscriber handles the callback automatically.

See [Expo AppDelegate subscribers](https://docs.expo.dev/modules/appdelegate-subscribers/)
and [Google's callback integration](https://developers.google.com/identity/sign-in/ios/sign-in)
for custom application lifecycle integrations.

## Example usage

The same API works on Android and iOS once platform setup is complete.

```tsx
import { useState } from 'react';
import { Alert, Button, View } from 'react-native';
import NativeGoogleAuth from '@stha_pranil/native-google-auth';

const WEB_CLIENT_ID = 'YOUR_WEB_CLIENT_ID.apps.googleusercontent.com';

export default function SignInScreen() {
  const [isSigningIn, setIsSigningIn] = useState(false);

  async function signIn() {
    setIsSigningIn(true);

    try {
      const user = await NativeGoogleAuth.signIn(WEB_CLIENT_ID);

      // Send user.idToken to your backend over HTTPS for verification.
      // Also available: user.email, user.displayName, and user.photoUrl.
      Alert.alert('Signed in', user.email);
    } catch (error: unknown) {
      if (error instanceof Error && 'code' in error && error.code === 'E_CANCELLED') {
        return;
      }

      const message = error instanceof Error ? error.message : String(error);
      Alert.alert('Google sign-in failed', message);
    } finally {
      setIsSigningIn(false);
    }
  }

  return (
    <View>
      <Button
        title={isSigningIn ? 'Signing in…' : 'Sign in with Google'}
        onPress={signIn}
        disabled={isSigningIn}
      />
    </View>
  );
}
```

## Result and errors

`signIn(webClientId)` returns:

```ts
{
  idToken: string;
  email: string;
  displayName: string | null;
  photoUrl: string | null;
}
```

Send `idToken` to your backend over HTTPS and verify its signature, issuer, expiry,
and audience against your Web application client ID before creating an app session.
See [Google's backend authentication guide](https://developers.google.com/identity/sign-in/ios/backend-auth).

The rejected error exposes `code` and `message`. Google SDK failures include the
provider's message; on iOS, the message also includes the native error domain,
numeric code, and underlying errors when available.

| Code              | Meaning                                                                       |
| ----------------- | ----------------------------------------------------------------------------- |
| `E_CANCELLED`     | The user cancelled sign-in.                                                   |
| `E_CONFIG`        | iOS client ID, callback scheme, or Web client ID is missing or invalid (iOS). |
| `E_IN_PROGRESS`   | Another sign-in request is already active (iOS).                              |
| `E_NO_ACTIVITY`   | No foreground activity or view controller is available.                       |
| `E_NO_CREDENTIAL` | Credential Manager could not provide a Google credential (Android).           |
| `E_PARSE`         | The returned credential or required user data is invalid.                     |
| `E_UNSUPPORTED`   | An unexpected credential type was returned (Android).                         |
| `E_SIGN_IN`       | Google sign-in failed; inspect `message` for details.                         |

For `E_CONFIG`, check both OAuth client types, the bundle identifier, and plugin
configuration, then rebuild. If authentication does not return to the app, check
the callback scheme and AppDelegate integration. For Google authorization errors,
check the OAuth consent configuration and permitted test users.
