import { requireNativeModule } from 'expo-modules-core';

export type GoogleSignInResult = {
  idToken: string;
  email: string;
  displayName: string | null;
  photoUrl: string | null;
};

type NativeGoogleAuthType = {
  signIn(webClientId: string): Promise<GoogleSignInResult>;
};

export default requireNativeModule<NativeGoogleAuthType>('NativeGoogleAuth');
