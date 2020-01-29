# Camunda Delegation Test
## Finding active timer Job instance associated with a user task in a given process instance

In the given  simple process ![](process.png) there is a timer which monitors the user task with a deadline.

The attempt is to acquire the timer Job instance programmatically so that it can be suspended/resumed at will.

In order to achieve that, a combined JavaDelegate and TaskListner class has been added to User Task "Random User Task". **Unfortunately** though, it cannot find any job instances. The delegate class has been attached to *start* execusion listner for the user task as well as to *create* and *assignment* events as task listner.

The example log extracts are:
```
INFORMATION:

LoggerDelegate::execute invoked by processDefinitionId=JobFinder:1:3, activtyId=randomUserTask, activtyName='Random User Task', processInstanceId=5, businessKey=null, executionId=7
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate execute
INFORMATION: How many jobs are there for current process instance? 0
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate execute
INFORMATION: How many jobs are there in total? 0
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:

LoggerDelegate::notify invoked by processDefinitionId=JobFinder:1:3, activtyId=randomUserTask, activtyName='Random User Task', processInstanceId=5, businessKey=null, executionId=7
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: Current Event: create
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there for current process instance? 0
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there in total? 0
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:

LoggerDelegate::notify invoked by processDefinitionId=JobFinder:1:3, activtyId=randomUserTask, activtyName='Random User Task', processInstanceId=5, businessKey=null, executionId=7
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: Current Event: assignment
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there for current process instance? 0
Jan. 24, 2020 3:14:50 NACHM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there in total? 0
```

On the other hand, the process can be driven through a test JUnit test driver application. There using the management service instance acquired through process engine object can indeed display the job.

```
===========================================================
Job Count: 1

Job Details: TimerEntity[repeat=null, id=9, revision=1, duedate=Sat Jan 25 15:14:50 CET 2020, repeatOffset=0, lockOwner=null, lockExpirationTime=null, executionId=7, processInstanceId=5, isExclusive=true, retries=3, jobHandlerType=timer-transition, jobHandlerConfiguration=timerTaskTerminator, exceptionByteArray=null, exceptionByteArrayId=null, exceptionMessage=null, deploymentId=1]
===========================================================
```

I'm trying to solve this mystery.

## Update 29.01.2020 - Experiment with multiple simultaneous process instances

The mystery has been solved. As of 29.01.2020 and Camunda BPM version 7.13.0-SNAPSHOT there exists a bug in JobQuery API in management service - Java SDK. The details are as follows.

### Test setup
The experiment was simple but revealed the crack in the implementation. Test was run through both test driver as well as deploying the process application on Camunda Docker container. Camunda version used is 7.13.0-SNAPSHOT.

Instead of starting one process instance, 3 process instances were started one after another and were kept open simultaneously. Process was simplified to keep the LoggerDelegate attached only to 'CREATE' event of the 'Random User Task' and all other attachments were removed in order to keep the logs readable. Also, job query in the unit test driver was removed.

### Test results
The results reveal curious behaviour of Java Management (JobQuery) API.

The [test driver log](Test-Results/JobFinder%20-%20Test%20Driver%20Log.txt) consists database log as well as `LoggerDelegate` log statements. Similar to previous behaviour the log shows 0 jobs for process ID and 0 total jobs after starting first instance.

