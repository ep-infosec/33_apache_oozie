

[::Go back to Oozie Documentation Index::](index.html)

# Command Line Interface Utilities

<!-- MACRO{toc|fromDepth=1|toDepth=4} -->

## Introduction

Oozie provides a command line utility, `oozie`, to perform job and admin tasks. All operations are done via
sub-commands of the `oozie` CLI.

The `oozie` CLI interacts with Oozie via its WS API.

## Oozie Command Line Usage


```
usage:
      the env variable 'OOZIE_URL' is used as default value for the '-oozie' option
      the env variable 'OOZIE_TIMEZONE' is used as default value for the '-timezone' option
      the env variable 'OOZIE_AUTH' is used as default value for the '-auth' option
      custom headers for Oozie web services can be specified using '-Dheader:NAME=VALUE'
```

### Oozie basic commands

```

oozie help      : display usage

oozie version   : show client version

```

### Oozie job operation commands

```
oozie job <OPTIONS>           : job operations
          -action <arg>         coordinator rerun/kill on action ids (requires -rerun/-kill);
                                coordinator log retrieval on action ids (requires -log)
          -allruns              Get workflow jobs corresponding to a coordinator action
                                including all the reruns
          -auditlog <arg>       job audit log
          -auth <arg>           select authentication type [SIMPLE|BASIC|KERBEROS]
          -change <arg>         change a coordinator or bundle job
          -config <arg>         job configuration file '.xml' or '.properties'
          -configcontent <arg>  job configuration
          -coordinator <arg>    bundle rerun on coordinator names (requires -rerun)
          -D <property=value>   set/override value for given property
          -date <arg>           coordinator/bundle rerun on action dates (requires -rerun);
                                coordinator log retrieval on action dates (requires -log)
          -debug                Use debug mode to see debugging statements on stdout
          -definition <arg>     job definition
          -diff <arg>           Show diff of the new coord definition and properties with the
                                existing one (default true)
          -doas <arg>           doAs user, impersonates as the specified user
          -dryrun               Dryrun a workflow (since 3.3.2), a coordinator (since 2.0)
                                or a bundle (since 5.1) job without actually executing it
          -errorlog <arg>       job error log
          -failed               runs the failed workflow actions of the coordinator actions
                                (requires -rerun)
          -filter <arg>         <key><comparator><value>[;<key><comparator><value>]*
                                (All Coordinator actions satisfying the filters will be
                                retrieved).
                                key: status or nominaltime
                                comparator: =, !=, <, <=, >, >=. = is used as OR and others
                                as AND
                                status: values are valid status like SUCCEEDED, KILLED etc.
                                Only = and != apply for status
                                nominaltime: time of format yyyy-MM-dd'T'HH:mm'Z'
          -ignore <arg>         change status of a coordinator job or action to IGNORED
                                (-action required to ignore coord actions)
          -info <arg>           info of a job
          -insecure             This option will allow SSL connections even though there's a problem with the
                                certificate. The connection will still be encrypted, but Oozie client won't validate
                                the server certificate.
          -interval <arg>       polling interval in minutes (default is 5, requires -poll)
          -kill <arg>           kill a job (coordinator can mention -action or -date)
          -len <arg>            number of actions (default TOTAL ACTIONS, requires -info)
          -localtime            use local time (same as passing your time zone to -timezone).
                                Overrides -timezone option
          -log <arg>            job log
          -logfilter <arg>      job log search parameter. Can be specified as -logfilter
                                opt1=val1;opt2=val1;opt3=val1. Supported options are recent,
                                start, end, loglevel, text, limit and debug
          -missingdeps <arg>    List missing dependencies of a coord action. To specify
                                multiple actions, use with -action or -date option.
          -nocleanup            do not clean up output-events of the coordinator rerun
                                actions (requires -rerun)
          -offset <arg>         job info offset of actions (default '1', requires -info)
          -oozie <arg>          Oozie URL
          -order <arg>          order to show coord actions (default ascending order, 'desc'
                                for descending order, requires -info)
          -password <arg>       password for BASIC authentication
          -poll <arg>           poll Oozie until a job reaches a terminal state or a timeout
                                occurs
          -refresh              re-materialize the coordinator rerun actions (requires
                                -rerun)
          -rerun <arg>          rerun a job  (coordinator requires -action or -date, bundle
                                requires -coordinator or -date)
          -resume <arg>         resume a job
          -retries <arg>        Get information of the retry attempts for a given workflow
                                action
          -run                  run a job
          -runjar <arg>         generate and run job definition
          -slachange <arg>      Update sla param for jobs, supported param are should-start,
                                should-end, nominal-time and max-duration
          -sladisable <arg>     disables sla alerts for the job and its children
          -slaenable <arg>      enables sla alerts for the job and its children
          -start <arg>          start a job
          -submit               submit a job
          -submitjar <arg>      generate and submit job definition
          -suspend <arg>        suspend a job
          -timeout <arg>        timeout in minutes (default is 30, negative values indicate
                                no timeout, requires -poll)
          -timezone <arg>       use time zone with the specified ID (default GMT).
                                See 'oozie info -timezones' for a list
          -update <arg>         Update coord definition and properties
          -username <arg>       username for BASIC authentication
          -validatejar <arg>    generate and check job definition
          -value <arg>          new endtime/concurrency/pausetime value for changing a
                                coordinator job
          -verbose              verbose mode
```

### Oozie jobs operation commands

```
oozie jobs <OPTIONS>          : jobs status
           -auth <arg>          select authentication type [SIMPLE|BASIC|KERBEROS]
           -bulk <arg>          key-value pairs to filter bulk jobs response. e.g.
                                bundle=<B>\;coordinators=<C>\;actionstatus=<S>\;startcreatedtime=
                                <SC>\;endcreatedtime=<EC>\;startscheduledtime=<SS>\;endscheduledt
                                ime=<ES>\; bundle, coordinators and actionstatus can be multiple
                                comma separated values. Bundle and coordinators can be id(s) or
                                appName(s) of those jobs. Specifying bundle is mandatory, other
                                params are optional
           -doas <arg>          doAs user, impersonates as the specified user
           -filter <arg>
                                text=<*>\;user=<U>\;name=<N>\;group=<G>\;status=<S>\;frequency=<F
                                >\;unit=<M>\;startcreatedtime=<SC>\;endcreatedtime=<EC>
                                \;sortBy=<SB>
                                (text filter: matches partially with name and user or complete
                                match with job ID. Valid unit values are 'months', 'days',
                                'hours' or 'minutes'. startcreatedtime, endcreatedtime: time of
                                format yyyy-MM-dd'T'HH:mm'Z'. Valid values for sortBy are
                                'createdTime' or 'lastModifiedTime'.)
           -insecure            This option will allow SSL connections even though there's a problem with the
                                certificate. The connection will still be encrypted, but Oozie client won't validate
                                the server certificate.
           -jobtype <arg>       job type ('Supported in Oozie-2.0 or later versions ONLY -
                                'coordinator' or 'bundle' or 'wf'(default))
           -kill                bulk kill operation
           -len <arg>           number of jobs (default '100')
           -localtime           use local time (same as passing your time zone to -timezone).
                                Overrides -timezone option
           -offset <arg>        jobs offset (default '1')
           -oozie <arg>         Oozie URL
           -password <arg>      password for BASIC authentication
           -resume              bulk resume operation
           -suspend             bulk suspend operation
           -timezone <arg>      use time zone with the specified ID (default GMT).
                                See 'oozie info -timezones' for a list
           -username <arg>      username for BASIC authentication
           -verbose             verbose mode
```

### Oozie admin operation commands

```
oozie admin <OPTIONS>         : admin operations
            -auth <arg>         select authentication type [SIMPLE|BASIC|KERBEROS]
            -configuration      show Oozie system configuration
            -doas <arg>         doAs user, impersonates as the specified user
            -insecure           This option will allow SSL connections even though there's a problem with the
                                certificate. The connection will still be encrypted, but Oozie client won't validate
                                the server certificate.
            -instrumentation    show Oozie system instrumentation
            -javasysprops       show Oozie Java system properties
            -metrics            show Oozie system metrics
            -oozie <arg>        Oozie URL
            -osenv              show Oozie system OS environment
            -password <arg>     password for BASIC authentication
            -purge <arg>        purge old oozie workflow, coordinator and bundle records from
                                DB (parameter unit: day)
            -queuedump          show Oozie server queue elements
            -servers            list available Oozie servers (more than one only if HA is
                                enabled)
            -shareliblist       List available sharelib that can be specified in a workflow
                                action
            -sharelibupdate     Update server to use a newer version of sharelib
            -status             show the current system status
            -systemmode <arg>   Supported in Oozie-2.0 or later versions ONLY. Change oozie
                                system mode [NORMAL|NOWEBSERVICE|SAFEMODE]
            -username <arg>     username for BASIC authentication
            -version            show Oozie server build version
```

### Oozie validate command

```
oozie validate <OPTIONS> <ARGS> : validate a workflow, coordinator, bundle XML file
               -auth <arg>       select authentication type [SIMPLE|BASIC|KERBEROS]
               -insecure         This option will allow SSL connections even though there's a problem with the
                                 certificate. The connection will still be encrypted, but Oozie client won't validate
                                 the server certificate.
               -oozie <arg>      Oozie URL
               -password <arg>   password for BASIC authentication
               -username <arg>   username for BASIC authentication
```

### Oozie SLA operation commands

```
oozie sla <OPTIONS>           : sla operations (Deprecated with Oozie 4.0)
          -auth <arg>           select authentication type [SIMPLE|BASIC|KERBEROS]
          -filter <arg>         filter of SLA events. e.g., jobid=<J>\;appname=<A>
          -len <arg>            number of results (default '100', max '1000')
          -offset <arg>         start offset (default '0')
          -oozie <arg>          Oozie URL
          -filter <arg>         jobid=<JobID/ActionID>\;appname=<Application Name>
          -insecure             This option will allow SSL connections even though there's a problem with the
                                certificate. The connection will still be encrypted, but Oozie client won't validate
                                the server certificate.
          -password <arg>       password for BASIC authentication
          -username <arg>       username for BASIC authentication
```

### Oozie Pig submit command

```
oozie pig <OPTIONS> -X <ARGS> : submit a pig job, everything after '-X' are pass-through parameters to pig, any '-D' arguments
                                after '-X' are put in <configuration>
          -auth <arg>           select authentication type [SIMPLE|BASIC|KERBEROS]
          -config <arg>         job configuration file '.properties'
          -D <property=value>   set/override value for given property
          -file <arg>           Pig script
          -insecure             This option will allow SSL connections even though there's a problem with the
                                certificate. The connection will still be encrypted, but Oozie client won't validate
                                the server certificate.
          -oozie <arg>          Oozie URL
          -P <property=value>   set parameters for script
          -password <arg>       password for BASIC authentication
          -username <arg>       username for BASIC authentication
```

### Oozie Hive submit command

```
oozie hive <OPTIONS> -X<ARGS>  : submit a hive job, everything after '-X' are pass-through parameters to hive,
                                 any '-D' arguments after '-X' are put in <configuration>
           -auth <arg>           select authentication type [SIMPLE|BASIC|KERBEROS]
           -config <arg>         job configuration file '.properties'
           -D <property=value>   set/override value for given property
           -doas <arg>           doAs user, impersonates as the specified user
           -file <arg>           hive script
           -insecure             This option will allow SSL connections even though there's a problem with the
                                 certificate. The connection will still be encrypted, but Oozie client won't validate
                                 the server certificate.
           -oozie <arg>          Oozie URL
           -P <property=value>   set parameters for script
           -password <arg>       password for BASIC authentication
           -username <arg>       username for BASIC authentication
```

### Oozie Sqoop submit command

```
oozie sqoop <OPTIONS> -X<ARGS> : submit a sqoop job, any '-D' arguments after '-X' are put in <configuration>
           -auth <arg>           select authentication type [SIMPLE|BASIC|KERBEROS]
           -config <arg>         job configuration file '.properties'
           -D <property=value>   set/override value for given property
           -doas <arg>           doAs user, impersonates as the specified user
           -insecure             This option will allow SSL connections even though there's a problem with the
                                 certificate. The connection will still be encrypted, but Oozie client won't validate
                                 the server certificate.
           -command <arg>        sqoop command
           -oozie <arg>          Oozie URL
           -password <arg>       password for BASIC authentication
           -username <arg>       username for BASIC authentication
```

### Oozie info command

```
oozie info <OPTIONS>           : get more detailed info about specific topics
          -timezones             display a list of available time zones
```

