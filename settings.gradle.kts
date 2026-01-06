pluginManagement {
    repositories {
        // 优先官方源
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()

        // 阿里云镜像（兜底）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 添加 JitPack 仓库
        maven { url = uri("https://jitpack.io") }
        // 如果你使用阿里云镜像，也添加这个
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        flatDir { dirs("app/libs") }
    }
}

rootProject.name = "Andrio_teacher"
include(":app")
include(":timcommon")
include(":tuiroomkit")
 