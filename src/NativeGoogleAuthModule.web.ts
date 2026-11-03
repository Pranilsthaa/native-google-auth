import { registerWebModule, NativeModule } from 'expo';

// NativeGoogleAuthModule is not available on the web platform.
class NativeGoogleAuthModule extends NativeModule<{}> {}

export default registerWebModule(NativeGoogleAuthModule, 'NativeGoogleAuthModule');