### Oozie MapReduce job command

```
oozie mapreduce <OPTIONS>           : submit a mapreduce job
                -auth <arg>           select authentication type [SIMPLE|BASIC|KERBEROS]
                -config <arg>         job configuration file '.properties'
                -D <property=value>   set/override value for given property
                -doas <arg>           doAs user, impersonates as the specified user
                -insecure             This option will allow SSL connections even though there's a problem with the
                                      certificate. The connection will still be encrypted, but Oozie client won't validate
                                      the server certificate.
                -oozie <arg>          Oozie URL
                -password <arg>       password for BASIC authentication
                -username <arg>       username for BASIC authentication
```

## Common CLI Options

### Authentication

The `oozie` CLI automatically perform authentication if the Oozie server requests it. By default it supports both
pseudo/simple authentication and Kerberos HTTP SPNEGO authentication.

To perform a specific authentication, the `auth` option with authentication type requests Oozie client to run the
specified authentication mechanism only. Oozie client provides three types `simple`, `basic` and `kerberos` which
supports `pseudo/simple` and `Kerberos`. Basic authentication can be used, when the server sits behind a proxy
which accepts basic authentication, and use Kerberos to authorize to Oozie itself.

For pseudo/simple authentication the `oozie` CLI uses the user name of the current OS user.

For Kerberos HTTP SPNEGO authentication the `oozie` CLI uses the default principal for the OS Kerberos cache
(normally the principal that did `kinit`).

Oozie uses Apache Hadoop-Auth (Java HTTP SPNEGO) library for authentication.
This library can be extended to support other authentication mechanisms.

Once authentication is performed successfully the received authentication token is cached in the user home directory
in the `.oozie-auth-token` file with owner-only permissions. Subsequent requests reuse the cached token while valid.

The use of the cache file can be disabled by invoking the `oozie` CLI with the `-Doozie.auth.token.cache`=false
option.

To use an custom authentication mechanism, a Hadoop-Auth `Authenticator` implementation must be specified with the
 `-Dauthenticator.class=CLASS` option.

### Impersonation, doAs

The `-doas` option allows the current user to impersonate other users when interacting with the Oozie
system. The current user must be configured as a proxyuser in the Oozie system. The proxyuser configuration may
restrict from which hosts a user may impersonate users, as well as users of which groups can be impersonated.

### Oozie URL

All `oozie` CLI sub-commands expect the `-oozie OOZIE_URL` option indicating the URL of the Oozie system
to run the command against. If the OOZIE_URL environment variable has not been set, `oozie` will use the default
URL specified in oozie-client-env.sh (equivalent to `!http://$(hostname -f):11000/oozie`).

If the `-oozie` option is not specified, the `oozie` CLI will look for the `OOZIE_URL` environment variable
and uses it if set.

If the option is not provided and the environment variable is not set, the `oozie` CLI will fail.

### Insecure connection

In case `-insecure` option is used then it will cause Oozie to allow certificate errors where the data is still encrypted,
but the client does not check the certificate.

If the `-insecure` option is not specified and SSL is enabled, the user needs to pass the `-Djavax.net.ssl.trustStore`
and `-Djavax.net.ssl.trustStorePassword` system properties to the Oozie client or the certificate needs to be imported
into the JDK's cert store otherwise Oozie client won't be able to connect to the Oozie server.

### Time zone

The `-timezone TIME_ZONE_ID` option in the `job` and `jobs` sub-commands allows you to specify the time zone to use in
the output of those sub-commands. The `TIME_ZONE_ID` should be one of the standard Java Time Zone IDs.  You can get a
list of the available time zones with the command `oozie info -timezones`.

If the `-localtime` option is used, it will cause Oozie to use whatever the time zone is of the machine. If
both `-localtime` and `-timezone TIME_ZONE_ID` are used, the `-localtime` option will override
the `-timezone TIME_ZONE_ID` option.  If neither option is given, Oozie will look for the `OOZIE_TIMEZONE` environment
variable and uses it if set.  If neither option is given and the environment variable is not set, or if Oozie is given an invalid
time zone, it will use GMT.

### Debug Mode

If you export `OOZIE_DEBUG=1` then the Oozie CLI will output the Web Services API details used by any commands you
execute. This is useful for debugging purposes to or see how the Oozie CLI works with the WS API.

### CLI retry
Oozie CLI retries connection to Oozie servers for transparent high availability failover when one of the Oozie servers go down.
`Oozie` CLI command will retry for all commands in case of ConnectException.
In case of SocketException, all commands except `PUT` and `POST` will have retry logic.
All job submit are POST call, examples of PUT and POST commands can be find out from [WebServicesAPI](WebServicesAPI.html).
Retry count can be configured with system property `oozie.connection.retry.count`. Default count is 4.


## Job Operations

### Submitting a Workflow, Coordinator or Bundle Job

* Submitting bundle feature is only supported in Oozie 3.0 or later. Similarly, all bundle operation features below are only
supported in Oozie 3.0 or later.

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -config job.properties -submit
.
job: 14-20090525161321-oozie-joe
```

The parameters for the job must be provided in a file, either a Java Properties file (.properties) or a Hadoop XML
Configuration file (.xml). This file must be specified with the `-config` option.

The workflow application path must be specified in the file with the `oozie.wf.application.path` property.  The
coordinator application path must be specified in the file with the `oozie.coord.application.path` property.The
bundle application path must be specified in the file with the `oozie.bundle.application.path` property.
Specified path must be an HDFS path.

The job will be created, but it will not be started, it will be in `PREP` status.

### Starting a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -start 14-20090525161321-oozie-joe
```

The `start` option starts a previously submitted workflow job or bundle job that is in `PREP` status.

After the command is executed the workflow job will be in `RUNNING` status and bundle job will be in `RUNNING`status.

A coordinator job does not support the `start` action. It will show the following error message when trying to start it
via the CLI:


```
Error: E0303 : E0303: Invalid parameter value, [action] = [start]
```

### Running a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -config job.properties -run
.
job: 15-20090525161321-oozie-joe
```

The `run` option creates and starts a workflow job, coordinator job or bundle job.

The parameters for the job must be provided in a file, either a Java Properties file (.properties) or a Hadoop XML
Configuration file (.xml). This file must be specified with the `-config` option.

The workflow application path must be specified in the file with the `oozie.wf.application.path` property. The
coordinator application path must be specified in the file with the `oozie.coord.application.path` property. The
bundle application path must be specified in the file with the `oozie.bundle.application.path` property.The
specified path must be an HDFS path.

The job will be created and it will started, the job will be in `RUNNING` status.

### Suspending a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -suspend 14-20090525161321-oozie-joe
```

The `suspend` option suspends a workflow job in `RUNNING` status.
After the command is executed the workflow job will be in `SUSPENDED` status.

The `suspend` option suspends a coordinator/bundle  job in `RUNNING`, `RUNNINGWITHERROR` or `PREP` status.
When the coordinator job is suspended, running coordinator actions will stay in running and the workflows will be suspended. If the coordinator job is in `RUNNING`status, it will transit to `SUSPENDED`status; if it is in `RUNNINGWITHERROR`status, it will transit to `SUSPENDEDWITHERROR`; if it is in `PREP`status, it will transit to `PREPSUSPENDED`status.

When the bundle job is suspended, running coordinators will be suspended. If the bundle job is in `RUNNING`status, it will transit to `SUSPENDED`status; if it is in `RUNNINGWITHERROR`status, it will transit to `SUSPENDEDWITHERROR`; if it is in `PREP`status, it will transit to `PREPSUSPENDED`status.

### Resuming a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -resume 14-20090525161321-oozie-joe
```

The `resume` option resumes a workflow job in `SUSPENDED` status.

After the command is executed the workflow job will be in `RUNNING` status.


The `suspend` option suspends a coordinator/bundle job in `SUSPENDED`, `SUSPENDEDWITHERROR` or `PREPSUSPENDED` status.
If the coordinator job is in `SUSPENDED`status, it will transit to `RUNNING`status; if it is in `SUSPENDEDWITHERROR`status, it will transit to `RUNNINGWITHERROR`; if it is in `PREPSUSPENDED`status, it will transit to `PREP`status.

When the coordinator job is resumed it will create all the coordinator actions that should have been created during the time
it was suspended, actions will not be lost, they will delayed.

When the bundle job is resumed, suspended coordinators will resume running. If the bundle job is in `SUSPENDED`status, it will transit to `RUNNING`status; if it is in `SUSPENDEDWITHERROR`status, it will transit to `RUNNINGWITHERROR`; if it is in `PREPSUSPENDED`status, it will transit to `PREP`status.


### Killing a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -kill 14-20090525161321-oozie-joe
```

The `kill` option kills a workflow job in `PREP`, `SUSPENDED` or `RUNNING` status and a coordinator/bundle job in
`PREP`, `RUNNING`, `PREPSUSPENDED`, `SUSPENDED`, `PREPPAUSED`, or `PAUSED`  status.

After the command is executed the job will be in `KILLED` status.

### Killing a Coordinator Action or Multiple Actions

Example:


```
$oozie job -kill <coord_Job_id> [-action 1, 3-4, 7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z, 2009-11-10T01:00Z, 2009-12-31T22:00Z]
```

   * The `kill` option here for a range of coordinator actions kills a non-terminal (`RUNNING`, `WAITING`, `READY`, `SUSPENDED`) coordinator action when coordinator job is not in `FAILED` or `KILLED` state.
   * Either -action or -date should be given.
   * If neither -action nor -date is given, the exception will be thrown. Also if BOTH -action and -date are given, an error will be thrown.
   * Multiple ranges can be used in -action or -date. See the above example.
   * If one of the actions in the given list of -action is already in terminal state, the output of this command will only include the other actions.
   * The dates specified in -date must be UTC.
   * Single date specified in -date must be able to find an action with matched nominal time to be effective.
   * After the command is executed the killed coordinator action will have `KILLED` status.

### Changing endtime/concurrency/pausetime/status of a Coordinator Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -change 14-20090525161321-oozie-joe -value endtime=2011-12-01T05:00Z\;concurrency=100\;2011-10-01T05:00Z
$ oozie job -oozie http://localhost:11000/oozie -change 0000001-140321155112907-oozie-puru-C  -value status=RUNNING
```

The `endtime/concurrency/pausetime` option changes a coordinator job that is not in `KILLED` status.

Valid value names are:


   * endtime: the end time of the coordinator job.
   * concurrency: the concurrency of the coordinator job.
   * pausetime: the pause time of the coordinator job.
   * status: new status for coordinator job.

Conditions and usage:

   * Repeated value names are not allowed.
   * New end time should not be before job's start time and last action time.
   * If end time is before job start time and if the job has not materialized any actions, then job status is changed to SUCCEEDED.
   * Currently status only takes RUNNING and can be used to change the status of FAILED, KILLED, IGNORED coordinator job to RUNNING and resuming materialization. This status change command does not affect the status of already materialized actions in the coordinator. If there are FAILED, KILLED or IGNORED coordinator actions they have to be rerun separately.
   * New concurrency value has to be a valid integer.
   * All lookahead actions which are in WAITING/READY state will be revoked according to the new pause/end time. If any action after new pause/end time is not in WAITING/READY state, an exception will be thrown.
   * Also empty string "" can be used to reset pause time to none.
   * Endtime/concurrency/pausetime of IGNORED Job cannot be changed.

After the command is executed the job's end time, concurrency or pause time should be changed. If an already-succeeded job changes its end time, its status will become running.

### Changing endtime/pausetime of a Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -change 14-20090525161321-oozie-joe -value pausetime=2011-12-01T05:00Z
```

The `change` option changes a bundle job that is not in `KILLED` status.

Valid value names are:

   * pausetime: the pause time of the bundle job.
   * endtime: the end time of the bundle job.

Repeated value names are not allowed. An empty string "" can be used to reset pause time to none. New end time should not be before job's kickoff time.

