# @stha_pranil/native-google-auth

Native Google sign-in for Expo using Android Credential Manager.

## Example usage

Use your Google OAuth **Web application client ID**, not your Android client ID.
Configure the Android OAuth client in the same Google Cloud project with your
app's package name and signing certificate SHA-1.

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

This package includes native code and requires a development or production build;
it does not run in Expo Go. The example above targets Android.
