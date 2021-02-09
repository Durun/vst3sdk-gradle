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

project("pluginterfaces") {
	apply(plugin = "cpp-library")
	apply(plugin = "maven-publish")

	val headerOut = buildDir.resolve("headers/cpp-api-headers")
	val debugOut = buildDir.resolve("lib/main/debug")
	val releaseOut = buildDir.resolve("lib/main/release")

	val libName = "pluginterfaces"

	library {
		linkage.set(listOf(Linkage.STATIC))
		targetMachines.add(machines.linux.x86_64)
		targetMachines.add(machines.macOS.x86_64)
		targetMachines.add(machines.windows.x86_64)
		baseName.set(libName)
	}

	val os = OperatingSystem.current()

	tasks {
		val copyArtifactDebug by creating(Copy::class) {
			fromFiles(
				builtBy = debug.tasks["cmakeBuild"],
				root = debug.buildDir
			) { it.name == libName.lib() }
			into(debugOut.resolve(os.category()))
		}
		val copyArtifactRelease by creating(Copy::class) {
			fromFiles(
				builtBy = release.tasks["cmakeBuild"],
				root = release.buildDir
			) { it.name == libName.lib() }
			into(releaseOut.resolve(os.category()))
		}
		val zipArtifactHeader by creating(Zip::class) {
			fromFileTree(
				builtBy = fetch.tasks["checkoutSource"],
				root = fetch.buildDir,
				subDirs = listOf("pluginterfaces")
			) { it.extension == "h" }
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


fun OperatingSystem.category() = when {
	isLinux -> "linux"
	isMacOsX -> "macos"
	isWindows -> "windows"
	else -> throw NotImplementedError("Not available in $familyName")
}

fun String.lib() = OperatingSystem.current().let {
	when {
		it.isLinux -> "lib$this.a"
		it.isMacOsX -> "lib$this.dylib"
		it.isWindows -> "$this.lib"
		else -> throw NotImplementedError("Not available in ${it.familyName}")
	}
}

fun Copy.fromFiles(builtBy: Task, root: File, predicate: (File) -> Boolean) {
	dependsOn(builtBy)
	from(fileTree(root).apply { builtBy(builtBy) }.filter { it.isDirectory || predicate(it) })
}

fun Copy.fromFileTree(builtBy: Task, root: File, subDirs: List<String>, predicate: (File) -> Boolean) {
	dependsOn(builtBy)
	subDirs.forEach {
		from(fileTree(root.resolve(it)).apply { builtBy(builtBy) })
	}
	include { it.isDirectory || predicate(it.file) }
}

fun Zip.fromFileTree(builtBy: Task, root: File, subDirs: List<String>, predicate: (File) -> Boolean) {
	dependsOn(builtBy)
	from(fileTree(root).apply { builtBy(builtBy) })
	include {
		when {
			it.isDirectory -> subDirs.any { dir -> it.relativePath.startsWith(dir) }
			else -> predicate(it.file)
		}
	}
	eachFile {
		copyTo(destinationDirectory.get().asFile.resolve(file.relativeTo(root)))
	}
}