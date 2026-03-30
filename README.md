This is a Kotlin Multiplatform project for ML.

### How to create .aar for Android and .xcFramework for iOS

To build and create both SDK we follow below step
- For Android .aar
  ```shell
  ./gradlew :{ModuleName}:bundleReleaseAar
  ```
  `For Example:- ./gradlew :MRZParser:bundleReleaseAar`

- For iOS .xcFramework
  ```shell
  ./gradlew :{ModuleName}:createXCFramework
  ```

`For Example:- ./gradlew :MRZParser:createXCFramework`
