plugins {
	`cpp-library`
	`maven-publish`
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
	apply(plugin="cpp-library")
	apply(plugin="maven-publish")

	val headerOut = buildDir.resolve("headers/cpp-api-headers")
	val debugOut = buildDir.resolve("lib/main/debug")
	val releaseOut = buildDir.resolve("lib/main/release")

	library {
		linkage.set(listOf(Linkage.STATIC))
		targetMachines.add(machines.linux.x86_64)
		targetMachines.add(machines.macOS.x86_64)
		targetMachines.add(machines.windows.x86_64)
		baseName.set("pluginterfaces")
	}

	tasks {
		val copyArtifactDebugLinux by creating(Copy::class) {
			fromFiles(
				builtBy = debug.tasks["cmakeBuild"],
				root = debug.buildDir
			) { it.name == "libpluginterfaces.a" }
			into(debugOut.resolve("linux"))
		}
		val copyArtifactReleaseLinux by creating(Copy::class) {
			fromFiles(
				builtBy = release.tasks["cmakeBuild"],
				root = release.buildDir
			) { it.name == "libpluginterfaces.a" }
			into(releaseOut.resolve("linux"))
		}
		val zipArtifactHeaderLinux by creating(Zip::class) {
			fromFileTree(
				builtBy = fetch.tasks["checkoutSource"],
				root = fetch.buildDir,
				subDirs = listOf("pluginterfaces")
			) { it.extension == "h" }
			archiveBaseName.set(headerOut.name)
			destinationDirectory.set(headerOut.parentFile)
		}

		getByName("generateMetadataFileForMainPublication").dependsOn(zipArtifactHeaderLinux)
		withType<AbstractPublishToMaven> {
			dependsOn(assemble)
		}
		assemble {
			dependsOn(copyArtifactDebugLinux)
			dependsOn(copyArtifactReleaseLinux)
		}
	}
}

publishing {
	repositories {
		mavenLocal()
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