# Dot (is the equiv of windows exe) Render to SVG

## Creates an svg file, which is then converted into a String for UI

A ProcessBuilder is an Operating System Process (rt.jar). 
This is used to execute the command to build the svg file on the file system
Look like the ProcessBuilder is created, with file and started. 
Only after the process is running does the dot string get used as input to process

```scala
import java.io.{IOException, OutputStreamWriter}
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import javax.ws.rs.InternalServerErrorException
import org.apache.commons.lang3.StringUtils

object DotRender {

  def dotToSvg(dot: String): String = {
    try {
      val tempFile: Path = Files.createTempFile("", ".svg")
      val dotProcess = new ProcessBuilder(dotExecutable, "-T", "svg", "-o", tempFile.toAbsolutePath.toString)
      val process: Process = dotProcess.start
      val outputStreamWriter = new OutputStreamWriter(process.getOutputStream)
      try {
        outputStreamWriter.write(dot)
        outputStreamWriter.flush()
      } finally if (outputStreamWriter != null) outputStreamWriter.close()
      //should finish in a few 100 millis
      process.waitFor(1, TimeUnit.SECONDS)
      val output: String = Files.readAllLines(tempFile).stream.collect(Collectors.joining("\n"))
      Files.deleteIfExists(tempFile)
      output
    } catch {
      case e@(_: IOException | _: InterruptedException) =>
        throw new InternalServerErrorException(e)
    }
  }

  private def dotExecutable = {
    val exe: String = System.getenv("DOT_EXECUTABLE")
    if (StringUtils.isNotBlank(exe)) exe
    else "dot"
  }

}

```
