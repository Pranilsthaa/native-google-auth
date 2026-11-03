import ExpoModulesCore

public class NativeGoogleAuthModule: Module {
  public func definition() -> ModuleDefinition {
    Name("NativeGoogleAuth")

    AsyncFunction("setValueAsync") { (value: String) in
    }
  }
}
