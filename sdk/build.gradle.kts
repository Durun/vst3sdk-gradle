import net.freudasoft.CMakePluginExtension

plugins {
	id("net.freudasoft.gradle-cmake-plugin") version "0.0.2"
}

group = "io.github.durun.vst3sdk-cpp"
version = "3.7.1"

evaluationDependsOn("fetch")
val fetch = project("fetch")
val checkoutSource: Task = fetch.tasks["checkoutSource"]

val configureCmake: CMakePluginExtension.() -> Unit = {
	sourceFolder.set(fetch.buildDir)
	buildTarget.set("install")
	def.put("SMTG_ADD_VST3_PLUGINS_SAMPLES", "OFF")
	def.put("SMTG_ADD_VST3_HOSTING_SAMPLES", "OFF")
	def.put("SMTG_CREATE_PLUGIN_LINK", "OFF")
}

val debug = project("debug") {
	apply(plugin = "net.freudasoft.gradle-cmake-plugin")
	cmake {
		configureCmake()
		workingFolder.set(buildDir)
		buildConfig.set("Debug")
		def.put("CMAKE_BUILD_TYPE", "Debug")
	}
	tasks["cmakeConfigure"].dependsOn(checkoutSource)
}

val release = project("release") {
	apply(plugin = "net.freudasoft.gradle-cmake-plugin")
	cmake {
		configureCmake()
		workingFolder.set(buildDir)
		buildConfig.set("Release")
		def.put("CMAKE_BUILD_TYPE", "Release")
	}
	tasks["cmakeConfigure"].dependsOn(checkoutSource)
}

tasks {
	val cmakeClean = getByName("cmakeClean") {
		dependsOn(debug.tasks["cmakeClean"], release.tasks["cmakeClean"])
	}
	val clean by creating {
		dependsOn(cmakeClean)
		dependsOn(fetch.tasks["clean"])
	}
}