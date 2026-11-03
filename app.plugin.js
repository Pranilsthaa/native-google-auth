const { withInfoPlist } = require('expo/config-plugins');

/**
 * @param {import('expo/config').ExpoConfig} config
 * @param {{ iosClientId: string }} options
 */
module.exports = function withNativeGoogleAuth(config, { iosClientId } = {}) {
  if (
    typeof iosClientId !== 'string' ||
    iosClientId.trim() !== iosClientId ||
    !/^[A-Za-z0-9_-]+\.apps\.googleusercontent\.com$/.test(iosClientId)
  ) {
    throw new Error(
      '@stha_pranil/native-google-auth: provide a valid iosClientId in the config plugin options. ' +
        'Use the iOS OAuth client ID from Google Cloud Console.'
    );
  }

  const reversedClientId = iosClientId.split('.').reverse().join('.');

  return withInfoPlist(config, (config) => {
    config.modResults.GIDClientID = iosClientId;

    const urlTypes = config.modResults.CFBundleURLTypes ?? [];
    if (!urlTypes.some((type) => type.CFBundleURLSchemes?.includes(reversedClientId))) {
      urlTypes.push({ CFBundleURLSchemes: [reversedClientId] });
    }
    config.modResults.CFBundleURLTypes = urlTypes;

    return config;
  });
};