Bundle will execute pause/end date change command on each coordinator job. Refer conditions and usage section of coordinator change command for more details [Coordinator job change command](DG_CommandLineTool.html#Changing_endtimeconcurrencypausetimestatus_of_a_Coordinator_Job).

### Rerunning a Workflow Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -config job.properties -rerun 14-20090525161321-oozie-joe
```

The `rerun` option reruns a completed ( `SUCCEEDED`, `FAILED` or `KILLED` ) job skipping the specified nodes.

The parameters for the job must be provided in a file, either a Java Properties file (.properties) or a Hadoop XML
Configuration file (.xml). This file must be specified with the `-config` option.

The workflow application path must be specified in the file with the `oozie.wf.application.path` property. The
specified path must be an HDFS path.

The list of nodes to skipped must be provided in the `oozie.wf.rerun.skip.nodes` property separated by commas.

After the command is executed the job will be in `RUNNING` status.

Refer to the [Rerunning Workflow Jobs](DG_WorkflowReRun.html) for details on rerun.

### Rerunning a Coordinator Action or Multiple Actions

Example:


```
$oozie job -rerun <coord_Job_id> [-nocleanup] [-refresh] [-failed] [-config <arg>]
[-action 1, 3-4, 7-40] (-action or -date is required to rerun.)
[-date 2009-01-01T01:00Z::2009-05-31T23:59Z, 2009-11-10T01:00Z, 2009-12-31T22:00Z]
(if neither -action nor -date is given, the exception will be thrown.)
```

The `rerun` option reruns a terminated (`TIMEDOUT`, `SUCCEEDED`, `KILLED`, `FAILED`, `IGNORED`) coordinator action when coordinator job
is not in `FAILED` or `KILLED` state.

After the command is executed the rerun coordinator action will be in `WAITING` status.

Refer to the [Rerunning Coordinator Actions](DG_CoordinatorRerun.html) for details on rerun.

### Rerunning a Bundle Job

Example:


```
$oozie job -rerun <bundle_Job_id> [-nocleanup] [-refresh]
[-coordinator c1, c3, c4] (-coordinator or -date is required to rerun.)
[-date 2009-01-01T01:00Z::2009-05-31T23:59Z, 2009-11-10T01:00Z, 2009-12-31T22:00Z]
(if neither -coordinator nor -date is given, the exception will be thrown.)
```

The `rerun` option reruns coordinator actions belonging to specified coordinators within the specified date range.

After the command is executed the rerun coordinator action will be in `WAITING` status.


### Checking the Information and Status of a Workflow, Coordinator or Bundle Job or a Coordinator Action

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -info 14-20090525161321-oozie-joe
.
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
Workflow Name :  map-reduce-wf
App Path      :  hdfs://localhost:8020/user/joe/workflows/map-reduce
Status        :  SUCCEEDED
Run           :  0
User          :  joe
Group         :  users
Created       :  2009-05-26 05:01 +0000
Started       :  2009-05-26 05:01 +0000
Ended         :  2009-05-26 05:01 +0000
Actions
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
Action Name             Type        Status     Transition  External Id            External Status  Error Code    Start                   End
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
hadoop1                 map-reduce  OK         end         job_200904281535_0254  SUCCEEDED        -             2009-05-26 05:01 +0000  2009-05-26 05:01 +0000
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
```

The `info` option can display information about a workflow job or coordinator job or coordinator action.
The `info` option for a Coordinator job will retrieve the Coordinator actions ordered by nominal time. However, the `info` command may timeout if the number of Coordinator actions are very high. In that case, `info` should be used with `offset` and `len` option.

The `offset` and `len` option should be used for pagination. offset determines the start offset of the action
returned among all the actions that matched the filter criteria. len determines number of actions to be returned.

The `localtime` option displays times in local time, if not specified times are displayed in GMT.

The `filter` option can be used to filter coordinator actions based on some criteria.
The filter option syntax is: `<key><comparator><value>[;<key><comparator><value>]*`.
(Note escape `\` needed before semicolon to specify multiple names for filter in shell)
key: status or nominalTime
comparator: `, !`, <, <`, >, >`
value: valid status like SUCCEEDED, KILLED, RUNNING etc. Only ` and !` apply for status
value for nominalTime is valid date of the format yyyy-MM-dd'T'HH:mm'Z' (like 2014-06-01T00:00Z)

Multiple values must be specified as different name value pairs. The query is formed by doing AND of all conditions,
with the exception of = which uses OR if there are multiple values for the same key. For example,
filter 'status`RUNNING;status`WAITING;nominalTime>`2014-06-01T00:00Z' maps to query (status ` RUNNING OR status =
WAITING) AND nominalTime >` 2014-06-01T00:00Z which returns all waiting or running actions with nominalTime >`
2014-06-01T00:00Z.

Currently, the filter option can be used only with an `info` option on Coordinator job.

The `verbose` option gives more detailed information for all the actions, if checking for workflow job or coordinator job.
An example below shows how the `verbose` option can be used to gather action statistics information for a job:


```
$ oozie job -oozie http://localhost:11000/oozie -info 0000001-111219170928042-oozie-para-W@mr-node -verbose
ID : 0000001-111219170928042-oozie-para-W@mr-node
------------------------------------------------------------------------------------------------------------------------------------
Console URL       : http://localhost:50030/jobdetails.jsp?jobid=job_201112191708_0006
Error Code        : -
Error Message     : -
External ID       : job_201112191708_0006
External Status   : SUCCEEDED
Name              : mr-node
Retries           : 0
Tracker URI       : localhost:8021
Type              : map-reduce
Started           : 2011-12-20 01:12
Status            : OK
Ended             : 2011-12-20 01:12
External Stats    : {"org.apache.hadoop.mapred.JobInProgress$Counter":{"TOTAL_LAUNCHED_REDUCES":1,"TOTAL_LAUNCHED_MAPS":1,"DATA_LOCAL_MAPS":1},"ACTION_TYPE":"MAP_REDUCE","FileSystemCounters":{"FILE_BYTES_READ":1746,"HDFS_BYTES_READ":1409,"FILE_BYTES_WRITTEN":3524,"HDFS_BYTES_WRITTEN":1547},"org.apache.hadoop.mapred.Task$Counter":{"REDUCE_INPUT_GROUPS":33,"COMBINE_OUTPUT_RECORDS":0,"MAP_INPUT_RECORDS":33,"REDUCE_SHUFFLE_BYTES":0,"REDUCE_OUTPUT_RECORDS":33,"SPILLED_RECORDS":66,"MAP_OUTPUT_BYTES":1674,"MAP_INPUT_BYTES":1409,"MAP_OUTPUT_RECORDS":33,"COMBINE_INPUT_RECORDS":0,"REDUCE_INPUT_RECORDS":33}}
External ChildIDs : null
------------------------------------------------------------------------------------------------------------------------------------
```

The two fields External Stats and External ChildIDs display the action statistics information (that includes counter information in case of MR action and PigStats information in case of a pig action) and child ids of the given job.

Note that the user can turn on/off External Stats by specifying the property _oozie.action.external.stats.write_ as _true_ or _false_ in workflow.xml. By default, it is set to false (not to collect External Stats). External ChildIDs will always be stored.

### Listing all the Workflows for a Coordinator Action

A coordinator action kicks off different workflows for its original run and all subsequent reruns.
Getting a list of those workflow ids is a useful tool to keep track of your actions' runs and
to go debug the workflow job logs if required. Along with ids, it also lists their statuses,
and start and end times for quick reference.

This is achieved by using the Coordinator Action info command and specifying a flag **`allruns`**
along with the `info` command.


```
$ oozie job -info 0000001-111219170928042-oozie-joe-C@1 -allruns -oozie http://localhost:11000/oozie
.
Job ID                                   Status    Started                 Ended
.----------------------------------------------------------------------------------------------------
0000001-140324163709596-oozie-joe-W     SUCCEEDED 2014-03-24 23:40 GMT    2014-03-24 23:40 GMT
.----------------------------------------------------------------------------------------------------
0000000-140324164318985-oozie-joe-W     SUCCEEDED 2014-03-24 23:44 GMT    2014-03-24 23:44 GMT
.----------------------------------------------------------------------------------------------------
0000001-140324164318985-oozie-joe-W     SUCCEEDED 2014-03-24 23:44 GMT    2014-03-24 23:44 GMT
.----------------------------------------------------------------------------------------------------
```

### Listing all retry attempts of a workflow action

When retry-max is specified for an action in the workflow definition, and there is a failure, it will be retried till it succeeds or retry-max attempts are exhausted. To get information on all the retry attempts, `-retries` command can be used.


```
$ oozie job -retries 0000000-161212175234862-oozie-puru-W@pig-node -oozie http://localhost:11000/oozie

ID : 0000000-161212175234862-oozie-puru-W@pig-node
------------------------------------------------------------------------------------------------------------------------------------
Attempt        : 1
Start Time     : Tue, 13 Dec 2016 01:56:24 GMT
End Time       : Tue, 13 Dec 2016 01:56:27 GMT
Console URL    : http://localhost:50030/jobdetails.jsp?jobid=job_201612051339_2650
------------------------------------------------------------------------------------------------------------------------------------
Attempt        : 2
Start Time     : Tue, 13 Dec 2016 01:56:24 GMT
End Time       : Tue, 13 Dec 2016 01:56:27 GMT
Console URL    : http://localhost:50030/jobdetails.jsp?jobid=job_201612051339_2650
------------------------------------------------------------------------------------------------------------------------------------
Attempt        : 3
Start Time     : Tue, 13 Dec 2016 01:56:24 GMT
End Time       : Tue, 13 Dec 2016 01:56:27 GMT
Console URL    : http://localhost:50030/jobdetails.jsp?jobid=job_201612051339_2650
------------------------------------------------------------------------------------------------------------------------------------
$
```


### Checking the xml definition of a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -definition 14-20090525161321-oozie-joe
.
<workflow-app xmlns="uri:oozie:workflow:0.2" name="sm3-segment-2413">
	<start to="p0"/>
    <action name="p0">
    </action>
	<end name="end"/>
</workflow-app>

```

### Checking the server logs of a Workflow, Coordinator or Bundle Job

Example:


```

$ oozie job -oozie http://localhost:11000/oozie -log 14-20090525161321-oozie-joe

```

### Checking the server error logs of a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -errorlog 0000000-150121110331712-oozie-puru-B
2015-01-21 11:33:29,090  WARN CoordSubmitXCommand:523 - SERVER[-] USER[-] GROUP[-] TOKEN[-] APP[-] JOB[0000000-150121110331712-oozie-puru-B] ACTION[] SAXException :
org.xml.sax.SAXParseException; lineNumber: 20; columnNumber: 22; cvc-complex-type.2.4.a: Invalid content was found starting with element 'concurrency'. One of '{"uri:oozie:coordinator:0.2":controls, "uri:oozie:coordinator:0.2":datasets, "uri:oozie:coordinator:0.2":input-events, "uri:oozie:coordinator:0.2":output-events, "uri:oozie:coordinator:0.2":action}' is expected.
        at org.apache.xerces.util.ErrorHandlerWrapper.createSAXParseException(Unknown Source)
        at org.apache.xerces.util.ErrorHandlerWrapper.error(Unknown Source)
        at org.apache.xerces.impl.XMLErrorReporter.reportError(Unknown Source)
        at org.apache.xerces.impl.XMLErrorReporter.reportError(Unknown Source)
        at org.apache.xerces.impl.XMLErrorReporter.reportError(Unknown Source)
        at org.apache.xerces.impl.xs.XMLSchemaValidator$XSIErrorReporter.reportError(Unknown Source)
        at org.apache.xerces.impl.xs.XMLSchemaValidator.reportSchemaError(Unknown Source)
        at org.apache.xerces.impl.xs.XMLSchemaValidator.handleStartElement(Unknown Source)
        at org.apache.xerces.impl.xs.XMLSchemaValidator.startElement(Unknown Source)
        at org.apache.xerces.impl.XMLNSDocumentScannerImpl.scanStartElement(Unknown Source)
        at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl$FragmentContentDispatcher.dispatch(Unknown Source)
        at org.apache.xerces.impl.XMLDocumentFragmentScannerImpl.scanDocument(Unknown Source)
        at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
        at org.apache.xerces.parsers.XML11Configuration.parse(Unknown Source)
        at org.apache.xerces.jaxp.validation.StreamValidatorHelper.validate(Unknown Source)
        at org.apache.xerces.jaxp.validation.ValidatorImpl.validate(Unknown Source)
```


### Checking the audit logs of a Workflow, Coordinator or Bundle Job

Example:


```
$ oozie job -oozie http://localhost:11000/oozie -auditlog 0000000-150322000230582-oozie-puru-C
2015-03-22 00:04:35,494  INFO oozieaudit:520 - IP [-], USER [purushah], GROUP [null], APP [-], JOBID [0000000-150322000230582-oozie-puru-C], OPERATION [start], PARAMETER [null], STATUS [SUCCESS], HTTPCODE [200], ERRORCODE [null], ERRORMESSAGE [null]
2015-03-22 00:05:13,823  INFO oozieaudit:520 - IP [-], USER [purushah], GROUP [null], APP [-], JOBID [0000000-150322000230582-oozie-puru-C], OPERATION [suspend], PARAMETER [0000000-150322000230582-oozie-puru-C], STATUS [SUCCESS], HTTPCODE [200], ERRORCODE [null], ERRORMESSAGE [null]
2015-03-22 00:06:59,561  INFO oozieaudit:520 - IP [-], USER [purushah], GROUP [null], APP [-], JOBID [0000000-150322000230582-oozie-puru-C], OPERATION [suspend], PARAMETER [0000000-150322000230582-oozie-puru-C], STATUS [SUCCESS], HTTPCODE [200], ERRORCODE [null], ERRORMESSAGE [null]
2015-03-22 23:22:20,012  INFO oozieaudit:520 - IP [-], USER [purushah], GROUP [null], APP [-], JOBID [0000000-150322000230582-oozie-puru-C], OPERATION [suspend], PARAMETER [0000000-150322000230582-oozie-puru-C], STATUS [SUCCESS], HTTPCODE [200], ERRORCODE [null], ERRORMESSAGE [null]
2015-03-22 23:28:48,218  INFO oozieaudit:520 - IP [-], USER [purushah], GROUP [null], APP [-], JOBID [0000000-150322000230582-oozie-puru-C], OPERATION [resume], PARAMETER [0000000-150322000230582-oozie-puru-C], STATUS [SUCCESS], HTTPCODE [200], ERRORCODE [null], ERRORMESSAGE [null]
```

### Checking the server logs for particular actions of a Coordinator Job

Example:


```

$ oozie job -log <coord_job_id> [-action 1, 3-4, 7-40] (-action is optional.)

```

### Filtering the server logs with logfilter options

User can provide multiple option to filter logs using -logfilter opt1=val1;opt2=val1;opt3=val1. This can be used to fetch only just logs of interest faster as fetching Oozie server logs is slow due to the overhead of pattern matching.
Available options are:

   * recent: Specify recent hours/min of logs to scan. The recent offset specified is taken relative to the `end` time specified, job end time or the current system time if the job is still running in that order of precedence. For eg: recent=3h or recent=30m will fetch logs starting 3 hours/30 minutes before the end time and up to the end time. H/h is used to denote hour and M/m is used to denote minutes. If no suffix is specified default is hours.
   * start: Start time for scanning logs. Default is start time of the job. User can provide a valid date or offset similar to `recent` option. Valid date formats are "yyyy-MM-dd'T'HH:mm'Z'" and "yyyy-MM-dd HH:mm:ss,SSS". When an offset is specified, it is calculated relative to the start time of the job. For eg: start=2h will fetch logs starting 2 hours after the job was started.
   * end: End time for scanning logs. Default is end time of the job or current system time if the job is still running. User can provide a valid date or offset similar to `start` option. When an offset is specified, it is calculated relative to start time i.e job start time . For eg: end=2h will fetch logs from start time and  start time plus 2 hours.
   * loglevel : Multiple log levels separated by "|" can be specified. Supported log levels are ALL, DEBUG, ERROR, INFO, TRACE, WARN, FATAL.
   * text: String to search in logs.
   * limit : Limit number of line to be searched. Log search will end when when n lines(excluding stack-trace) have been matched.
   * debug : Prints debug information on the log files, time ranges and patterns being searched for. Can be used to debug if expected logs are not shown with the filter options provided.

Examples.
Searching log with log level ERROR or WARN will only give log with Error and Warning (with stack-trace) only.
This will be useful if job has failed and user want to find error logs with exception.

```

$ ./oozie job -log 0000006-140319184715726-oozie-puru-W  -logfilter loglevel=WARN\;limit=3 -oozie http://localhost:11000/oozie/
2014-03-20 10:01:52,977  WARN ActionStartXCommand:542 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[map-reduce-wf] JOB[0000006-140319184715726-oozie-puru-W] ACTION[0000006-140319184715726-oozie-puru-W@:start:] [***0000006-140319184715726-oozie-puru-W@:start:***]Action status=DONE
2014-03-20 10:01:52,977  WARN ActionStartXCommand:542 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[map-reduce-wf] JOB[0000006-140319184715726-oozie-puru-W] ACTION[0000006-140319184715726-oozie-puru-W@:start:] [***0000006-140319184715726-oozie-puru-W@:start:***]Action updated in DB!
2014-03-20 10:01:53,189  WARN ActionStartXCommand:542 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[map-reduce-wf] JOB[0000006-140319184715726-oozie-puru-W] ACTION[0000006-140319184715726-oozie-puru-W@mr-node-1] Error starting action [mr-node-1]. ErrorType [TRANSIENT], ErrorCode [JA009], Message [JA009: java.io.IOException: java.io.IOException: Queue "aaadefault" does not exist
	at org.apache.hadoop.mapred.JobTracker.submitJob(JobTracker.java:3615)
	at org.apache.hadoop.mapred.JobTracker.submitJob(JobTracker.java:3561)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at org.apache.hadoop.ipc.RPC$Server.call(RPC.java:587)
	at org.apache.hadoop.ipc.Server$Handler$1.run(Server.java:1432)
	at org.apache.hadoop.ipc.Server$Handler$1.run(Server.java:1428)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.Subject.doAs(Subject.java:394)
	at org.apache.hadoop.security.UserGroupInformation.doAs(UserGroupInformation.java:1190)
	at org.apache.hadoop.ipc.Server$Handler.run(Server.java:1426)
Caused by: java.io.IOException: Queue "aaadefault" does not exist
	at org.apache.hadoop.mapred.JobInProgress.<init>(JobInProgress.java:433)
	at org.apache.hadoop.mapred.JobTracker.submitJob(JobTracker.java:3613)
	... 12 more
$
```

Search with specific text and recent option.

```
$ ./oozie job -log 0000003-140319184715726-oozie-puru-C  -logfilter text=Missing\;limit=4\;recent=1h -oozie http://localhost:11000/oozie/
2014-03-20 09:59:50,329  INFO CoordActionInputCheckXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[-] APP[-] JOB[0000003-140319184715726-oozie-puru-C] ACTION[0000003-140319184715726-oozie-puru-C@1] [0000003-140319184715726-oozie-puru-C@1]::CoordActionInputCheck:: Missing deps:hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/
2014-03-20 09:59:50,330  INFO CoordActionInputCheckXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[-] APP[-] JOB[0000003-140319184715726-oozie-puru-C] ACTION[0000003-140319184715726-oozie-puru-C@1] [0000003-140319184715726-oozie-puru-C@1]::ActionInputCheck:: In checkListOfPaths: hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/ is Missing.
2014-03-20 10:02:19,087  INFO CoordActionInputCheckXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[-] APP[-] JOB[0000003-140319184715726-oozie-puru-C] ACTION[0000003-140319184715726-oozie-puru-C@2] [0000003-140319184715726-oozie-puru-C@2]::CoordActionInputCheck:: Missing deps:hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/
2014-03-20 10:02:19,088  INFO CoordActionInputCheckXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[-] APP[-] JOB[0000003-140319184715726-oozie-puru-C] ACTION[0000003-140319184715726-oozie-puru-C@2] [0000003-140319184715726-oozie-puru-C@2]::ActionInputCheck:: In checkListOfPaths: hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/ is Missing.
$
```

Search example with specific date range.

```
$ ./oozie job -log 0000003-140319184715726-oozie-puru-C  -logfilter "start=2014-03-20 10:00:57,063;end=2014-03-20 10:10:57,063" -oozie http://localhost:11000/oozie/
2014-03-20 10:00:57,063  INFO CoordActionUpdateXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[-] APP[-] JOB[0000003-140319184715726-oozie-puru-C] ACTION[0000003-140319184715726-oozie-puru-C@1] Updating Coordinator action id :0000003-140319184715726-oozie-puru-C@1 status  to KILLED, pending = 0
2014-03-20 10:02:18,967  INFO CoordMaterializeTransitionXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[aggregator-coord] JOB[0000003-140319184715726-oozie-puru-C] ACTION[-] materialize actions for tz=Coordinated Universal Time,
 start=Thu Dec 31 18:00:00 PST 2009, end=Thu Dec 31 19:00:00 PST 2009,
 timeUnit 12,
 frequency :60:MINUTE,
 lastActionNumber 1
2014-03-20 10:02:18,971  WARN CoordELFunctions:542 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[aggregator-coord] JOB[0000003-140319184715726-oozie-puru-C] ACTION[-] If the initial instance of the dataset is later than the current-instance specified, such as coord:current(-200) in this case, an empty string is returned. This means that no data is available at the current-instance specified by the user and the user could try modifying his initial-instance to an earlier time.
2014-03-20 10:02:18,975  INFO CoordMaterializeTransitionXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[aggregator-coord] JOB[0000003-140319184715726-oozie-puru-C] ACTION[-] [0000003-140319184715726-oozie-puru-C]: all actions have been materialized, set pending to true
2014-03-20 10:02:18,975  INFO CoordMaterializeTransitionXCommand:539 - SERVER[ ] USER[-] GROUP[-] TOKEN[] APP[aggregator-coord] JOB[0000003-140319184715726-oozie-puru-C] ACTION[-] Coord Job status updated to = RUNNING
```

### Dryrun of Coordinator Job

* This feature is only supported in Oozie 2.0 or later.

Example:


```

$ oozie job -oozie http://localhost:11000/oozie -dryrun -config job.properties
***coordJob after parsing: ***
<coordinator-app xmlns="uri:oozie:coordinator:0.1" name="sla_coord" frequency="20"
start="2009-03-06T010:00Z" end="2009-03-20T11:00Z" timezone="America/Los_Angeles">
  <output-events>
    <data-out name="Output" dataset="DayLogs">
      <dataset name="DayLogs" frequency="1440" initial-instance="2009-01-01T00:00Z" timezone="UTC" freq_timeunit="MINUTE" end_of_duration="NONE">
        <uri-template>hdfs://localhost:8020/user/angeloh/coord_examples/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
      <instance>${coord:current(0)}</instance>
    </data-out>
  </output-events>
  <action>
  </action>
</coordinator-app>

***actions for instance***
***total coord actions is 1 ***
------------------------------------------------------------------------------------------------------------------------------------
coordAction instance: 1:
<coordinator-app xmlns="uri:oozie:coordinator:0.1" name="sla_coord" frequency="20"
start="2009-03-06T010:00Z" end="2009-03-20T11:00Z" timezone="America/Los_Angeles">
  <output-events>
    <data-out name="Output" dataset="DayLogs">
      <uris>hdfs://localhost:8020/user/angeloh/coord_examples/2009/03/06</uris>
      <dataset name="DayLogs" frequency="1440" initial-instance="2009-01-01T00:00Z" timezone="UTC" freq_timeunit="MINUTE" end_of_duration="NONE">
        <uri-template>hdfs://localhost:8020/user/angeloh/coord_examples/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
    </data-out>
  </output-events>
  <action>
  </action>
</coordinator-app>
------------------------------------------------------------------------------------------------------------------------------------

```

The `dryrun` option tests running a coordinator job with given job properties and does not create the job.

The parameters for the job must be provided in a file, either a Java Properties file (.properties) or a Hadoop XML
Configuration file (.xml). This file must be specified with the `-config` option.

The coordinator application path must be specified in the file with the `oozie.coord.application.path` property. The
specified path must be an HDFS path.

### Dryrun of Workflow Job

* This feature is only supported in Oozie 3.3.2 or later.

Example:


```

$ oozie job -oozie http://localhost:11000/oozie -dryrun -config job.properties
OK

```

The `dryrun` option tests running a workflow job with given job properties and does not create the job.

The parameters for the job must be provided in a file, either a Java Properties file (.properties) or a Hadoop XML
Configuration file (.xml). This file must be specified with the `-config` option.

The workflow application path must be specified in the file with the `oozie.wf.application.path` property. The
specified path must be an HDFS path.

If the workflow is accepted (i.e. Oozie is able to successfully read and parse it), it will return `"OK"`; otherwise, it will return
an error message describing why it was rejected.

### Dryrun of Bundle Job

* This feature is only supported in Oozie 5.1 or later.

Example:


```

$ oozie job -oozie http://localhost:11000/oozie -dryrun -config job.properties
***Bundle job after parsing: ***
<bundle-app xmlns="uri:oozie:bundle:0.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" name="My Bundle">
  <parameters>
    <property>
      <name>oozie.use.system.libpath</name>
      <value>true</value>
    </property>
  </parameters>
  <controls>
    <kick-off-time>2017-02-14T00:13Z</kick-off-time>
  </controls>
  <coordinator name="My Coordinator-0">
    <app-path>hdfs://localhost:8020/user/hue/oozie/deployments/_admin_-oozie-24-1487060026.39</app-path>
    <configuration>
      <property>
        <name>wf_application_path</name>
        <value>hdfs://localhost:8020/user/hue/oozie/workspaces/hue-oozie-1486786607.01</value>
      </property>
      <property>
        <name>start_date</name>
        <value>2017-02-14T08:12Z</value>
      </property>
      <property>
        <name>end_date</name>
        <value>2017-02-21T08:12Z</value>
      </property>
    </configuration>
  </coordinator>
</bundle-app>

```

The `dryrun` option tests running a bundle job with given job properties and does not create the job.

The parameters for the job must be provided in a file, either a Java Properties file (.properties) or a Hadoop XML
Configuration file (.xml). This file must be specified with the `-config` option.

If the bundle job is accepted (i.e. Oozie is able to successfully read and parse it), it will return the parsed bundle job in xml;
otherwise, it will return an error message describing why it was rejected.

### Updating coordinator definition and properties
Existing coordinator definition will be replaced by new definition. The refreshed coordinator would keep the same coordinator ID, state, and coordinator actions.
All created coord action(including in WAITING) will use old configuration.
One can rerun actions with -refresh option, -refresh option will use new configuration to rerun coord action

Update command also verifies coordinator definition like submit command, if there is any issue with definition, update will fail.
Update command with -dryrun will show coordinator definition and properties differences.
Config option is optional, if not specified existing coordinator property is used to find coordinator path.

Update command doesn't allow update of coordinator name, frequency, start time, end time and timezone and will fail on an attempt to change any of them. To change end time of coordinator use the `-change` command.

To change the entire XML for a running coordinator, hdfs path for the new XML can be specified
as `oozie.coord.application.path` in job.properties. Then, use `-config job.properties` in the update command.



```
$ oozie job -oozie http://localhost:11000/oozie -config job.properties -update 0000005-140402104721140-oozie-puru-C -dryrun
.
**********Job definition changes**********
@@ -3,8 +3,8 @@
     <concurrency>1</concurrency>
   </controls>
   <input-events>
-    <data-in name="input" dataset="raw-logs">
-      <dataset name="raw-logs" frequency="20" initial-instance="2010-01-01T00:00Z" timezone="UTC" freq_timeunit="MINUTE" end_of_duration="NONE">
+    <data-in name="input" dataset="raw-logs-rename">
+      <dataset name="raw-logs-rename" frequency="20" initial-instance="2010-01-01T00:00Z" timezone="UTC" freq_timeunit="MINUTE" end_of_duration="NONE">
         <uri-template>hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/</uri-template>
         <done-flag />
       </dataset>
**********************************
**********Job conf changes**********
@@ -8,10 +8,6 @@
     <value>hdfs://localhost:9000/user/purushah/examples/apps/aggregator/coordinator.xml</value>
   </property>
   <property>
-    <name>old</name>
-    <value>test</value>
-  </property>
-  <property>
     <name>user.name</name>
     <value>purushah</value>
   </property>
@@ -28,6 +24,10 @@
     <value>hdfs://localhost:9000</value>
   </property>
   <property>
+    <name>adding</name>
+    <value>new</value>
+  </property>
+  <property>
     <name>jobTracker</name>
     <value>localhost:9001</value>
   </property>
**********************************
```


### Ignore a Coordinator Job

Example:


```
$oozie job -ignore <coord_Job_id>
```

The `ignore` option changes a coordinator job in `KILLED`, `FAILED` to `IGNORED` state.
When a coordinator job in a bundle is in `IGNORED` state, the coordinator job doesn't impact the state of the bundle job.
For example, when a coordinator job in a bundle failed and afterwards ignored, the bundle job becomes `SUCCEEDED` instead of `DONEWITHERROR` as long as other coordinator jobs in the bundle succeeded.
 A ignored coordinator job can be changed to `RUNNING` using -change command.
 Refer to the [Coordinator job change command](DG_CommandLineTool.html#Changing_endtimeconcurrencypausetimestatus_of_a_Coordinator_Job) for details.

### Ignore a Coordinator Action or Multiple Coordinator Actions

Example:


```
$oozie job -ignore <coord_Job_id> -action 1,3-4,7-40
```
The `ignore` option changes a coordinator action(s) in terminal state (`KILLED`, `FAILED`, `TIMEDOUT`) to `IGNORED` state, while not changing the state of the coordinator job.
When a coordinator action is in `IGNORED` state, the action doesn't impact the state of a coordinator job.
For example, when a coordinator action failed and afterwards ignored, a coordinator job becomes `SUCCEEDED` instead of `DONEWITHERROR` as long
 as other coordinator actions succeeded.

A ignored coordinator action can be rerun using -rerun command.
Refer to the [Rerunning Coordinator Actions](DG_CoordinatorRerun.html) for details.
When a workflow job of a ignored coordinator action is rerun, the coordinator action becomes `RUNNING` state.

### Polling an Oozie job

This command allows polling the Oozie server for an Oozie job until it reaches a completed status (e.g. `SUCCEEDED`, `KILLED`, etc).

Example:


```
$ oozie job -poll <job_id> -interval 10 -timeout 60 -verbose
.
RUNNING
RUNNING
RUNNING
SUCCEEDED
```

The `-poll` argument takes a valid Workflow Job ID, Coordinator Job ID, Coordinator Action ID, or Bundle Job ID.

All other arguments are optional:

   * `verbose` will cause the job status to be printed at each poll; otherwise, there will be no output
   * `interval`  allows specifying the polling interval in minutes (default is 5)
   * `timeout` allows specifying the timeout in minutes (default is 30 minutes); negative values indicate no timeout

### Changing job SLA definition and alerting
   * slaenable command can be used to enable job sla alerts.
   * sladisable command can be used to disable job sla alerts.
   * slachange command can be used to change sla job definition.
   * Supported parameters for sla change command are should-start, should-end and max-duration. Please specify the value in single quotes instead of double quotes in command line to avoid bash interpreting braces in EL functions and causing error.
   * All sla commands takes -action or -date parameter. For bundle jobs additional -coordinator (coord_name/id) parameter can be passed. Sla change command need extra parameter -value to specify new sla definition.
   * Sla commands without -action or -date parameter is applied to all non terminated actions and all future actions.
   * Sla commands with -action or -date parameter will be applied to only non terminated actions.

  Eg.

```
  $oozie job -slaenable <coord_Job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z]
  $oozie job -sladisable <coord_Job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z]
  $oozie job -slachange <coord_Job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z] -value 'sla-max-duration=${10 ** MINUTES};sla-should-end=${30 ** MINUTES};sla-max-duration=${30 * MINUTES}'
  $oozie job -slaenable <bundle_job_id> [-action 1,3-4,7-40] [-date 2009-01-01T01:00Z::2009-05-31T23:59Z,2009-11-10T01:00Z::2009-12-31T22:00Z] [-coordinator <List_of_coord_names/ids]
```

### Getting missing dependencies of coordinator action(s)
   * Coordination action id can be specified directly for getting missing dependencies of a single action.
   * To get information on multiple actions, either -action or -date option can be specified with the coordinator job id.
   * missingdeps command doesn't recompute dependencies. It list missing dependencies which were last computed.
   * Oozie checks missing dependencies sequentially, and it will stop on first missing dependency. `Blocked On` is the first missing dependency for action. So, there could be a chance that Oozie will report some missing dependencies, but it might be present. To resolve the waiting issue, one should fix the blockedOn missing dependency.
   * For input logic, missingdeps command doesn't compute input-logic expression. It will report everything which is missing or not computed.

```
$oozie job  -oozie http://localhost:11000/oozie -missingdeps 0000000-170104141851590-oozie-puru-C -action 1
$oozie job  -oozie http://localhost:11000/oozie -missingdeps 0000000-170104141851590-oozie-puru-C@1
.
CoordAction :  1
Blocked on  : hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/2010/01/01/00/00/_SUCCESS
.
Dataset     : input-1
Pending Dependencies :
      hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/2010/01/01/01/00/_SUCCESS
.
Dataset     : input-2
Pending Dependencies :
      hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/2010/01/01/01/00/_SUCCESS
      hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/2010/01/01/00/40/_SUCCESS
      hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/2010/01/01/00/20/_SUCCESS
      hdfs://localhost:9000/user/purushah/examples/input-data/rawLogs/2010/01/01/00/00/_SUCCESS
$
```

### Checking a workflow definition generated by a Fluent Job API jar file

Since Oozie 5.1.0.

Generate a workflow definition given the Fluent Job API jar file supplied at command line, and check for its correctness.

**Preconditions:**

   * the Fluent Job API jar file has to be present and readable by the current user at the local path provided
   * the folder containing the Fluent Job API jar file provided has to be writable by the current user, since the generated workflow
   definition is written there

If the `-verbose` option is provided, a lot more debugging output, including the generated workflow definition, is given.

For more information what an Fluent Job API jar file is, how to build it etc.,
refer to [Fluent Job API - API JAR format](DG_FluentJobAPI.html#AE.A_Appendix_A_API_JAR_format).

**Example:**


```
$ oozie job -oozie http://localhost:11000/oozie -validatejar /tmp/workflow-api-jar.jar
Valid workflow-app
```

**Example (verbose):**


```
$ oozie job -oozie http://localhost:11000/oozie -validatejar /tmp/workflow-api-jar.jar -verbose
Checking API jar:/tmp/workflow-api-jar.jar
Loading API jar /tmp/workflow-api-jar.jar
Workflow job definition generated from API jar:
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workflow:workflow-app xmlns:workflow="uri:oozie:workflow:1.0" ... name="shell-example">
...
</workflow:workflow-app>
.
API jar is written to /tmp/workflow1876390751841950810.xml
Servlet response is:
Valid workflow-app
API jar is valid.
```

### Submitting a workflow definition generated by a Fluent Job API jar file

Since Oozie 5.1.0.

Generate a workflow definition given the Fluent Job API jar file supplied at command line, write it to a provided or generated HDFS
location, and submit.

**Preconditions:**

   * all the parameters that are present in the workflow definition have to be supplied either as command line parameters or via
   `job.properties` passed along with the `-config` option
   * the Fluent Job API jar file has to be present and readable by the current user at the local path provided
   * the folder containing the Fluent Job API jar file provided has to be writable by the current user, since the generated workflow
   definition is written there
   * the HDFS folder either given by `-Doozie.wf.application.path` command line parameter, or in its absence contained by
   `oozie-site.xml#oozie.client.jobs.application.generated.path` has to be writable by the current user

If the `-verbose` option is provided, a lot more debugging output, including the generated workflow definition, is given.

For more information what an Fluent Job API jar file is, how to build it etc., refer to
[Fluent Job API - API JAR format](DG_FluentJobAPI.html#AE.A_Appendix_A_API_JAR_format).

**Example:**


```
$ oozie job -oozie http://localhost:11000/oozie -submitjar /tmp/workflow-api-jar.jar -config /tmp/job.properties
job: 0000009-180107110323219-oozie-oozi-W
```

**Example (verbose):**


```
$ oozie job -oozie http://localhost:11000/oozie -submitjar /tmp/workflow-api-jar.jar -config /tmp/job.properties -verbose
Submitting a job based on API jar: /tmp/workflow-api-jar.jar
Loading API jar /tmp/workflow-api-jar.jar
Workflow job definition generated from API jar:
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workflow:workflow-app xmlns:workflow="uri:oozie:workflow:1.0" ... name="shell-example">
...
</workflow:workflow-app>
.
job: 0000010-180107110323219-oozie-oozi-W
Job based on API jar submitted successfully.
```

### Running a workflow definition generated by a Fluent Job API jar file

Since Oozie 5.1.0.

Generate a workflow definition given the Fluent Job API jar file supplied at command line, write it to a provided or generated HDFS
location, submit and run.

**Preconditions:**

   * all the parameters that are present in the workflow definition have to be supplied either as command line parameters or via
   `job.properties` passed along with the `-config` option
   * the Fluent Job API jar file has to be present and readable by the current user at the local path provided
   * the folder containing the Fluent Job API jar file provided has to be writable by the current user, since the generated workflow
   definition is written there
   * the HDFS folder either given by `-Doozie.wf.application.path` command line parameter, or in its absence contained by
   `oozie-site.xml#oozie.client.jobs.application.generated.path` has to be writable by the current user

If the `-verbose` option is provided, a lot more debugging output, including the generated workflow definition, is given.

For more information what an Fluent Job API jar file is, how to build it etc., refer to
[Fluent Job API - API JAR format](DG_FluentJobAPI.html#AE.A_Appendix_A_API_JAR_format).

**Example:**


```
$ oozie job -oozie http://localhost:11000/oozie -runjar /tmp/workflow-api-jar.jar -config /tmp/job.properties
job: 0000011-180107110323219-oozie-oozi-W
```

**Example (verbose):**


```
$ oozie job -oozie http://localhost:11000/oozie -runjar /tmp/workflow-api-jar.jar -config /tmp/job.properties -verbose
Submitting a job based on API jar: /tmp/workflow-api-jar.jar
Loading API jar /tmp/workflow-api-jar.jar
Workflow job definition generated from API jar:
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workflow:workflow-app xmlns:workflow="uri:oozie:workflow:1.0" ... name="shell-example">
...
</workflow:workflow-app>
.
job: 0000010-180107110323219-oozie-oozi-W
Job based on API jar run successfully.
```

## Jobs Operations

### Checking the Status of multiple Workflow Jobs

Example:


```
$ oozie jobs -oozie http://localhost:11000/oozie -localtime -len 2 -filter status=RUNNING
.
Job Id                          Workflow Name         Status     Run  User      Group     Created                Started                 Ended
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
4-20090527151008-oozie-joe     hadoopel-wf           RUNNING    0    joe      other     2009-05-27 15:34 +0530 2009-05-27 15:34 +0530  -
0-20090527151008-oozie-joe     hadoopel-wf           RUNNING    0    joe      other     2009-05-27 15:15 +0530 2009-05-27 15:15 +0530  -
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
```

The `jobs` sub-command will display information about multiple jobs.

The `offset` and `len` option specified the offset and number of jobs to display, default values are `1` and `100`
respectively.

The `localtime` option displays times in local time, if not specified times are displayed in GMT.

The `verbose` option gives more detailed information for each job.

A filter can be specified after all options.

The `filter`option syntax is: `[NAME=VALUE][;NAME=VALUE]*`.

Valid filter names are:

   * text: any text that might be a part of application name or a part of user name or a complete job ID
   * name: the workflow application name from the workflow definition.
   * user: the user that submitted the job.
   * group: the group for the job.
   * status: the status of the job.
   * startcreatedtime: the start of time window in specifying createdtime range filter.
   * endcreatedtime: the end of time window in specifying createdtime range filter
   * sortby: order the results. Supported values for `sortby` are: `createdTime` and `lastModifiedTime`

The query will do an AND among all the filter names. The query will do an OR among all the filter values for the same
name. Multiple values must be specified as different name value pairs.

startCreatedTime and endCreatedTime should be specified either in **ISO8601 (UTC)** format (**yyyy-MM-dd'T'HH:mm'Z'**) or a offset value in days or hours from the current time. For example, -2d means the current time - 2 days. -3h means the current time - 3 hours, -5m means the current time - 5 minutes

### Checking the Status of multiple Coordinator Jobs

* This feature is only supported in Oozie 2.0 or later.

Example:


```
$ oozie jobs -oozie http://localhost:11000/oozie -jobtype coordinator
.
Job ID                                                                                   App Name               Status      Freq Unit                    Started                 Next Materialized
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
0004165-100531045722929-oozie-wrkf-C     smaggs-xaggsptechno-coordinator SUCCEEDED 1440 MINUTE       2010-05-27 00:00        2010-05-29 00:00
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
0003823-100531045722929-oozie-wrkf-C     coordcal2880minutescurrent SUCCEEDED 2880 MINUTE       2010-02-01 16:30        2010-02-05 16:30
.----------------------------------------------------------------------------------------------------------------------------------------------------------------
```

The `jobtype` option specified the job type to display, default value is 'wf'. To see the coordinator jobs, value is 'coordinator'.

Valid filter names are:

   * name: the workflow application name from the workflow definition.
   * user: the user that submitted the job.
   * group: the group for the job.
   * status: the status of the job.
   * frequency: the frequency of the Coordinator job.
   * unit: the time unit. It can take one of the following four values: months, days, hours or minutes. Time unit should be added only when frequency is specified.
   * sortby: order the results. Supported values for `sortby` are: `createdTime` and `lastModifiedTime`

### Checking the Status of multiple Bundle Jobs

* This feature is only supported in Oozie 3.0 or later.

Example:


```
$ oozie jobs -oozie http://localhost:11000/oozie -jobtype bundle
Job ID                                   Bundle Name    Status    Kickoff             Created             User         Group
.------------------------------------------------------------------------------------------------------------------------------------
0000027-110322105610515-oozie-chao-B     BUNDLE-TEST    RUNNING   2012-01-15 00:24    2011-03-22 18:07    joe        users
.------------------------------------------------------------------------------------------------------------------------------------
0000001-110322105610515-oozie-chao-B     BUNDLE-TEST    RUNNING   2012-01-15 00:24    2011-03-22 18:06    joe        users
.------------------------------------------------------------------------------------------------------------------------------------
0000000-110322105610515-oozie-chao-B     BUNDLE-TEST    DONEWITHERROR2012-01-15 00:24    2011-03-22 17:58    joe        users
.------------------------------------------------------------------------------------------------------------------------------------
```

The `jobtype` option specified the job type to display, default value is 'wf'. To see the bundle jobs, value is 'bundle'.

### Bulk kill, suspend or resume multiple jobs

Example:


```
$ oozie jobs -oozie http://localhost:11000/oozie -kill|-suspend|-resume -filter name=cron-coord -jobtype coordinator
The following jobs have been killed|suspended|resumed
Job ID                                   App Name       Status    Freq Unit         Started                 Next Materialized
.------------------------------------------------------------------------------------------------------------------------------------
0000005-150224141553231-oozie-bzha-C     cron-coord     KILLED    10   MINUTE       2015-02-24 22:05 GMT    2015-02-24 23:05 GMT
.------------------------------------------------------------------------------------------------------------------------------------
0000001-150224141553231-oozie-bzha-C     cron-coord     KILLED    10   MINUTE       2015-02-24 22:00 GMT    2015-02-24 23:00 GMT
.------------------------------------------------------------------------------------------------------------------------------------
0000000-150224141553231-oozie-bzha-C     cron-coord     KILLED    10   MINUTE       2015-02-25 22:00 GMT    -
.------------------------------------------------------------------------------------------------------------------------------------
```

The above command will kill, suspend, or resume all the coordinator jobs with name of "cron-coord" starting with offset 1
to 50.
The `jobs` sub-command will bulk modify all the jobs that satisfy the filter, len, offset, and jobtype options when adding
a -kill|-suspend|-resume option. Another way to think about is the subcommand works to modify all the jobs that will be
displayed if the write option(kill|suspend|resume) is not there.

The `offset` and `len` option specified the offset and number of jobs to be modified, default values are `1` and `50`
respectively.
The `jobtype` option specifies the type of jobs to be modified, be it "wf", "coordinator" or "bundle". default value is "wf".

A filter can be specified after all options.

The `filter`option syntax is: `[NAME=VALUE][;NAME=VALUE]*`.

Valid filter names are:

   * name: the workflow application name from the workflow definition.
   * user: the user that submitted the job.
   * group: the group for the job.
   * status: the status of the job.
   * frequency: the frequency of the Coordinator job.
   * unit: the time unit. It can take one of the following four values: months, days, hours or minutes. Time unit should be added only when frequency is specified.
   * sortby: order the results. Supported values for `sortby` are: `createdTime` and `lastModifiedTime`

The query will do an AND among all the filter names. The query will do an OR among all the filter values for the same
name. Multiple values must be specified as different name value pairs.

The following example shows how to suspend the first 20 bundle jobs whose name is "bundle-app":


```
$ oozie jobs -oozie http://localhost:11000/oozie -suspend -filter name=bundle-app -jobtype bundle -len 20
```

### Bulk monitoring for jobs launched via Bundles

* This command-line query helps to directly query for a bulk of jobs using a set of rich filters.
The jobs need to have a parent **Bundle**, and this performs a deep query to provide results about all the workflows that satisfy your filters.
These results display relevant job-ids, app-names, and error message (if any) and are most helpful when you need to monitor a bulk of jobs and get a gist,
while avoiding typing multiple queries.

This feature is only supported in Oozie 3.3 or later.

Example 1:


```
$ oozie jobs -oozie http://localhost:11000/oozie -bulk bundle=bundle-app-1
.
Bundle Name  Bundle ID                             Coord Name  Coord Action ID                         External ID                            Status    Created Time          Error Message
.-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
bundle-app-1 0000000-130408151805946-oozie-chit-B  coord-1     0000001-130408151805946-oozie-chit-C@1  0000002-130408151805946-oozie-chit-W   KILLED    2013-04-08 22:20 GMT  null
.-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```

Example 2: This example illustrates giving multiple arguments and -verbose option.

_NOTE: The filter string after -bulk should be enclosed within quotes_


```
.
$ oozie jobs -oozie http://localhost:11000/oozie -bulk 'bundle=bundle-app-2;actionstatus=SUCCEEDED' -verbose
.
Bundle Name : bundle-app-2
.------------------------------------------------------------------------------------------------------------------------------------
Bundle ID        : 0000000-130422184245158-oozie-chit-B
Coordinator Name : coord-1
Coord Action ID  : 0000001-130422184245158-oozie-chit-C@1
Action Status    : SUCCEEDED
External ID      : 0000002-130422184245158-oozie-chit-W
Created Time     : 2013-04-23 01:43 GMT
User             : user_xyz
Error Message    : -
.------------------------------------------------------------------------------------------------------------------------------------
```

You can type 'help' to view usage for the CLI options and filters available. Namely:

   * bundle: Bundle app-name (mandatory)
   * coordinators: multiple, comma-separated Coordinator app-names. By default, if none specified, all coordinators pertaining to the bundle are included
   * actionstatus: status of jobs (default job-type is coordinator action aka workflow job) you wish to filter on. Default value is (KILLED,FAILED)
   * startcreatedtime/endcreatedtime: specify boundaries for the created-time. Only jobs created within this window are included.
   * startscheduledtime/endscheduledtime: specify boundaries for the nominal-time. Only jobs scheduled to run within this window are included.


```
$ oozie jobs <OPTIONS> : jobs status
                 -bulk <arg>       key-value pairs to filter bulk jobs response. e.g.
                                   bundle=<B>\;coordinators=<C>\;actionstatus=<S>\;
                                   startcreatedtime=<SC>\;endcreatedtime=<EC>\;
                                   startscheduledtime=<SS>\;endscheduledtime=<ES>\;
                                   coordinators and actionstatus can be multiple comma
                                   separated values. "bundle" and "coordinators" are 'names' of those jobs.
                                   Bundle name is mandatory, other params are optional
```

Similar to the usual jobs filter, different filter arguments here should be separated by semicolon (;).

## Admin Operations

### Checking the Status of the Oozie System

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -status
.
Safemode: OFF
```

It returns the current status of the Oozie system.

### Changing the Status of the Oozie System

* This feature is only supported in Oozie 2.0 or later.

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -systemmode [NORMAL|NOWEBSERVICE|SAFEMODE]
.
Safemode: ON
```

It returns the current status of the Oozie system.

### Displaying the Build Version of the Oozie System

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -version
.
Oozie server build version: 2.0.2.1-0.20.1.3092118008--
```

It returns the Oozie server build version.

### Displaying the queue dump of the Oozie System

* This feature is for administrator debugging purpose

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -queuedump
.
[Server Queue Dump]:
(coord_action_start,1),(coord_action_start,1),(coord_action_start,1)
(coord_action_ready,1)
(action.check,2)

```

It returns the Oozie server current queued commands.

### Displaying the list of available Oozie Servers

Example:


```
$ oozie admin -oozie http://hostA:11000/oozie -servers
hostA : http://hostA:11000/oozie
hostB : http://hostB:11000/oozie
hostC : http://hostC:11000/oozie
```

It returns a list of available Oozie Servers.  This is useful when Oozie is configured for [High Availability](AG_Install.html#HA); if
not, it will simply return the one Oozie Server.

### Displaying the Oozie server configuration

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -configuration
local.realm : LOCALHOST
oozie.JobCommand.job.console.url : http://localhost:11000/oozie?job=
oozie.action.fs.glob.max : 1000
oozie.action.jobinfo.enable : false
oozie.action.launcher.mapreduce.job.ubertask.enable : true
oozie.action.launcher.yarn.timeline-service.enabled : false
oozie.action.mapreduce.uber.jar.enable : false
oozie.action.max.output.data : 2048
oozie.action.retries.max : 3
...
```

It returns a list of the configuration properties and values from oozie-site.xml and oozie-default.xml being used by the Oozie
server.

### Displaying the Oozie server OS environment

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -osenv
...
JETTY_OPTS :  -Doozie.home.dir=/Users/asasvari/dev/oozie -Doozie.config.dir=/Users/asasvari/dev/oozie/conf -Doozie.log.dir=/Users/asasvari/dev/oozie/logs -Doozie.data.dir=/Users/asasvari/dev/oozie/data -Doozie.config.file=oozie-site.xml -Doozie.log4j.file=oozie-log4j.properties -Doozie.log4j.reload=10 -Djava.library.path= -cp /Users/asasvari/dev/oozie/embedded-oozie-server/**:/Users/asasvari/dev/oozie/embedded-oozie-server/dependency/**:/Users/asasvari/dev/oozie/lib/**:/Users/asasvari/dev/oozie/libtools/**:/Users/asasvari/dev/oozie/embedded-oozie-server
JETTY_OUT : /Users/asasvari/dev/oozie/logs/jetty.out
JETTY_PID_FILE : /Users/asasvari/dev/oozie/embedded-oozie-server/oozie.pid
OOZIE_CONFIG : /Users/asasvari/dev/oozie/conf
OOZIE_CONFIG_FILE : oozie-site.xml
OOZIE_DATA : /Users/asasvari/dev/oozie/data
OOZIE_HOME : /Users/asasvari/dev/oozie
OOZIE_LOG : /Users/asasvari/dev/oozie/logs
OOZIE_LOG4J_FILE : oozie-log4j.properties
OOZIE_LOG4J_RELOAD : 10
...
```

It returns a list of OS environment variables in the Oozie server.

### Displaying the Oozie server Java system properties

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -javasysprops
...
oozie.config.dir : /Users/asasvari/dev/oozie/conf
oozie.config.file : oozie-site.xml
oozie.data.dir : /Users/asasvari/dev/oozie/data
oozie.home.dir : /Users/asasvari/dev/oozie
oozie.log.dir : /Users/asasvari/dev/oozie/logs
oozie.log4j.file : oozie-log4j.properties
oozie.log4j.reload : 10
...
```

It returns a list of java system properties in the Oozie server.

### Displaying the Oozie server Instrumentation

Deprecated and by default disabled since 5.0.0.

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -instrumentation
COUNTERS
--------
callablequeue.executed : 1
callablequeue.queued : 1
commands.purge.executions : 1
...

VARIABLES
---------
configuration.action.types : [hive, shell, :START:, :FORK:, switch, spark, ssh, hive2, pig, :END:, email, distcp, :KILL:, sub-workflow, fs, java, :JOIN:, sqoop, map-reduce]
configuration.config.dir : /Users/rkanter/dev/oozie/conf
configuration.config.file : /Users/rkanter/dev/oozie/conf/oozie-site.xml
...

SAMPLERS
---------
callablequeue.queue.size : 0.0
callablequeue.threads.active : 0.0
jdbc.connections.active : 0.0
...

TIMERS
---------
callablequeue.time.in.queue
	own time standard deviation : -1.0
	own average time : 1
	own max time : 1
	own min time : 1
	total time standard deviation : -1.0
	total average time : 1
	total max time : 1
	total min time : 1
	ticks : 1
commands.purge.call
	own time standard deviation : -1.0
	own average time : 222
	own max time : 222
	own min time : 222
	total time standard deviation : -1.0
	total average time : 222
	total max time : 222
	total min time : 222
	ticks : 1
...
```

It returns the instrumentation from the Oozie server.  Keep in mind that timers and counters that the Oozie server
hasn't incremented yet will not show up.

**Note:** If Instrumentation is enabled, then Metrics is unavailable.

### Displaying the Oozie server Metrics

By default enabled since 5.0.0.

Example:


```
$ oozie admin -oozie http://localhost:11000/oozie -metrics
COUNTERS
--------
callablequeue.executed : 1
callablequeue.queued : 1
commands.purge.executions : 1
...

GAUGES
------
configuration.action.types : [hive, shell, :START:, :FORK:, switch, spark, ssh, hive2, pig, :END:, email, distcp, :KILL:, sub-workflow, fs, java, :JOIN:, sqoop, map-reduce]
configuration.config.dir : /Users/rkanter/dev/oozie/conf
configuration.config.file : /Users/rkanter/dev/oozie/conf/oozie-site.xml
...

TIMERS
------
callablequeue.time.in.queue.timer
	999th percentile : 4.0
	99th percentile : 4.0
	98th percentile : 4.0
	95th percentile : 4.0
	75th percentile : 4.0
	50th percentile : 4.0
	mean : 4.0
	max : 4.0
	min : 4.0
	count : 1
	standard deviation : 0.0
	15 minute rate : 0.0
	5 minute rate : 0.0
	1 minute rate : 0.0
	mean rate : 0.0
	duration units : milliseconds
	rate units : calls/millisecond
commands.purge.call.timer
	999th percentile : 260.0
	99th percentile : 260.0
	98th percentile : 260.0
	95th percentile : 260.0
	75th percentile : 260.0
	50th percentile : 260.0
	mean : 260.0
	max : 260.0
	min : 260.0
	count : 1
	standard deviation : 0.0
	15 minute rate : 0.0
	5 minute rate : 0.0
	1 minute rate : 0.0
	mean rate : 0.0
	duration units : milliseconds
	rate units : calls/millisecond
...

HISTOGRAMS
----------
callablequeue.queue.size.histogram
	999th percentile : 0.0
	99th percentile : 0.0
	98th percentile : 0.0
	95th percentile : 0.0
	75th percentile : 0.0
	50th percentile : 0.0
	mean : 0.0
	max : 0.0
	min : 0.0
	count : 13
	standard deviation : 0.0
callablequeue.threads.active.histogram
	999th percentile : 10.0
	99th percentile : 10.0
	98th percentile : 10.0
	95th percentile : 10.0
	75th percentile : 0.0
	50th percentile : 0.0
	mean : 0.8461538461538461
	max : 10.0
	min : 0.0
	count : 13
	standard deviation : 2.764240517940803
...
```

It returns the metrics from the Oozie server.  Keep in mind that timers and counters that the Oozie server
hasn't incremented yet will not show up.

**Note:** If Metrics is enabled, then Instrumentation is unavailable.

### Running purge command

Oozie admin purge command cleans up the Oozie Workflow/Coordinator/Bundle records based on the parameters.
The unit for parameters is day.
Purge command will delete the workflow records (wf=30) older than 30 days, coordinator records (coord=7) older than 7 days and
bundle records (bundle=7) older than 7 days. The limit (limit=10) defines, number of records to be fetch at a time. Turn
(oldCoordAction=true/false) `on/off` coordinator action record purging for long running coordinators. If any of the parameter is
not provided, then it will be taken from the `oozie-default/oozie-site` configuration.

Example:


```

$ oozie admin -purge wf=30\;coord=7\;bundle=7\;limit=10\;oldCoordAction=true

Purge command executed successfully

```

## Validate Operations

### Validating a Workflow XML

Example:


```
$ oozie validate myApp/workflow.xml
.
Error: E0701: XML schema error, workflow.xml, org.xml.sax.SAXParseException: cvc-complex-type.2.4.a:
       Invalid content was found starting with element 'xend'. One of '{"uri:oozie:workflow:0.1":decision,
       "uri:oozie:workflow:0.1":fork, "uri:oozie:workflow:0.1":join, "uri:oozie:workflow:0.1":kill,
       "uri:oozie:workflow:0.1":action, "uri:oozie:workflow:0.1":end}' is expected.
```


```
$ oozie validate /home/test/myApp/coordinator.xml
.
Error: E0701: XML schema error, coordinator.xml, org.xml.sax.SAXParseException; lineNumber: 4; columnNumber: 52; cvc-elt.1.a:
       Cannot find the declaration of element 'coordinator-app-invalid'.
```


```
$ oozie validate hdfs://localhost:8020/user/test/myApp/bundle.xml
.
Error: E0701: XML schema error, bundle.xml, org.xml.sax.SAXParseException: cvc-complex-type.2.4.b:
       The content of element 'bundle-app' is not complete. One of '{"uri:oozie:bundle:0.1":coordinator}' is expected.
```

It performs an XML Schema validation on the specified workflow, coordinator, bundle XML file.
The XML file can be a local file or in HDFS.

### Getting list of available sharelib
This command is used to get list of available sharelib.
If the name of the sharelib is passed as an argument (regex supported) then all corresponding files are also listed.


```
$ oozie admin -oozie http://localhost:11000/oozie -shareliblist
[Available ShareLib]
    oozie
    hive
    distcp
    hcatalog
    sqoop
    mapreduce-streaming
    pig

$ oozie admin -oozie http://localhost:11000/oozie -sharelib pig*
[Available ShareLib]
    pig
        hdfs://localhost:9000/user/purushah/share/lib/lib_20131114095729/pig/pig.jar
        hdfs://localhost:9000/user/purushah/share/lib/lib_20131114095729/pig/piggybank.jar
```

### Update system sharelib
This command makes the oozie server(s) to pick up the latest version of sharelib present
under oozie.service.WorkflowAppService.system.libpath directory based on the sharelib directory timestamp or reloads
the sharelib metafile if one is configured. The main purpose is to update the sharelib on the oozie server without restarting.


```
$ oozie admin -oozie http://localhost:11000/oozie -sharelibupdate
[ShareLib update status]
ShareLib update status]
    host = host1:8080
    status = Successful
    sharelibDirOld = hdfs://localhost:9000/user/purushah/share/lib/lib_20131114095729
    sharelibDirNew = hdfs://localhost:9000/user/purushah/share/lib/lib_20131120163343

    host = host2:8080
    status = Successful
    sharelibDirOld = hdfs://localhost:9000/user/purushah/share/lib/lib_20131114095729
    sharelibDirNew = hdfs://localhost:9000/user/purushah/share/lib/lib_20131120163343

    host = host3:8080
    status = Server not found
```

Sharelib update for metafile configuration.

```
$ oozie admin -oozie http://localhost:11000/oozie -sharelibupdate
[ShareLib update status]
    host = host1
    status = Successful
    sharelibMetaFile = hdfs://localhost:9000/user/purushah/sharelib_metafile.property
    sharelibMetaFileOldTimeStamp = Thu, 21 Nov 2013 00:40:04 GMT
    sharelibMetaFileNewTimeStamp = Thu, 21 Nov 2013 01:01:25 GMT

    host = host2
    status = Successful
    sharelibMetaFile = hdfs://localhost:9000/user/purushah/sharelib_metafile.property
    sharelibMetaFileOldTimeStamp = Thu, 21 Nov 2013 00:40:04 GMT
    sharelibMetaFileNewTimeStamp = Thu, 21 Nov 2013 01:01:25 GMT
```

<a name="SLAOperations"></a>
## SLA Operations

### Getting a list of SLA events

  This set of sla commands are deprecated as of Oozie 4.0 with a newer SLA monitoring system.

Example:


```
$ oozie sla -oozie http://localhost:11000/oozie -len 3
.
<sla-message>
  <event>
    <sequence-id>1</sequence-id>
    <registration>
      <sla-id>0000000-130130150445097-oozie-joe-C@1</sla-id>
      <app-type>COORDINATOR_ACTION</app-type>
      <app-name>aggregator-sla-app</app-name>
      <user>joe</user>
      <group />
      <parent-sla-id>null</parent-sla-id>
      <expected-start>2013-01-30T23:00Z</expected-start>
      <expected-end>2013-01-30T23:30Z</expected-end>
      <status-timestamp>2013-02-08T18:51Z</status-timestamp>
      <notification-msg>Notifying User for 2013-01-30T23:00Z nominal time</notification-msg>
      <alert-contact>www@yahoo.com</alert-contact>
      <dev-contact>abc@yahoo.com</dev-contact>
      <qa-contact>abc@yahoo.com</qa-contact>
      <se-contact>abc@yahoo.com</se-contact>
      <alert-percentage>80</alert-percentage>
      <alert-frequency>LAST_HOUR</alert-frequency>
      <upstream-apps />
      <job-status>CREATED</job-status>
      <job-data />
    </registration>
  </event>
  <event>
    <sequence-id>2</sequence-id>
    <status>
      <sla-id>0000000-130130150445097-oozie-joe-C@1</sla-id>
      <status-timestamp>2013-01-30T23:05Z</status-timestamp>
      <job-status>STARTED</job-status>
      <job-data />
    </status>
  </event>
  <event>
    <sequence-id>3</sequence-id>
    <status>
      <sla-id>0000000-130130150445097-oozie-joe-C@1</sla-id>
      <status-timestamp>2013-01-30T23:30Z</status-timestamp>
      <job-status>SUCCEEDED</job-status>
      <job-data />
    </status>
  </event>
  <last-sequence-id>3</last-sequence-id>
</sla-message>

```

The `offset` and `len` option specified the offset and number of sla events to display, default values are `1` and `100` respectively.

The `offset` corresponds to sequence ID of an event.

The max value of `len` limited by oozie server setting which defaults to '1000'. To get more than `1000` events, it is necessary to iterate based on the number of records you want.

The return message is XML format that can be easily consumed by SLA users.


### Getting the SLA event with particular sequenceID

* This feature is only supported in Oozie 2.0 or later.

Example:  Get the SLA event with sequenceID = 3  (Note that offset corresponds to sequence ID)


```
$ oozie sla -oozie http://localhost:11000/oozie -offset 2 -len 1
.
<sla-message>
  <event>
    <sequence-id>3</sequence-id>
    <status>
      <sla-id>0000000-130130150445097-oozie-joe-C@1</sla-id>
      <status-timestamp>2013-01-30T23:05Z</status-timestamp>
      <job-status>SUCCEEDED</job-status>
      <job-data />
    </status>
  </event>
  <last-sequence-id>3</last-sequence-id>
</sla-message>

```


### Getting information about SLA events using filter

* This feature is only supported in Oozie 2.0 or later.

Example:


```

$ oozie sla -filter jobid=0000000-130130150445097-oozie-joe-C@1\;appname=aggregator-sla-app -len 1 -oozie http://localhost:11000/oozie

<sla-message>
  <event>
    <sequence-id>1</sequence-id>
    <registration>
      <sla-id>0000000-130130150445097-oozie-joe-C@1</sla-id>
      <app-type>COORDINATOR_ACTION</app-type>
      <app-name>aggregator-sla-app</app-name>
      <user>joe</user>
      <group />
      <parent-sla-id>null</parent-sla-id>
      <expected-start>2010-01-01T02:00Z</expected-start>
      <expected-end>2010-01-01T03:00Z</expected-end>
      <status-timestamp>2013-01-30T23:05Z</status-timestamp>
      <notification-msg>Notifying User for 2010-01-01T01:00Z nominal time</notification-msg>
      <alert-contact>www@yahoo.com</alert-contact>
      <dev-contact>abc@yahoo.com</dev-contact>
      <qa-contact>abc@yahoo.com</qa-contact>
      <se-contact>abc@yahoo.com</se-contact>
      <alert-percentage>80</alert-percentage>
      <alert-frequency>LAST_HOUR</alert-frequency>
      <upstream-apps />
      <job-status>CREATED</job-status>
      <job-data />
    </registration>
  </event>
</sla-message>

```

A filter can be specified after all options.

The `filter`option syntax is: `[NAME=VALUE][\;NAME=VALUE]*`. Note `\` before semi-colon is for escape.

Valid filter names are:

   * jobid: workflow action/job id, coordinator action/job id
   * appname: the coordinator/workflow application name

The query will do an AND among all the filter names. The query will do an OR among all the filter values for the same
name. Multiple values must be specified as different name value pairs.


## Pig Operations

### Submitting a pig job through HTTP

Syntax:


```
$ oozie pig -file PIG-SCRIPT -config OOZIE-CONFIG [-Dkey=value] [-Pkey=value] [-X [-Dkey=value opts for Launcher/Job configuration] [Other opts to pass to Pig]]
```

Example:


```
$ oozie pig -file pigScriptFile -config job.properties -Dfs.default.name=hdfs://localhost:8020 -PINPUT=/user/me/in -POUTPUT=/user/me/out -X -Dmapred.job.queue.name=UserQueue -param_file params
.
job: 14-20090525161321-oozie-joe-W
.
$cat job.properties
fs.default.name=hdfs://localhost:8020
mapreduce.jobtracker.kerberos.principal=ccc
dfs.namenode.kerberos.principal=ddd
oozie.libpath=hdfs://localhost:8020/user/oozie/pig/lib/
```

The parameters for the job must be provided in a Java Properties file (.properties). jobtracker, namenode, libpath must be
specified in this file. pigScriptFile is a local file. All jar files (including pig jar file) and all other files needed by the pig
job (e.g., parameter file in above example) need to be uploaded onto HDFS under libpath beforehand. In addition to a parameter file,
specifying script parameters can be done via -Pkey=value. The workflow.xml will be created in Oozie server internally. Users can get
the workflow.xml from console or command line(-definition). The -D options passed after the -X will be placed into the generated
workflow's `<configuration> elements` (and make it to the configuration used by Pig); any other opts after -X will be
passed as-is to the invoked Pig program.
Multiple -D and -P arguments can be specified.

The job will be created and run right away.

## Hive Operations

### Submitting a hive job through HTTP

Syntax:


```
$ oozie hive -file HIVE-SCRIPT -config OOZIE-CONFIG [-Dkey=value] [-Pkey=value] [-X [-Dkey=value opts for Launcher/Job configuration] [Other opts to pass to Hive]]
```

Example:


```
$ oozie hive -file hiveScriptFile -config job.properties -Dfs.default.name=hdfs://localhost:8020 -PINPUT=/user/me/in -POUTPUT=/user/me/out -X -Dmapred.job.queue.name=UserQueue -v
.
job: 14-20090525161321-oozie-joe-W
.
$cat job.properties
fs.default.name=hdfs://localhost:8020
mapreduce.jobtracker.kerberos.principal=ccc
dfs.namenode.kerberos.principal=ddd
oozie.libpath=hdfs://localhost:8020/user/oozie/hive/lib/
```

The parameters for the job must be provided in a Java Properties file (.properties). jobtracker, namenode, libpath must be
specified in this file. hiveScriptFile is a local file. All jar files (including hive jar file) and all other files needed by the
hive job need to be uploaded onto HDFS under libpath beforehand. Specifying script parameters can be done via -Pkey=value. The
workflow.xml will be created in Oozie server internally. Users can get the workflow.xml from console or command line(-definition).
The -D options passed after the -X will be placed into the generated workflow's `<configuration> elements` (and make it
to the configuration used by Hive); any other opts after -X will be passed as-is to the invoked Hive program.
Multiple -D and -P arguments can be specified.

The job will be created and run right away.

## Sqoop Operations

### Submitting a sqoop job through HTTP

Syntax:


```
$ oozie sqoop [-Dkey=value] -command completeSqoopCommand -config OOZIE-CONFIG [-X [-Dkey=value opts for Launcher/Job configuration]]
```

Example:


```
$ oozie sqoop -oozie http://localhost:11000/oozie -Dfs.default.name=hdfs://localhost:8020 -command import --connect jdbc:mysql://localhost:3306/oozie --username oozie --password oozie --table WF_JOBS --target-dir '/user/${wf:user()}/${examplesRoot}/output-data/sqoop' -m 1 -config job.properties -X -Dmapred.job.queue.name=default
.
job: 14-20090525161322-oozie-joe-W
.
```

Sqoop Freeform Example:

```
$ oozie sqoop -oozie http://localhost:11000/oozie -command import --connect jdbc:mysql://localhost:3306/oozie --username oozie --password oozie --query "SELECT a.id FROM WF_JOBS a WHERE \$CONDITIONS" --target-dir '/user/${wf:user()}/${examplesRoot}/output-data/sqoop' -m 1 -config job.properties -X -Dmapred.job.queue.name=default
.
job: 14-20090525161321-oozie-joe-W
.
$cat job.properties
fs.default.name=hdfs://localhost:8020
mapreduce.jobtracker.kerberos.principal=ccc
dfs.namenode.kerberos.principal=ddd
oozie.libpath=hdfs://localhost:8020/user/oozie/sqoop/lib/
```

The parameters for the job must be provided in a Java Properties file (.properties). jobtracker, namenode,
libpath must be specified in this file. All jar files (including sqoop jar file) and all other files needed by the
sqoop job need to be uploaded onto HDFS under libpath beforehand. The workflow.xml will be created in Oozie server
internally. Users can get the workflow.xml from console or command line(-definition).
The -D options passed after the -X will be placed into the generated workflow's `<configuration> elements`
(and make it to the configuration used by Sqoop); Multiple -D arguments can be specified.

The job will be created and run right away.

Note: in the freeform query example, the "select" query itself must be double quoted and the "$" sign in the query is
properly escaped by "\". And all other variables containing "$" within sqoop command are escaped by single quoting the
variable itself like the value of "--target-dir". All the "-D" arguments before "-X" that are overriding given property
must be placed before the "-command" argument.

## Info Operations

The Info sub-command provides a convenient place for Oozie to display misc information.

### Getting a list of time zones

Example:


```
$ oozie info -timezones
.
The format is "SHORT_NAME (ID)"
Give the ID to the -timezone argument
GMT offsets can also be used (e.g. GMT-07:00, GMT-0700, GMT+05:30, GMT+0530)
Available Time Zones :
      SST (Pacific/Midway)
      NUT (Pacific/Niue)
      SST (Pacific/Pago_Pago)
      SST (Pacific/Samoa)
      SST (US/Samoa)
      HAST (America/Adak)
      HAST (America/Atka)
      HST (HST)
      ...
```

The `-timezones` option will print out a (long) list of all available time zones.

These IDs (the text in the parentheses) are what should be used for the `-timezone TIME_ZONE_ID` option in the `job`
and `jobs` sub-commands

## Map-reduce Operations

### Submitting a map-reduce job

Example:


```
$ oozie mapreduce -oozie http://localhost:11000/oozie -config job.properties
```

The parameters must be in the Java Properties file (.properties). This file must be specified for a map-reduce job.
The properties file must specify the `mapred.mapper.class`, `mapred.reducer.class`, `mapred.input.dir`, `mapred.output.dir`,
`oozie.libpath`, `mapred.job.tracker`, and `fs.default.name` properties.

The map-reduce job will be created and submitted. All jar files and all other files needed by the mapreduce job need to be uploaded onto HDFS under libpath beforehand. The workflow.xml will be created in Oozie server internally. Users can get the workflow.xml from console or command line(-definition).

## Getting Oozie diagnostics bundles

A tool that collects a diagnostic bundle of information from Oozie. Collected information includes available Oozie ShareLibs;
effective configuration, system properties, environment variables, thread dump of the Oozie server; instrumentation logs;
dump of queued commands; details about workflow(s) (such as workflow xml, logs, current state, job properties), coordinator(s)
and bundle(s). When retrieving coordinator information, the tool will also try to fetch information about related child workflow(s).

Syntax:


```
$ oozie-diag-bundle-collector.sh [-jobs <id ...>] [-maxchildactions <n>]
       [-numbundles <n>] [-numcoordinators <n>] [-numworkflows <n>] -oozie
       <url> -output <dir>
```

where
 -jobs \<id ...\>         Detailed information on the given job IDs will be
                        collected (default: none)
 -maxchildactions \<n\>   Maximum number of Workflow or Coordinator actions
                        that will be collected (default: 10)
 -numbundles \<n\>        Detailed information on the last n Bundles will be
                        collected (default: 0)
 -numcoordinators \<n\>   Detailed information on the last n Coordinators
                        will be collected (default: 0)
 -numworkflows \<n\>      Detailed information on the last n workflows will
                        be collected (default: 0)
 -oozie \<url\>           Required: Oozie URL (or specify with OOZIE_URL env
                        var)
 -output \<dir\>          Required: Directory to output the zip file

Example:


```
$ oozie-diag-bundle-collector.sh -jobs 0000001-170918144116149-oozie-test-W -oozie http://localhost:11000/oozie -output diag

...

Using Temporary Directory: /var/folders/9q/f8p_r6gj0wbck49_dc092q_m0000gp/T/1505748796767-0
Getting Sharelib Information...Done
Getting Configuration...Done
Getting OS Environment Variables...Done
Getting Java System Properties...Done
Getting Queue Dump...Done
Getting Thread Dump...Done
Getting Instrumentation...Skipping (Instrumentation is unavailable)
Getting Metrics...Done
Getting Details for 0000001-170918144116149-oozie-test-W...Done
Creating Zip File: /var/lib/oozie/diag/oozie-diag-bundle-1505748797206.zip...Done
```

Before executing the command, make sure OOZIE_HOME environment variable is set correctly. If Oozie authorization is enabled, then
the user must be an admin user in order to perform admin operations (for example getting a Thread dump of the Oozie server). If the
output directory does not exist, the tool will create it and store generated bundle there.

[::Go back to Oozie Documentation Index::](index.html)


