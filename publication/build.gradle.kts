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


val pluginterfacesHeaders = listOf(
	"pluginterfaces/base/conststringtable.h",
	"pluginterfaces/base/falignpop.h",
	"pluginterfaces/base/falignpush.h",
	"pluginterfaces/base/fplatform.h",
	"pluginterfaces/base/fstrdefs.h",
	"pluginterfaces/base/ftypes.h",
	"pluginterfaces/base/funknown.h",
	"pluginterfaces/base/futils.h",
	"pluginterfaces/base/fvariant.h",
	"pluginterfaces/base/geoconstants.h",
	"pluginterfaces/base/ibstream.h",
	"pluginterfaces/base/icloneable.h",
	"pluginterfaces/base/ierrorcontext.h",
	"pluginterfaces/base/ipersistent.h",
	"pluginterfaces/base/ipluginbase.h",
	"pluginterfaces/base/istringresult.h",
	"pluginterfaces/base/iupdatehandler.h",
	"pluginterfaces/base/keycodes.h",
	"pluginterfaces/base/pluginbasefwd.h",
	"pluginterfaces/base/smartpointer.h",
	"pluginterfaces/base/typesizecheck.h",
	"pluginterfaces/base/ucolorspec.h",
	"pluginterfaces/base/ustring.h",
	"pluginterfaces/gui/iplugview.h",
	"pluginterfaces/gui/iplugviewcontentscalesupport.h",
	"pluginterfaces/vst/ivstattributes.h",
	"pluginterfaces/vst/ivstaudioprocessor.h",
	"pluginterfaces/vst/ivstautomationstate.h",
	"pluginterfaces/vst/ivstchannelcontextinfo.h",
	"pluginterfaces/vst/ivstcomponent.h",
	"pluginterfaces/vst/ivstcontextmenu.h",
	"pluginterfaces/vst/ivsteditcontroller.h",
	"pluginterfaces/vst/ivstevents.h",
	"pluginterfaces/vst/ivsthostapplication.h",
	"pluginterfaces/vst/ivstinterappaudio.h",
	"pluginterfaces/vst/ivstmessage.h",
	"pluginterfaces/vst/ivstmidicontrollers.h",
	"pluginterfaces/vst/ivstmidilearn.h",
	"pluginterfaces/vst/ivstnoteexpression.h",
	"pluginterfaces/vst/ivstparameterchanges.h",
	"pluginterfaces/vst/ivstparameterfunctionname.h",
	"pluginterfaces/vst/ivstphysicalui.h",
	"pluginterfaces/vst/ivstpluginterfacesupport.h",
	"pluginterfaces/vst/ivstplugview.h",
	"pluginterfaces/vst/ivstprefetchablesupport.h",
	"pluginterfaces/vst/ivstprocesscontext.h",
	"pluginterfaces/vst/ivstrepresentation.h",
	"pluginterfaces/vst/ivstunits.h",
	"pluginterfaces/vst/vstpresetkeys.h",
	"pluginterfaces/vst/vstpshpack4.h",
	"pluginterfaces/vst/vstspeaker.h",
	"pluginterfaces/vst/vsttypes.h"
)

val baseHeaders = listOf(
	"base/source/classfactoryhelpers.h",
	"base/source/fbuffer.h",
	"base/source/fcleanup.h",
	"base/source/fcommandline.h",
	"base/source/fdebug.h",
	"base/source/fdynlib.h",
	"base/source/fobject.h",
	"base/source/fstreamer.h",
	"base/source/fstring.h",
	"base/source/timer.h",
	"base/source/updatehandler.h",
	"base/thread/include/fcondition.h",
	"base/thread/include/flock.h"
)

val sdk_commonHeaders = baseHeaders + listOf(
	"public.sdk/source/common/openurl.h",
	"public.sdk/source/common/systemclipboard.h",
	"public.sdk/source/common/threadchecker.h",
	"public.sdk/source/vst/vstpresetfile.h"
)

val sdkHeaders = sdk_commonHeaders + listOf(
	"public.sdk/source/common/pluginview.h",
	"public.sdk/source/main/pluginfactory.h"
)

val sdk_hostingHeaders = sdk_commonHeaders + listOf(
	"public.sdk/source/vst/hosting/connectionproxy.h",
	"public.sdk/source/vst/hosting/eventlist.h",
	"public.sdk/source/vst/hosting/hostclasses.h",
	"public.sdk/source/vst/hosting/module.h",
	"public.sdk/source/vst/hosting/parameterchanges.h",
	"public.sdk/source/vst/hosting/pluginterfacesupport.h",
	"public.sdk/source/vst/hosting/processdata.h",
	"public.sdk/source/vst/utility/optional.h",
	"public.sdk/source/vst/utility/stringconvert.h",
	"public.sdk/source/vst/utility/uid.h",
	"public.sdk/source/vst/utility/versionparser.h"
)

project("pluginterfaces") {
	configureModule(
		"pluginterfaces",
		headerDir = fetch.buildDir,
		headers = pluginterfacesHeaders
	)
}

project("base") {
	configureModule("base",
		headerDir = fetch.buildDir,
		headers = baseHeaders)
}
project("sdk_common") {
	configureModule("sdk_common",
		headerDir = fetch.buildDir,
		headers = sdk_commonHeaders)
}
project("sdk") {
	configureModule(
		"sdk",
		headerDir = fetch.buildDir,
		headers = sdkHeaders)
}
project("sdk_hosting") {
	configureModule(
		"sdk_hosting",
		headerDir = fetch.buildDir,
		headers = sdk_hostingHeaders)
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
