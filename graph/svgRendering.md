# Dot (software) Render to SVG

## Softwsre used (dot)
(https://graphviz.gitlab.io/)

Already installed on linux via maven, but required to be installed on windows client.

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

## Server returns SVG XML
```java
    @GET
    @Path("graphSubledger/{asOfDate}/{dataLoadMode}/graph.svg")
    @Produces(MediaType.APPLICATION_SVG_XML)
    String graphSubledger(LocalDate asOfDate, DataLoadMode dataLoadMode);
```

## UI uses a ReactSvg component (provided)

```js
import React, { useState } from 'react';
import styles from './PipelineVisualisation.module.css';
import { Item, Toolbar } from 'devextreme-react/toolbar';
import { SelectBox } from 'devextreme-react/select-box';
import { ReactSVG } from 'react-svg';
import svgPanZoom from 'svg-pan-zoom';

interface Pipeline {
  name: string;
  url: string;
}

const PipelineVisualisation: React.FC = () => {
  const [pipeline, setPipeline] = useState<Pipeline | null>(null);
  return (
    <div className={styles.gridContainer}>
      <Toolbar className={styles.toolbar}>
        <Item>
          <SelectBox
            items={[
              { name: 'Subledger', url: '/api/spark/graphSubledger/2018-12-31/AUDIT/graph.svg' },
              { name: 'Stock Report', url: '/api/spark/graphStockReportPipelines/2018-12-31/AUDIT/graph.svg' }
            ]}
            selectedItem={pipeline}
            displayExpr="name"
            onSelectionChanged={(e) => setPipeline(e.selectedItem)}
          />
        </Item>
      </Toolbar>
      {pipeline !== null && (
        <ReactSVG
          loading={() => <span>Loading...</span>}
          className={styles.container}
          src={pipeline?.url ?? ''}
          beforeInjection={(svg) => {
            svg.setAttribute('style', 'height:inherit');
          }}
          afterInjection={(err, svg) => {
            svgPanZoom(svg as HTMLElement, {
              zoomScaleSensitivity: 0.5
            });
          }}
        />
      )}
    </div>
  );
};

export default PipelineVisualisation;

```
