package com.mercuria.giant.spark.subledger.common.pipeline

import java.io.File
import java.time
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import com.mercuria.giant.spark.StorageNameToHumanFriendlyName
import com.mercuria.giant.spark.pipeline.{DataFrameComponent, PipelineComponent}
import com.mercuria.giant.spark.store.StorageInfo.State
import com.mercuria.giant.spark.store.StorageRead.StorageReadResult
import com.mercuria.giant.spark.store._
import com.mercuria.giant.spark.subledger.common.RawDataKeys
import com.mercuria.giant.spark.subledger.common.loader.GiantDataLoader
import com.mercuria.giant.spark.subledger.common.pipeline.Pipeline.PageName
import com.mercuria.giant.spark.subledger.common.pipeline.PipelineExecutor.{ExecutionMode, Parallel, Serial}
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.apache.hadoop.fs.FileUtil
import org.apache.spark.SparkStatusTracker
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.collection.{Set, mutable}
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PipelineExecutor[A <: PipelineConfig](
                                             sparkSession: SparkSession,
                                             giantDataLoader: GiantDataLoader,
                                             storageReadWrite: StorageReadWrite,
                                             pipelines: Seq[ComputationPipeline[A]],
                                             sleepDuration: Duration = Duration(5, TimeUnit.SECONDS),
                                             executionMode: ExecutionMode = Serial,
                                             lazyPipelineCheckDuration: Duration = Duration(15, TimeUnit.SECONDS),
                                             numberOfPartitions: Long = 10
                                           ) {

  private val DEFAULT_PARTITIONS: Long = 10

  private val logger: Logger = LoggerFactory.getLogger(classOf[PipelineExecutor[_]])

  import scala.collection.JavaConverters._

  def execute(pipelineConfig: A): Map[PipelineDataKey, PipelineComponent] = {
    cleanup(sparkSession, numberOfPartitions)
    try {

      val allPipelineNodes: Seq[PipelineNode[Pipeline[A]]] = createAllPipelineNodes(pipelineConfig)

      logger.info(s"Starting computation after building pipeline graph.  Pipeline contains ${allPipelineNodes.size} nodes.")

      val result = executionMode match {
        case Serial => executeInSerial(pipelineConfig, allPipelineNodes.toList)
        case Parallel => executeInParallel(pipelineConfig, allPipelineNodes.toList)
      }
      result.mapValues(_.pipelineComponent)
    } finally {
      // Always set to the default partitions after execution
      cleanup(sparkSession, DEFAULT_PARTITIONS)
    }
  }

  private def cleanup(sparkSession: SparkSession, partitions: Long): Unit = {
    PipelineExecutor.Lock.synchronized {
      val statusTracker: SparkStatusTracker = sparkSession.sparkContext.statusTracker
      if (statusTracker.getActiveJobIds().isEmpty && statusTracker.getActiveStageIds().isEmpty) {
        logger.debug("No spark jobs are running, so will try and clear any cached datasets")
        sparkSession.catalog.clearCache()
        sparkSession.sparkContext.getPersistentRDDs.valuesIterator.foreach(_.unpersist())
        sparkSession.sparkContext.getCheckpointDir.foreach(checkpointDir => {
          logger.debug(s"Clearing checkpoint directory contents in $checkpointDir")
          FileUtil.fullyDeleteContents(new File(checkpointDir))
        })
      } else {
        logger.debug(s"Active job IDs: ${statusTracker.getActiveJobIds()} active stage IDs: ${statusTracker.getActiveStageIds()}")
      }
      sparkSession.conf.set("spark.sql.shuffle.partitions", partitions)
    }
  }

  /**
    * Generates a diagraph of the pipeline nodes and the data sets they produce.
    *
    * Multistage piplines are broken up and displayed as subgraphs.
    */
  def toDotGraph(pipelineConfig: A): String = {

    val allPipelineNodes: Seq[PipelineNode[Pipeline[A]]] = createAllPipelineNodes(pipelineConfig)

    val data: Seq[String] = toDotGraphLines(allPipelineNodes)

    val graph =
      s"""
         |digraph "Pipeline" {
         |${data.mkString("\n")}
         |}
     """.stripMargin

    graph
  }

  private def toDotGraphLines(pipelineNodes: Seq[PipelineNode[Pipeline[A]]]): Seq[String] = {

    def pageId(page: PipelineDataKey): String = {
      if (page.isSideEffect) {
        s"d_${page.task}_se_${page.date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
      } else {
        s"d_${page.task}_${page.page}_${page.date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
      }
    }

    def nodeId(any: AnyRef) = s"n${any.hashCode()}"

    val nodes: Seq[String] = pipelineNodes.flatMap {
      case PipelineNode(_: LoadingPipeline[A], _, _) =>
        Seq() //shouldn't be any loading pipelines in here anywway
        Seq() //shouldn't be any loading pipelines in here anywway

      case PipelineNode(multi: MultiStagePipeline[A], _, _) =>
        toDotGraphLines(multi.nestedNodes)

      case PipelineNode(compute: ComputationPipeline[A], inputs, outputs) =>
        val node = s"""${nodeId(compute)} [label="${compute.name}",shape="component",fillcolor="#018571", style=filled];"""
        val linksOut: Set[String] = outputs.map(page => s"""${nodeId(compute)} -> ${pageId(page)};""")
        val linksIn: Set[String] = inputs.map(page => s"""${pageId(page)} -> ${nodeId(compute)};""")
        linksOut ++ linksIn + node
    }


    val pages: Seq[String] = pipelineNodes.flatMap {
      case PipelineNode(_, input, output) => input ++ output
    }
      .distinct
      .map(page => {
        if (page.page == RawDataKeys.Pages.RawData) {
          s"""${pageId(page)} [label="${StorageNameToHumanFriendlyName.getForTask(page.task)} - ${page.date}",shape="invhouse",fillcolor="#80cdc1", style=filled];"""
        } else if (page.isSideEffect) {
          s"""${pageId(page)} [label="External Output for ${StorageNameToHumanFriendlyName.getForTask(page.task)}",shape="note",fillcolor="#a6611a", style=filled];"""
        } else {
          s"""${pageId(page)} [label="${StorageNameToHumanFriendlyName.getForTaskAndPage(page.task, page.page)} - ${page.date}",shape="folder",fillcolor="#dfc27d", style=filled];"""
        }
      })

    pages ++ nodes
  }

  private def isDevEnvironment: Boolean = {
    System.getenv("GIANT_ENV") match {
      case null | "" | "local" | "dev" => true
      case _ => false
    }
  }

  private def createAllPipelineNodes(pipelineConfig: A): Seq[PipelineNode[Pipeline[A]]] = {

    val startTime: Instant = Instant.now()
    //we are going to need to know the inputs and outputs of each node, so collect this up front
    val partialComputePipelines: Seq[PipelineNode[ComputationPipeline[A]]] = pipelines.map(pipeline => {
      val (inputKeys, outputKeys) = getInputsAndOutputs(pipelineConfig, pipeline)
      PipelineNode(pipeline, inputKeys, outputKeys)
    })
    val inputOutputGatheringTime = time.Duration.between(startTime, Instant.now())
    if (inputOutputGatheringTime.toMillis >= lazyPipelineCheckDuration.toMillis) {
      val message = s"It took ${inputOutputGatheringTime.toMillis} milliseconds to work out the inputs and outputs to your pipeline execution, this likely indicates your pipeline steps are NOT lazy and is an issue"
      logger.error(message)
      if (isDevEnvironment) {
        //if this is in dev, throw an explicit exception so we are forced to fix this issue.
        throw new IllegalStateException(message)
      }
    } else {
      logger.debug("It took {} seconds to work out the inputs and outputs to your pipeline execution", inputOutputGatheringTime.getSeconds)
    }

    //some tasks will have their pages split over multiple pipelines, but it makes things alot simpler if they appear as a single pipeline
    //this code will combine them together
    val computePipelines: Seq[PipelineNode[ComputationPipeline[A]]] = combinePipelinesThatProducePagesFromSameTaskIntoOne(partialComputePipelines)

    //raw data pages are loaded automatically using these nodes
    val loadPipelines: Iterable[PipelineNode[LoadingPipeline[A]]] = createRawDataLoadPipelines(pipelineConfig, computePipelines)

    val allPipelineNodes: Seq[PipelineNode[Pipeline[A]]] = loadPipelines.toSeq ++ computePipelines

    verifyPipelines(allPipelineNodes)
    allPipelineNodes
  }

  private def combinePipelinesThatProducePagesFromSameTaskIntoOne(computePipelines: Seq[PipelineNode[ComputationPipeline[A]]]): Seq[PipelineNode[ComputationPipeline[A]]] = {
    val groupedByOutput = computePipelines
      //group by task - at this point, we haven't checked if the output exists - it should do, and the verify will check this
      .groupBy(_.outputs.headOption.map(pdk => (pdk.task, pdk.date)).getOrElse((UUID.randomUUID().toString, None)))

    logger.debug(s"Outputs ${groupedByOutput.keySet}")

    groupedByOutput
      .flatMap {
        case ((task, date), nodesProducingATask) =>
          if (nodesProducingATask.size == 1) {
            //don't worry about pipelines where all task pages are produced in a single node
            nodesProducingATask
          } else {
            //several nodes are producing a pages for a single task and date, so we need to combine them
            logger.debug(s"Nodes ${nodesProducingATask.map(_.sparkPipeline.name)} all produce data for $task and $date, so need combining")

            //need to find nodes that don't take this task as an input as a starting point
            val (starterNodes, intermediateNodes) = nodesProducingATask.partition(aNode => {
              !aNode.inputs.exists(inputKey => inputKey.task == task)
            })

            require(starterNodes.nonEmpty, s"Found recursive nodes for $task")

            def combine(left: PipelineNode[ComputationPipeline[A]], right: PipelineNode[ComputationPipeline[A]]): PipelineNode[ComputationPipeline[A]] = {
              val pipeline = new MultiStagePipeline(left, right, task)
              val inputs: Set[PipelineDataKey] = (left.inputs ++ right.inputs)
                .filterNot(key => key.task == task) //inputs should not include the same task inputs
              PipelineNode(pipeline, inputs, left.outputs ++ right.outputs)
            }

            val starterNode: PipelineNode[ComputationPipeline[A]] = if (starterNodes.size == 1) starterNodes.head else starterNodes.reduce(combine)

            @tailrec def addIntermediateNodesNodes(node: PipelineNode[ComputationPipeline[A]], nodesToReduce: List[PipelineNode[ComputationPipeline[A]]], iterCount: Int): PipelineNode[ComputationPipeline[A]] = {
              nodesToReduce match {
                case remaining if iterCount > remaining.length =>
                  logger.error(s"Node: ${node.sparkPipeline.name}")
                  throw new IllegalStateException("Found some recursive loop when combining pipelines")
                case Nil => node
                case nextNode :: rest
                  //if the current left hand side has all 'same task' output pages as this node takes as an input
                  if nextNode.inputs.filter(key => key.task == task).forall(key => node.outputs.contains(key)) =>
                  addIntermediateNodesNodes(combine(node, nextNode), rest, 0)
                case nextNode :: rest =>
                  addIntermediateNodesNodes(node, rest ::: List(nextNode), iterCount + 1)
              }
            }

            Seq(addIntermediateNodesNodes(starterNode, intermediateNodes.toList, 0))
          }
      }.toSeq
  }

  /**
    * Checks various preconditions in the pipeline
    */
  private def verifyPipelines(allPipelineNodes: Seq[PipelineNode[Pipeline[A]]]): Unit = {

    val allOutputs = allPipelineNodes.flatMap(_.outputs)

    val tasksSoFar = mutable.Set[(String, LocalDate)]()
    val taskPagesSoFar = mutable.Set[PipelineDataKey]()

    allPipelineNodes.foreach(pipelineNode => {
      val outputTasks = pipelineNode.outputs.map(_.task)
      require(outputTasks.size == 1, s"A pipeline should only output more one task, but ${pipelineNode.sparkPipeline.name} produced $outputTasks")
      val outputTask = outputTasks.head
      val outputDate = pipelineNode.outputs.map(_.date).head

      pipelineNode.inputs.foreach(anInput => {
        require(allOutputs.contains(anInput), s"Nothing is producing $anInput as used as an input in ${pipelineNode.sparkPipeline.name}\n$allOutputs")
      })

      val inputTasks = pipelineNode.inputs.map(_.task)
      require(!inputTasks.contains(outputTask), s"A pipeline should not take and produce the same task but ${pipelineNode.sparkPipeline.name} does with $outputTask ")

      require(!pipelineNode.inputs.exists(key => key.isSideEffect), s"No pipeline should take a side effect as an input, but ${pipelineNode.sparkPipeline.name} does")

      require(!tasksSoFar.contains((outputTask, outputDate)), s"An task for a date should only be produced by one node, but $outputTask for $outputDate produced by several including  ${pipelineNode.sparkPipeline.name}")
      val pagesProducedByOtherPiplines: mutable.Set[PipelineDataKey] = taskPagesSoFar.intersect(pipelineNode.outputs)
      require(pagesProducedByOtherPiplines.isEmpty, s"A page should only be produced by one node, but $pagesProducedByOtherPiplines were produced by several including  ${pipelineNode.sparkPipeline.name}")

      taskPagesSoFar ++= pipelineNode.outputs
      tasksSoFar += ((outputTask, outputDate))
    })
  }

  //  old single threaded approach
  /**
    * Loops through the list of nodes, and executes any where we have all the input, placing nodes where we dont have the input to the bottom of the list
    */
  private def executeInSerial(pipelineConfig: A, nodes: List[PipelineNode[Pipeline[A]]]): Map[PipelineDataKey, PipelineComponentWithStorageInfo] = {

    @tailrec def recurse(
                          pipelinesToProcess: List[PipelineNode[Pipeline[A]]],
                          previouslyComputedData: Map[PipelineDataKey, PipelineComponentWithStorageInfo],
                          triedToCompute: Int
                        ): (List[PipelineNode[Pipeline[A]]], Map[PipelineDataKey, PipelineComponentWithStorageInfo]) = {
      pipelinesToProcess match {
        case Nil => (pipelinesToProcess, previouslyComputedData)
        case pipelineReadyToCompute :: rest if pipelineReadyToCompute.inputs.forall(previouslyComputedData.contains) =>
          val thisPipelineComputedData = executePipeline(pipelineReadyToCompute, pipelineConfig, previouslyComputedData)

          recurse(rest, previouslyComputedData ++ thisPipelineComputedData, 0)

        case all if all.size == triedToCompute =>
          logger.error(s"Cannot execute as stuck in a loop [${all.map(_.sparkPipeline.name).mkString(", ")}]")
          throw new IllegalStateException("Found recursive tasks, and cannot execute any of them. ")

        case itemNotReadyToCompute :: rest =>
          //add this item to the back of the list and carry on
          recurse(rest ::: List(itemNotReadyToCompute), previouslyComputedData, triedToCompute + 1)
      }
    }

    recurse(nodes, Map(), 0)._2
  }

  private def executeInParallel(pipelineConfig: A, nodes: List[PipelineNode[Pipeline[A]]]): Map[PipelineDataKey, PipelineComponentWithStorageInfo] = {

    val executorService: ExecutorService = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().daemon(true).build())
    implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

    @tailrec
    def recurse(
                 pipelinesToProcess: List[PipelineNode[Pipeline[A]]],
                 pipelinesInProgress: List[Future[Map[PipelineDataKey, PipelineComponentWithStorageInfo]]],
                 previouslyComputedData: Map[PipelineDataKey, PipelineComponentWithStorageInfo]
               ): (List[PipelineNode[Pipeline[A]]], Map[PipelineDataKey, PipelineComponentWithStorageInfo]) = {

      //this will find all PipelineNode we can currently execute (where we have all inputs) and spawn them off to compute in a Future
      // then periodically check for any completed Futures, update the computed data with the new entries, and repeat
      // stop when pipelines are completed, or no pipelines are running and we still cant run a pipeline (this indicates a loop)

      if (pipelinesToProcess.isEmpty && pipelinesInProgress.isEmpty) {
        (Nil, previouslyComputedData)
      } else if (pipelinesInProgress.isEmpty && !pipelinesToProcess.exists(_.inputs.forall(previouslyComputedData.contains))) {
        logger.error(s"Cannot execute as no pipelines can be processed [${pipelinesToProcess.map(_.sparkPipeline.name).mkString(", ")}]")
        throw new IllegalStateException("Found recursive tasks, and cannot execute any of them. ")
      } else {
        val (completed, stillInProgress) = pipelinesInProgress.partition(_.isCompleted)

        logger.debug(s"${completed.size} are completed, ${stillInProgress.size} running, ${pipelinesToProcess.size} yet to start")

        if (completed.isEmpty && stillInProgress.nonEmpty) {
          logger.debug(s"No pipelines are complete yet, so sleeping for $sleepDuration seconds.")
          TimeUnit.MILLISECONDS.sleep(sleepDuration.toMillis)
          recurse(pipelinesToProcess, pipelinesInProgress, previouslyComputedData)
        } else {

          val currentComputedData = completed.foldLeft(previouslyComputedData)((data, completedFuture) => completedFuture.value match {
            case None => throw new IllegalStateException("We already checked for ones that are complete so this should not happen")
            case Some(Failure(exception)) =>
              logger.error("Error computing data for node", exception)
              throw new RuntimeException(exception)
            case Some(Success(pipelineData)) => data ++ pipelineData
          })

          val (readyToCompute, notReadyToCompute) = pipelinesToProcess.partition(_.inputs.forall(currentComputedData.contains))
          logger.debug(s"${readyToCompute.size} ready to compute, ${notReadyToCompute.size} not ready")

          val inProgress: List[Future[Map[PipelineDataKey, PipelineComponentWithStorageInfo]]] = readyToCompute.map(toCompute => {
            Future {
              executePipeline(toCompute, pipelineConfig, currentComputedData)
            }
          })

          recurse(notReadyToCompute, stillInProgress ++ inProgress, currentComputedData)
        }
      }
    }

    try {
      recurse(nodes, Nil, Map())._2
    } finally {
      executorService.shutdownNow()
    }
  }

  private def executePipeline(pipelineNode: PipelineNode[Pipeline[A]], pipelineConfig: A, executedSoFar: Map[PipelineDataKey, PipelineComponentWithStorageInfo]): Map[PipelineDataKey, PipelineComponentWithStorageInfo] = {


    logger.debug(s"Going to execute ${pipelineNode.sparkPipeline.name} with ${pipelineNode.inputs}")

    //if all inputs are loaded from storage AND the latest stored version of our dataset also has those same input versions
    //we can just load from the database
    //otherwise we need to execute
    //for a raw data load, we always need to execute (though this is almost a no-op)

    pipelineNode.sparkPipeline match {
      case loadingPipeline: LoadingPipeline[A] =>
        logger.debug(s"Loading ${pipelineNode.outputs.head} from storage")
        val loaded = loadingPipeline.inputLoaded(pipelineConfig)
        val (key, component) = loadingPipeline.generateData(pipelineConfig)
        Map((key, PipelineComponentWithStorageInfo(component, loaded)))

      case sparkPipeline: ComputationPipeline[A] =>
        val inputStorageVersionsVersions: Set[PipelineDataStorageInfo] =
          pipelineNode.inputs.map(key => executedSoFar(key).pipelineDataStorageInfo)


        logger.debug(s"All inputs to ${sparkPipeline.name} are loaded from database, so it is possible we can load from database too")
        logger.debug(s"Input storage versions to ${sparkPipeline.name}: $inputStorageVersionsVersions")

        //load up data from storage if the links match
        val loadedFromStorage = loadFromStorage(pipelineConfig, pipelineNode, inputStorageVersionsVersions)

        if (loadedFromStorage.keySet == pipelineNode.storedOutputs) {
          logger.debug(s"All inputs to ${sparkPipeline.name} match latest storage version, so using data from storage for ${loadedFromStorage.keySet}")
          //if we manged to load up everything, then use it
          loadedFromStorage
        } else {
          logger.debug(s"Not all inputs to ${sparkPipeline.name} match latest storage version, so executing everything")
          val inputForExecution = pipelineNode.inputs.foldLeft(PipelineData.Empty) {
            (pipelineData, key) => pipelineData.withData(key, executedSoFar(key).pipelineComponent)
          }
          val generatedData = sparkPipeline.generateData(pipelineConfig, inputForExecution).data
          writeToStorageAndExecuteSideEffects(pipelineConfig, pipelineNode, generatedData, executedSoFar)
        }

    }
  }

  private def loadFromStorage(
                               pipelineConfig: A,
                               pipelineNode: PipelineNode[Pipeline[A]],
                               inputStorageVersions: Set[PipelineDataStorageInfo]
                             ): Map[PipelineDataKey, PipelineComponentWithStorageInfo] = {
    pipelineNode.storedOutputs.flatMap {
      //note all outputs should have the same storage links, so we should just be able to test the head and load the rest
      // but in case we added or removed a page we are checking all of them
      key =>
        val taskName: TaskName = pipelineConfig.storageConfig.dataLoadMode.asTaskName(key.task)
        val storageRead: StorageReadResult = storageReadWrite.fetchLatestDataPageForDateWithInfo(taskName, key.page, key.date)
        val storageIsOK: Boolean = storageRead.getStorageInfo.optionalState().orElse(State.FAIL) == State.OK
        if (storageIsOK) {
          val version: Long = storageRead.getStorageInfo.version()
          val incomingLinks = storageReadWrite
            .fetchLinksTo(taskName, key.page, version)
            .asScala
            .map(sil => PipelineDataStorageInfo(sil.fromTask(), sil.fromPage(), sil.fromVersion())).toSet

          logger.debug(s"Incoming links to ${pipelineNode.sparkPipeline.name} $key: $incomingLinks")

          if (storageIsOK && incomingLinks == inputStorageVersions) {
            Some((key, PipelineComponentWithStorageInfo(DataFrameComponent(storageRead.getData, key.toString), PipelineDataStorageInfo(taskName.toPath, key.page, version))))
          } else {
            None
          }
        } else {
          None
        }
    }.toMap
  }

  private def writeToStorageAndExecuteSideEffects(pipelineConfig: A,
                                                  node: PipelineNode[_],
                                                  pipelineData: Map[PipelineDataKey, PipelineComponent],
                                                  executedData: Map[PipelineDataKey, PipelineComponentWithStorageInfo]
                                                 ): Map[PipelineDataKey, PipelineComponentWithStorageInfo] = {

    //Grok the task that we're currently working on. There should be exactly one of these because of the rules on nodes.
    val task = node.outputs.map(_.task).head

    val (sideEffects, componentsThatNeedSaving) = pipelineData.partition(_._1.isSideEffect)

    val asOfDate = componentsThatNeedSaving.keys.map(_.date).toList.distinct match {
      case aDate :: Nil => aDate
      case Nil => throw new IllegalStateException(s"Somehow we have no storage dates for ${componentsThatNeedSaving.keySet}, which doesn't make sense")
      case moreThanOneDate => throw new IllegalStateException(s"Is is not allowed to store multiple dates in one go, but we have $moreThanOneDate")
    }

    val dataframeThatNeedSaving = componentsThatNeedSaving.map {
      case (PipelineDataKey(`task`, page, `asOfDate`), component) => (page, component.execute)
      //due to preconditions, this cannot happen, but it keeps the compiler happy
      case (PipelineDataKey(otherTask, page, otherDate), _) => throw new IllegalStateException(s"Received page $page for task $otherTask and $otherDate we did not expect")
    }

    val dataframesLoadedFromStorage = if (dataframeThatNeedSaving.nonEmpty) {
      logger.debug(s"For task $task as of $asOfDate, we have all pages (${dataframeThatNeedSaving.keys.mkString(", ")}), so will save (if required)")

      val linkedInputVersions: Set[PipelineDataStorageInfo] = node.inputs
        .map(inputKey => executedData(inputKey).pipelineDataStorageInfo)(collection.breakOut) //collection.breakOut = magic to turn list into set

      //this should override the pipeline data with the saved version
      storeAndReadBack(pipelineConfig, task, asOfDate, dataframeThatNeedSaving, linkedInputVersions)

    } else {
      logger.debug(s"No outputs for $task as of $asOfDate")
      Map[PipelineDataKey, PipelineComponentWithStorageInfo]()
    }

    //Calculate the side effects only after we've successfully stored the components-that-need-saving, so that
    //we're certain that any data used for side effects is already stored in the DB. [Though side effects are still
    //executed with the live data rather than the version loaded from the DB]
    sideEffects.values.foreach(sideEffect => {
      logger.debug(s"Executing side effect ${sideEffect.name} as of $asOfDate")
      sideEffect.execute
    })


    //do some clean up
    dataframeThatNeedSaving.values.foreach(_.unpersist(blocking = false))


    dataframesLoadedFromStorage
  }

  private def storeAndReadBack(
                                pipelineConfig: A,
                                task: String,
                                asOfDate: LocalDate,
                                pages: Map[String, DataFrame],
                                allInputs: Set[PipelineDataStorageInfo]
                              ): Map[PipelineDataKey, PipelineComponentWithStorageInfo] = {

    logger.debug(s"Going to store $task $asOfDate ${pages.keys}")
    val storageConfig: PipelineStorageConfig = pipelineConfig.storageConfig
    val taskName: TaskName = storageConfig.dataLoadMode.asTaskName(task)
    val storageInfos: mutable.Set[StorageInfo] = storageReadWrite.storeDataset(
      taskName,
      asOfDate,
      pipelineConfig.user,
      pages.asJava
    ).asScala

    //store the links
    val allLinks = storageInfos.flatMap(si => {
      allInputs.map(pdsi =>
        ImmutableStorageInfoLink
          .builder()
          .fromTask(pdsi.task)
          .fromPage(pdsi.page)
          .fromVersion(pdsi.version)
          .toTask(si.task())
          .toPage(si.page())
          .toVersion(si.version())
          .build().asInstanceOf[StorageInfoLink]
      )
    })
    storageReadWrite.addLinks(allLinks.asJava)

    //now load back up from storage so the stored version can be used in place of the spark-cached version
    //this means links for subsequent stages are correct, and is a performance improvement
    storageInfos.map(si => {
      val page: PageName = si.page()
      val version: Long = si.version()
      val data = () => storageReadWrite.fetchSpecificDataPage(ImmutableSimpleTaskName.of(si.task()), page, version)
      (PipelineDataKey(task, page, asOfDate), PipelineComponentWithStorageInfo(DataFrameComponent(data(), s"$task $page (v$version)"), PipelineDataStorageInfo(si.task(), page, version)))
    }).toMap
  }

  /**
    * Raw data pages are special - we know they will be in the database, and we can load them
    */
  private def createRawDataLoadPipelines(pipelineConfig: A, processingPipelines: Seq[PipelineNode[ComputationPipeline[A]]]): Iterable[PipelineNode[LoadingPipeline[A]]] = {
    processingPipelines
      .flatMap(pipeline =>
        pipeline
          .inputs
          .filter(pdk => pdk.page == RawDataKeys.Pages.RawData)
      )
      .distinct
      .map(pipelineDataKey =>
        PipelineNode(new RawDataLoadPipeline[A](sparkSession, giantDataLoader, pipelineDataKey), Set(), Set(pipelineDataKey))
      )
  }

  private def getInputsAndOutputs(pipelineConfig: A, pipeline: ComputationPipeline[A]): (Set[PipelineDataKey], Set[PipelineDataKey]) = {
    val dataRecorder = new RecordingPipelineData()
    val output = pipeline.generateData(pipelineConfig, dataRecorder).data.keySet
    val input: Set[PipelineDataKey] = dataRecorder.wantedInputs.toSet

    (input, output)
  }

}

object PipelineExecutor {

  private val Lock = new Object

  sealed trait ExecutionMode

  case object Parallel extends ExecutionMode

  case object Serial extends ExecutionMode

}


private case class PipelineComponentWithStorageInfo(pipelineComponent: PipelineComponent, pipelineDataStorageInfo: PipelineDataStorageInfo)

private class RecordingPipelineData extends PipelineData(Map()) {
  val wantedInputs = mutable.Set[PipelineDataKey]()

  override def get(task: String, page: PageName, date: LocalDate): PipelineComponent = {
    wantedInputs.add(PipelineDataKey(task, page, date))
    //this will be totally fine if the pipeline is lazy like it should be
    DataFrameComponent.anonymous(null)
  }


  override def get(pipelineDataKey: PipelineDataKey): PipelineComponent = {
    wantedInputs.add(pipelineDataKey)
    //this will be totally fine if the pipeline is lazy like it should be
    DataFrameComponent.anonymous(null)
  }

  override def ++(other: PipelineData): PipelineData = this
}
