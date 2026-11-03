// Reexport the native module. On web, it will be resolved to NativeGoogleAuthModule.web.ts
// and on native platforms to NativeGoogleAuthModule.ts
export { default } from './NativeGoogleAuthModule';
export * from './NativeGoogleAuth.types';
