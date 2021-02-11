import org.gradle.internal.os.OperatingSystem

plugins {
	`cpp-library`
}

allprojects {
	group = "io.github.durun.vst3sdk-cpp"
	version = "3.7.1"
}

evaluationDependsOn(":sdk")
val fetch = project(":sdk:fetch")
val debug = project(":sdk:debug")
val release = project(":sdk:release")

subprojects {
	apply(plugin = "cpp-library")
	apply(plugin = "maven-publish")

	library {
		linkage.set(listOf(Linkage.STATIC))
		targetMachines.add(machines.linux.x86_64)
		targetMachines.add(machines.macOS.x86_64)
		targetMachines.add(machines.windows.x86_64)
	}
}

project("pluginterfaces") {
	configureModule(
		"pluginterfaces",
		headerDir = fetch.buildDir
	) { it.extension == ".h" }
}



// Common configurations
fun Project.configureModule(libName: String, headerDir: File, headers: List<String>) =
	configureModule(libName, headerDir) { headers.contains(it.relativeTo(headerDir).toString()) }

fun Project.configureModule(libName: String, headerDir: File, headerPred: (File) -> Boolean) {
	val outName = this.name
	val headerOut = buildDir.resolve("headers/cpp-api-headers")
	val debugOut = buildDir.resolve("lib/main/debug")
	val releaseOut = buildDir.resolve("lib/main/release")

	library {
		baseName.set(outName)
	}

	val os = OperatingSystem.current()

	tasks {
		val copyArtifactDebug by creating(Copy::class) {
			dependsOn(debug.tasks["cmakeBuild"])
			from(debug.buildDir.resolve("lib/Debug/${libName.lib()}"))
			into(debugOut.resolve(os.category()))
		}
		val copyArtifactRelease by creating(Copy::class) {
			dependsOn(release.tasks["cmakeBuild"])
			from(release.buildDir.resolve("lib/Release/${libName.lib()}"))
			into(releaseOut.resolve(os.category()))
		}
		val zipArtifactHeader by creating(Zip::class) {
			dependsOn(fetch.tasks["checkoutSource"])
			fromFiles(headerDir, headerPred)
			archiveBaseName.set(headerOut.name)
			destinationDirectory.set(headerOut.parentFile)
		}

		getByName("generateMetadataFileForMainPublication").dependsOn(zipArtifactHeader)
		withType<AbstractPublishToMaven> {
			dependsOn(assemble)
		}
		assemble {
			dependsOn(copyArtifactDebug)
			dependsOn(copyArtifactRelease)
		}
	}
}


// OS Utils
fun OperatingSystem.category() = when {
	isMacOsX -> "macos"
	isWindows -> "windows"
	isLinux -> "linux"
	else -> throw NotImplementedError("Not available in $familyName")
}

fun String.lib() = OperatingSystem.current().let {
	when {
		it.isMacOsX -> "lib$this.a"
		it.isWindows -> "$this.lib"
		it.isLinux -> "lib$this.a"
		else -> throw NotImplementedError("Not available in ${it.familyName}")
	}
}

// File Utils
fun Zip.fromFiles(root: File, predicate: (File) -> Boolean) {
	from(fileTree(root))
	includeEmptyDirs = false
	include {
		when {
			it.isDirectory -> true
			else -> predicate(it.file)
		}
	}
	eachFile {
		copyTo(destinationDirectory.get().asFile.resolve(file.relativeTo(root)))
	}
}

fun Zip.fromFiles(root: File, files: List<String>) = fromFiles(root) { files.contains(it.relativeTo(root).toString()) }
