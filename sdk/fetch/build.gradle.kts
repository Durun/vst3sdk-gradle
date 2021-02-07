import org.ajoberstar.grgit.Grgit

plugins {
	id("org.ajoberstar.grgit") version "3.1.1"
}

val sourceBranch = "v3.7.1_build_50"
val sourceUrl = "https://github.com/steinbergmedia/vst3sdk.git"

tasks {
	val checkoutSource by creating {
		doFirst {
			checkout(
				url = sourceUrl,
				branch = sourceBranch,
				dir = buildDir
			)
		}
	}
	val clean by creating {
		doLast {
			delete(buildDir)
		}
	}
}


// Utils
fun checkout(url: Any, branch: String, dir: Any) {
	val repository = runCatching {
		Grgit.open { this.dir = dir }
	}.getOrElse {
		mkdir(file(dir).parentFile)
	    exec {
            workingDir = file(dir).parentFile
	        commandLine = listOf("git", "clone", "--recursive", "$url", file(dir).name)
			standardOutput = System.out
        }.assertNormalExitValue()
        Grgit.open { this.dir = dir }
    } ?: throw kotlin.IllegalStateException("Cannot clone $url")
    repository.let {
        it.fetch()
        it.checkout { this.branch = branch }
    }
}