```
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:

LoggerDelegate::notify invoked by processDefinitionId=JobFinder:1:3, activtyId=randomUserTask, activtyName='Random User Task', processInstanceId=5, businessKey=null, executionId=7
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: Current Event: create
11:19:39.696 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13005 Starting command -------------------- JobQueryImpl ----------------------
11:19:39.697 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13010 reusing existing command context
11:19:39.709 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==>  Preparing: select count(distinct RES.ID_ ) from ACT_RU_JOB RES WHERE RES.PROCESS_INSTANCE_ID_ = ?
11:19:39.710 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==> Parameters: 5(String)
11:19:39.713 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - <==      Total: 1
11:19:39.713 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13006 Finishing command -------------------- JobQueryImpl ----------------------
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there for current process instance? 0
11:19:39.717 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13005 Starting command -------------------- JobQueryImpl ----------------------
11:19:39.717 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13010 reusing existing command context
11:19:39.719 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==>  Preparing: select count(distinct RES.ID_ ) from ACT_RU_JOB RES
11:19:39.720 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==> Parameters:
11:19:39.720 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - <==      Total: 1
11:19:39.721 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13006 Finishing command -------------------- JobQueryImpl ----------------------
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there in total? 0
11:19:39.723 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13005 Starting command -------------------- JobQueryImpl ----------------------
11:19:39.723 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13010 reusing existing command context
11:19:39.725 [main] DEBUG o.c.b.e.i.p.e.J.selectJobByQueryCriteria - ==>  Preparing: select distinct RES.* from ACT_RU_JOB RES order by RES.ID_ asc LIMIT ? OFFSET ?
11:19:39.727 [main] DEBUG o.c.b.e.i.p.e.J.selectJobByQueryCriteria - ==> Parameters: 2147483647(Integer), 0(Integer)
11:19:39.727 [main] DEBUG o.c.b.e.i.p.e.J.selectJobByQueryCriteria - <==      Total: 0
11:19:39.728 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13006 Finishing command -------------------- JobQueryImpl ----------------------
11:19:39.730 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13011 closing existing command context
```
Only notable thing here is the process instance ID `5` of the process 'JobFinder'. The reason will be explained shortly.

After starting the second instance, things got interesting. This time the total jobs for the process instance were still 0 but total jobs in general were being shown as 1.
```
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:

LoggerDelegate::notify invoked by processDefinitionId=JobFinder:1:3, activtyId=randomUserTask, activtyName='Random User Task', processInstanceId=15, businessKey=null, executionId=17
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: Current Event: create
11:19:39.859 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13005 Starting command -------------------- JobQueryImpl ----------------------
11:19:39.859 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13010 reusing existing command context
11:19:39.861 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==>  Preparing: select count(distinct RES.ID_ ) from ACT_RU_JOB RES WHERE RES.PROCESS_INSTANCE_ID_ = ?
11:19:39.864 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==> Parameters: 15(String)
11:19:39.865 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - <==      Total: 1
11:19:39.866 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13006 Finishing command -------------------- JobQueryImpl ----------------------
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there for current process instance? 0
11:19:39.867 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13005 Starting command -------------------- JobQueryImpl ----------------------
11:19:39.867 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13010 reusing existing command context
11:19:39.869 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==>  Preparing: select count(distinct RES.ID_ ) from ACT_RU_JOB RES
11:19:39.870 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - ==> Parameters:
11:19:39.872 [main] DEBUG o.c.b.e.i.p.e.J.selectJobCountByQueryCriteria - <==      Total: 1
11:19:39.872 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13006 Finishing command -------------------- JobQueryImpl ----------------------
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: How many jobs are there in total? 1
11:19:39.874 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13005 Starting command -------------------- JobQueryImpl ----------------------
11:19:39.874 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13010 reusing existing command context
11:19:39.877 [main] DEBUG o.c.b.e.i.p.e.J.selectJobByQueryCriteria - ==>  Preparing: select distinct RES.* from ACT_RU_JOB RES order by RES.ID_ asc LIMIT ? OFFSET ?
11:19:39.878 [main] DEBUG o.c.b.e.i.p.e.J.selectJobByQueryCriteria - ==> Parameters: 2147483647(Integer), 0(Integer)
11:19:39.881 [main] DEBUG o.c.b.e.i.p.e.J.selectJobByQueryCriteria - <==      Total: 1
11:19:39.881 [main] DEBUG org.camunda.bpm.engine.cmd - ENGINE-13006 Finishing command -------------------- JobQueryImpl ----------------------
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:
Job Details: TimerEntity[repeat=null, id=9, revision=1, duedate=Thu Jan 30 11:19:39 CET 2020, repeatOffset=0, lockOwner=null, lockExpirationTime=null, executionId=7, processInstanceId=5, isExclusive=true, retries=3, jobHandlerType=timer-transition, jobHandlerConfiguration=timerTaskTerminator, exceptionByteArray=null, exceptionByteArrayId=null, exceptionMessage=null, deploymentId=1]
``` 
Now, all attention must be paid. Here, the querying process instance ID is `15` (see in the very beginning of the excerpt, `processInstanceId=15`) but the job details for the `TimerEntity` found under all jobs belonged to process instance ID `5` (in the end) which belonged to the previous process instance.

