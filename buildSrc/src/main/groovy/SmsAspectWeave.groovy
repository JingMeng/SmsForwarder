import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main

/** Preserves XAOP's compiled aspects using AGP 8 scoped artifacts. */
abstract class SmsAspectWeave extends DefaultTask {
    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract ListProperty<RegularFile> getInputJars()
    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract ListProperty<Directory> getInputDirectories()
    @Classpath abstract ConfigurableFileCollection getWeaveClasspath()
    @OutputFile abstract RegularFileProperty getOutputJar()

    @TaskAction void weave() {
        def inputs = inputJars.get().collect { it.asFile } + inputDirectories.get().collect { it.asFile }
        def destination = outputJar.get().asFile
        destination.parentFile.mkdirs()
        def messages = new MessageHandler(true)
        new Main().run([
            '-11', '-encoding', 'UTF-8', '-showWeaveInfo',
            '-inpath', inputs.collect { it.absolutePath }.join(File.pathSeparator),
            '-aspectpath', weaveClasspath.asPath,
            '-classpath', weaveClasspath.asPath,
            '-outjar', destination.absolutePath
        ] as String[], messages)
        messages.getMessages(null, true).each { message ->
            if (message.isError() || message.isFailed() || message.isAbort()) logger.error(message.toString())
            else if (message.kind == IMessage.WEAVEINFO) logger.lifecycle(message.toString())
        }
        if (messages.hasAnyMessage(IMessage.ERROR, true)) throw new GradleException('XAOP aspect weaving failed')
    }
}
