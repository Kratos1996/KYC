Pod::Spec.new do |spec|
    spec.name                     = 'FaceController'
    spec.version                  = '0.0.1'
    spec.homepage                 = 'https://github.com/ctn-mobile-innovation/KMP-ml/'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Face Liveness Detection'
    spec.vendored_frameworks      = 'build/cocoapods/framework/FaceController.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '13.0'
    spec.dependency 'MLKitCommon'
    spec.dependency 'MLKitTextRecognition'
    spec.dependency 'MLKitVision'
    spec.dependency 'MediaPipeTasksVision'
    spec.dependency 'kta-tesseract'
                
    if !Dir.exist?('build/cocoapods/framework/FaceController.framework') || Dir.empty?('build/cocoapods/framework/FaceController.framework')
        raise "

        Kotlin framework 'FaceController' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :FaceController:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':FaceController',
        'PRODUCT_MODULE_NAME' => 'FaceController',
    }
                
    spec.script_phases = [
        {
            :name => 'Build FaceController',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
    spec.resources = ['build/compose/cocoapods/compose-resources']
end