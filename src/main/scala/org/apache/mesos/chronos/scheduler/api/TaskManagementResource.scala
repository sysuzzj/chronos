package org.apache.mesos.chronos.scheduler.api

import java.util.logging.{Level, Logger}
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import javax.ws.rs.{DELETE, PUT, Path, PathParam, Produces, WebApplicationException}

import org.apache.mesos.chronos.scheduler.config.SchedulerConfiguration
import org.apache.mesos.chronos.scheduler.graph.JobGraph
import org.apache.mesos.chronos.scheduler.jobs._
import org.apache.mesos.chronos.scheduler.state.PersistenceStore
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Inject
import org.apache.mesos.Protos.{TaskID, TaskState, TaskStatus}

import scala.beans.BeanProperty

/**
 * The REST API for managing tasks such as updating the status of an asynchronous task.
 * @author Florian Leibert (flo@leibert.de)
 */
//TODO(FL): Create a case class that removes epsilon from the dependent.
@Path(PathConstants.taskBasePath)
@Produces(Array("application/json"))
class TaskManagementResource @Inject()(
                                        val persistenceStore: PersistenceStore,
                                        val jobScheduler: JobScheduler,
                                        val jobGraph: JobGraph,
                                        val taskManager: TaskManager,
                                        val configuration: SchedulerConfiguration) {

  private[this] val log = Logger.getLogger(getClass.getName)

  @DELETE
  @Path(PathConstants.killTaskPattern)
  def killTasksForJob(@PathParam("jobName") jobName: String): Response = {
    log.info("Task purge request received")
    try {
      require(jobGraph.lookupVertex(jobName).isDefined, "JobSchedule '%s' not found".format(jobName))
      val job = jobGraph.getJobForName(jobName).get
      taskManager.cancelMesosTasks(job)
      Response.noContent().build()
    } catch {
      case ex: Exception =>
        log.log(Level.WARNING, "Exception while serving request", ex)
        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR)
    }
  }
}

case class TaskNotification(@JsonProperty @BeanProperty statusCode: Int)
