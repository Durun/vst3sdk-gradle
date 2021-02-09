# VST3 SDK on Gradle
A Gradle-style package of [VST3 SDK](https://github.com/steinbergmedia/vst3sdk)

## How to Use
1. Build VST3SDK into your own MavenLocal
    ```shell
    ./gradlew publishToMavenLocal
    ```
1. Add it as a dependency to your cpp-application/library project
    ```kotlin
    // build.gradle.kts
    plugins {  
        `cpp-application`
        // or
        `cpp-library`
    }
    
    repositories { 
        mavenLocal()
    }
    
    dependencies { 
        implementation("io.github.durun.vst3sdk-cpp:pluginterfaces:3.7.1")
    }
    ```

## Requirement
- cmake (>=3.4.3)