This behaviour continuous itself after starting the third instance and log shows `2` jobs in total and no jobs for current process instance.
```
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:

LoggerDelegate::notify invoked by processDefinitionId=JobFinder:1:3, activtyId=randomUserTask, activtyName='Random User Task', processInstanceId=25, businessKey=null, executionId=27
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION: Current Event: create
...
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:
Job Details: TimerEntity[repeat=null, id=19, revision=1, duedate=Thu Jan 30 11:19:39 CET 2020, repeatOffset=0, lockOwner=null, lockExpirationTime=null, executionId=17, processInstanceId=15, isExclusive=true, retries=3, jobHandlerType=timer-transition, jobHandlerConfiguration=timerTaskTerminator, exceptionByteArray=null, exceptionByteArrayId=null, exceptionMessage=null, deploymentId=1]
Jan. 29, 2020 11:19:39 VORM. test.camunda.delegation.LoggerDelegate notify
INFORMATION:
Job Details: TimerEntity[repeat=null, id=9, revision=1, duedate=Thu Jan 30 11:19:39 CET 2020, repeatOffset=0, lockOwner=null, lockExpirationTime=null, executionId=7, processInstanceId=5, isExclusive=true, retries=3, jobHandlerType=timer-transition, jobHandlerConfiguration=timerTaskTerminator, exceptionByteArray=null, exceptionByteArrayId=null, exceptionMessage=null, deploymentId=1]
```
Notice again, that both the timer jobs listed, belong to previous process instances. The deployed process on Camunda Docker also exibits the same behaviour. Here, more nuanced analysis can be done using REST API calls. While `LoggerDelegate` logs are behaving weirdly, REST APIs find the right job for right process instance right from beginning. (The logs can be found in directory [Test-Results](Test-Results).) This shows that there is a descrepancy within Camunda REST API and Java API for Job Query.

A possible hypothesis might be a bug in the `SELECT` query for the jobs. Had it been something related to transactions, i.e. `TimerEntity` Job transaction not being committed unless another process instance is started, REST API would also not have shown it. But given the fact the REST-API can show it correct every time, somehting deep in `JobQuery` interface implementation is fishy.

## Update later on 29.01.2020: This is NOT a bug!
It is an inherent limitation of the design whereby a delegate cannot see the job the process as it is being called in a same transaction where the job was created and not yet committed.

See full discussion and response from a Camunda develeper [here](https://forum.camunda.org/t/java-api-bug-in-jobquery-implementation-in-management-service-api/17662).
Also, see [workaround solution](https://forum.camunda.org/t/lookup-timer-due-date-for-a-boundary-event-from-task-create-listener/6155/2) for this scenario. The same has been given here for convenience while thanking Philipp Ossler.
```
public class MyTaskListener implements TaskListener
{
    public void notify(final DelegateTask delegateTask)
    {
        TransactionContext transactionContext = Context.getCommandContext().getTransactionContext();
        transactionContext.addTransactionListener(TransactionState.COMMITTED, new TransactionListener()
        {
            public void execute(CommandContext commandContext)
            {
                ManagementService managementService = delegateTask.getProcessEngineServices().getManagementService();

                List<Job> timers = managementService.createJobQuery().timers().list();

                for (Job timer : timers)
                {
                    Date duedate = timer.getDuedate();
                    // ...
                }

            }
        });
    }
}
```
