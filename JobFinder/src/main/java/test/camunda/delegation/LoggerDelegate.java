package test.camunda.delegation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;

/**
 * This is an easy adapter implementation illustrating how a Java Delegate can
 * be used from within a BPMN 2.0 Service Task.
 */
public class LoggerDelegate implements JavaDelegate, TaskListener {

  private final Logger LOGGER = LoggerFactory.getLogger(LoggerDelegate.class.getName());

  @Override
  public void execute(DelegateExecution execution) throws Exception {

    final ManagementService managementService = execution.getProcessEngineServices().getManagementService();

    LOGGER.info("\n\nLoggerDelegate::execute invoked by " + "processDefinitionId=" + execution.getProcessDefinitionId()
        + ", activtyId=" + execution.getCurrentActivityId() + ", activtyName='" + execution.getCurrentActivityName()
        + "'" + ", processInstanceId=" + execution.getProcessInstanceId() + ", businessKey="
        + execution.getProcessBusinessKey() + ", executionId=" + execution.getId());

    LOGGER.info("How many jobs are there for current process instance? "
        + managementService.createJobQuery().processInstanceId(execution.getProcessInstanceId()).count());
    LOGGER.info("How many jobs are there in total? " + managementService.createJobQuery().count());
    for (var job : managementService.createJobQuery().list()) {
      LOGGER.info("\nJob Details: " + job.toString());
    }
  }

  @Override
  public void notify(DelegateTask delegateTask) {
    final DelegateExecution execution = delegateTask.getExecution();
    final ManagementService managementService = execution.getProcessEngineServices().getManagementService();
    
    LOGGER.info("\n\nLoggerDelegate::notify invoked by " + "processDefinitionId=" + execution.getProcessDefinitionId()
        + ", activtyId=" + execution.getCurrentActivityId() + ", activtyName='" + execution.getCurrentActivityName()
        + "'" + ", processInstanceId=" + execution.getProcessInstanceId() + ", businessKey="
        + execution.getProcessBusinessKey() + ", executionId=" + execution.getId());

    LOGGER.info("Current Event: " + delegateTask.getEventName());
    LOGGER.info("How many jobs are there for current process instance? "
        + managementService.createJobQuery().processInstanceId(execution.getProcessInstanceId()).count());
    LOGGER.info("How many jobs are there in total? " + managementService.createJobQuery().count());
    for (var job : managementService.createJobQuery().list()) {
      LOGGER.info("\nJob Details: " + job.toString());
    }
  }

}
