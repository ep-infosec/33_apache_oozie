

[::Go back to Oozie Documentation Index::](index.html)

-----

# Oozie Coordinator Specification

The goal of this document is to define a coordinator engine system specialized in submitting workflows based on time and data triggers.

<!-- MACRO{toc|fromDepth=1|toDepth=4} -->

## Changelog

**03/JUL/2013**

   * Appendix A, Added new coordinator schema 0.4, sla schema 0.2 and changed schemas ordering to newest first

**07/JAN/2013**

   * 6.8 Added section on new EL functions for datasets defined with HCatalog

**26/JUL/2012**

   * Appendix A, updated XML schema 0.4 to include `parameters` element
   * 6.5 Updated to mention about `parameters` element as of schema 0.4

**23/NOV/2011:**

   * Update execution order typo

**05/MAY/2011:**

   * Update coordinator schema 0.2

**09/MAR/2011:**

   * Update coordinator status

**02/DEC/2010:**

   * Update coordinator done-flag

**26/AUG/2010:**

   * Update coordinator rerun

**09/JUN/2010:**

   * Clean up unsupported functions

**02/JUN/2010:**

   * Update all EL functions in CoordFunctionalSpec with "coord:" prefix

**02/OCT/2009:**

   * Added Appendix A, Oozie Coordinator XML-Schema
   * Change #5.3., Datasets definition supports 'include' element

**29/SEP/2009:**

   * Change #4.4.1, added `${coord:endOfDays(int n)}` EL function
   * Change #4.4.2, added `${coord:endOfMonths(int n)}` EL function

**11/SEP/2009:**

   * Change #6.6.4. `${coord:tzOffset()}` EL function now returns offset in minutes. Added more explanation on behavior
   * Removed 'oozie' URL from action workflow invocation, per arch review feedback coord&wf run on the same instance

**07/SEP/2009:**

   * Full rewrite of sections #4 and #7
   * Added sections #6.1.7, #6.6.2, #6.6.3 & #6.6.4
   * Rewording through the spec definitions
   * Updated all examples and syntax to latest changes

**03/SEP/2009:**

   * Change #2. Definitions. Some rewording in the definitions
   * Change #6.6.4. Replaced `${coord:next(int n)}` with `${coord:version(int n)}` EL Function
   * Added #6.6.5. Dataset Instance Resolution for Instances Before the Initial Instance

## 1. Coordinator Overview

Users typically run map-reduce, hadoop-streaming, hdfs and/or Pig jobs on the grid. Multiple of these jobs can be combined to form a workflow job. [Oozie, Hadoop Workflow System](https://issues.apache.org/jira/browse/HADOOP-5303) defines a workflow system that runs such jobs.

Commonly, workflow jobs are run based on regular time intervals and/or data availability. And, in some cases, they can be triggered by an external event.

Expressing the condition(s) that trigger a workflow job can be modeled as a predicate that has to be satisfied. The workflow job is started after the predicate is satisfied. A predicate can reference to data, time and/or external events. In the future, the model can be extended to support additional event types.

It is also necessary to connect workflow jobs that run regularly, but at different time intervals. The outputs of multiple subsequent runs of a workflow become the input to the next workflow. For example, the outputs of last 4 runs of a workflow that runs every 15 minutes become the input of another workflow that runs every 60 minutes. Chaining together these workflows result it is referred as a data application pipeline.

The Oozie **Coordinator** system allows the user to define and execute recurrent and interdependent workflow jobs (data application pipelines).

Real world data application pipelines have to account for reprocessing, late processing, catchup, partial processing, monitoring, notification and SLAs.

This document defines the functional specification for the Oozie Coordinator system.

## 2. Definitions

**Actual time:** The actual time indicates the time when something actually happens.

**Nominal time:** The nominal time specifies the time when something should happen. In theory the nominal time and the actual time should match, however, in practice due to delays the actual time may occur later than the nominal time.

**Dataset:** Collection of data referred to by a logical name. A dataset normally has several instances of data and each
one of them can be referred individually. Each dataset instance is represented by a unique set of URIs.

**Synchronous Dataset:** Synchronous datasets instances are generated at fixed time intervals and there is a dataset
instance associated with each time interval. Synchronous dataset instances are identified by their nominal time.
For example, in the case of a HDFS based dataset, the nominal time would be somewhere in the file path of the
dataset instance: `hdfs://foo:8020/usr/logs/2009/04/15/23/30`. In the case of HCatalog table partitions, the nominal time
would be part of some partition values: `hcat://bar:8020/mydb/mytable/year=2009;month=04;dt=15;region=us`.

**Coordinator Action:** A coordinator action is a workflow job that is started when a set of conditions are met (input dataset instances are available).

**Coordinator Application:** A coordinator application defines the conditions under which coordinator actions should be created (the frequency) and when the actions can be started. The coordinator application also defines a start and an end time. Normally, coordinator applications are parameterized. A Coordinator application is written in XML.

**Coordinator Job:** A coordinator job is an executable instance of a coordination definition. A job submission is done by submitting a job configuration that resolves all parameters in the application definition.

**Data pipeline:** A data pipeline is a connected set of coordinator applications that consume and produce interdependent datasets.

**Coordinator Definition Language:** The language used to describe datasets and coordinator applications.

**Coordinator Engine:** A system that executes coordinator jobs.

## 3. Expression Language for Parameterization

Coordinator application definitions can be parameterized with variables, built-in constants and built-in functions.

At execution time all the parameters are resolved into concrete values.

The parameterization of workflow definitions it done using JSP Expression Language syntax from the [JSP 2.0 Specification (JSP.2.3)](http://jcp.org/aboutJava/communityprocess/final/jsr152/index.html), allowing not only to support variables as parameters but also functions and complex expressions.

EL expressions can be used in XML attribute values and XML text element values. They cannot be used in XML element and XML attribute names.

Refer to section #6.5 'Parameterization of Coordinator Applications' for more details.

## 4. Datetime, Frequency and Time-Period Representation

Oozie processes coordinator jobs in a fixed timezone with no DST (typically `UTC`), this timezone is referred as 'Oozie
processing timezone'.

The Oozie processing timezone is used to resolve coordinator jobs start/end times, job pause times and the initial-instance
of datasets. Also, all coordinator dataset instance URI templates are resolved to a datetime in the Oozie processing
time-zone.

All the datetimes used in coordinator applications and job parameters to coordinator applications must be specified
in the Oozie processing timezone. If Oozie processing timezone is `UTC`, the qualifier is  **Z**. If Oozie processing
time zone is other than `UTC`, the qualifier must be the GMT offset, `(+/-)####`.

For example, a datetime in `UTC`  is `2012-08-12T00:00Z`, the same datetime in `GMT+5:30` is `2012-08-12T05:30+0530`.

For simplicity, the rest of this specification uses `UTC` datetimes.

<a name="datetime"></a>
### 4.1. Datetime

If the Oozie processing timezone is `UTC`, all datetime values are always in
[UTC](http://en.wikipedia.org/wiki/Coordinated_Universal_Time) down to a minute precision, 'YYYY-MM-DDTHH:mmZ'.

For example `2009-08-10T13:10Z` is August 10th 2009 at 13:10 UTC.

If the Oozie processing timezone is a GMT offset `GMT(+/-)####`, all datetime values are always in
[ISO 8601](http://en.wikipedia.org/wiki/ISO_8601) in the corresponding GMT offset down to a minute precision,
'YYYY-MM-DDTHH:mmGMT(+/-)####'.

For example `2009-08-10T13:10+0530` is August 10th 2009 at 13:10 GMT+0530, India timezone.

#### 4.1.1 End of the day in Datetime Values

It is valid to express the end of day as a '24:00' hour (i.e. `2009-08-10T24:00Z`).

However, for all calculations and display, Oozie resolves such dates as the zero hour of the following day
(i.e. `2009-08-11T00:00Z`).

### 4.2. Timezone Representation

There is no widely accepted standard to identify timezones.

Oozie Coordinator will understand the following timezone identifiers:

   * Generic NON-DST timezone identifier: `GMT[+/-]##:##` (i.e.: GMT+05:30)
   * UTC timezone identifier: `UTC` (i.e.: 2009-06-06T00:00Z)
   * ZoneInfo identifiers, with DST support, understood by Java JDK (about 600 IDs) (i.e.: America/Los_Angeles)

Due to DST shift from PST to PDT, it is preferred that GMT, UTC or Region/City timezone notation is used in
favor of direct three-letter ID (PST, PDT, BST, etc.). For example, America/Los_Angeles switches from PST to PDT
at a DST shift. If used directly, PST will not handle DST shift when time is switched to PDT.

Oozie Coordinator must provide a tool for developers to list all supported timezone identifiers.

### 4.3. Timezones and Daylight-Saving

While Oozie coordinator engine works in a fixed timezone with no DST (typically `UTC`), it provides DST support for coordinator applications.

The baseline datetime for datasets and coordinator applications are expressed in UTC. The baseline datetime is the time of the first occurrence.

Datasets and coordinator applications also contain a timezone indicator.

The use of UTC as baseline enables a simple way of mix and matching datasets and coordinator applications that use a different timezone by just adding the timezone offset.

The timezone indicator enables Oozie coordinator engine to properly compute frequencies that are daylight-saving sensitive. For example: a daily frequency can be 23, 24 or 25 hours for timezones that observe daylight-saving. Weekly and monthly frequencies are also affected by this as the number of hours in the day may change.

Section #7 'Handling Timezones and Daylight Saving Time' explains how coordinator applications can be written to handle timezones and daylight-saving-time properly.

### 4.4. Frequency and Time-Period Representation

Frequency is used to capture the periodic intervals at which datasets that are produced, and coordinator applications are scheduled to run.

This time periods representation is also used to specify non-recurrent time-periods, for example a timeout interval.

For datasets and coordinator applications the frequency time-period is applied `N` times to the baseline datetime to compute recurrent times.

Frequency is always expressed in minutes.

Because the number of minutes in day may vary for timezones that observe daylight saving time, constants cannot be use to express frequencies greater than a day for datasets and coordinator applications for such timezones. For such uses cases, Oozie coordinator provides 2 EL functions, `${coord:days(int n)}` and `${coord:months(int n)}`.

Frequencies can be expressed using EL constants and EL functions that evaluate to an positive integer number.

Coordinator Frequencies can also be expressed using cron syntax.

**<font color="#008000"> Examples: </font>**

| **EL Constant** | **Value** | **Example** |
| --- | --- | --- |
| `${coord:minutes(int n)}` | _n_ | `${coord:minutes(45)}` --> `45` |
| `${coord:hours(int n)}` | _n * 60_ | `${coord:hours(3)}` --> `180` |
| `${coord:days(int n)}` | _variable_ | `${coord:days(2)}` --> minutes in 2 full days from the current date |
| `${coord:months(int n)}` | _variable_ | `${coord:months(1)}` --> minutes in a 1 full month from the current date |
| `${cron syntax}` | _variable_ | `${0,10 15 * * 2-6}` --> a job that runs every weekday at 3:00pm and 3:10pm UTC time|

Note that, though `${coord:days(int n)}` and `${coord:months(int n)}` EL functions are used to calculate minutes precisely including
variations due to daylight saving time for Frequency representation, when specified for coordinator timeout interval, one day is
calculated as 24 hours and one month is calculated as 30 days for simplicity.

#### 4.4.1. The coord:days(int n) and coord:endOfDays(int n) EL functions

The `${coord:days(int n)}` and `${coord:endOfDays(int n)}` EL functions should be used to handle day based frequencies.

Constant values should not be used to indicate a day based frequency (every 1 day, every 1 week, etc) because the number of hours in
every day is not always the same for timezones that observe daylight-saving time.

It is a good practice to use always these EL functions instead of using a constant expression (i.e. `24 * 60`) even if the timezone
for which the application is being written for does not support daylight saving time. This makes application foolproof to country
legislation changes and also makes applications portable across timezones.

##### 4.4.1.1. The coord:days(int n) EL function

The `${coord:days(int n)}` EL function returns the number of minutes for 'n' complete days starting with the day of the specified nominal time for which the computation is being done.

The `${coord:days(int n)}` EL function includes **all** the minutes of the current day, regardless of the time of the day of the current nominal time.

**<font color="#008000"> Examples: </font>**

| **Starting Nominal UTC time** | **Timezone** | **Usage**  | **Value** | **First Occurrence** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2009-01-01T08:00Z` | `UTC` | `${coord:days(1)}` | 1440 | `2009-01-01T08:00Z` | total minutes on 2009JAN01 UTC time |
| `2009-01-01T08:00Z` | `America/Los_Angeles` | `${coord:days(1)}` | 1440 | `2009-01-01T08:00Z` | total minutes in 2009JAN01 PST8PDT time |
| `2009-01-01T08:00Z` | `America/Los_Angeles` | `${coord:days(2)}` | 2880 | `2009-01-01T08:00Z` | total minutes in 2009JAN01 and 2009JAN02 PST8PDT time |
| |||||
| `2009-03-08T08:00Z` | `UTC` | `${coord:days(1)}` | 1440 | `2009-03-08T08:00Z` | total minutes on 2009MAR08 UTC time |
| `2009-03-08T08:00Z` | `Europe/London` | `${coord:days(1)}` | 1440 | `2009-03-08T08:00Z` | total minutes in 2009MAR08 BST1BDT time |
| `2009-03-08T08:00Z` | `America/Los_Angeles` | `${coord:days(1)}` | 1380 | `2009-03-08T08:00Z` | total minutes in 2009MAR08 PST8PDT time <br/> (2009MAR08 is DST switch in the US) |
| `2009-03-08T08:00Z` | `UTC` | `${coord:days(2)}` | 2880 | `2009-03-08T08:00Z` | total minutes in 2009MAR08 and 2009MAR09 UTC time |
| `2009-03-08T08:00Z` | `America/Los_Angeles` | `${coord:days(2)}` | 2820 | `2009-03-08T08:00Z` | total minutes in 2009MAR08 and 2009MAR09 PST8PDT time <br/> (2009MAR08 is DST switch in the US) |
| `2009-03-09T08:00Z` | `America/Los_Angeles` | `${coord:days(1)}` | 1440 | `2009-03-09T07:00Z` | total minutes in 2009MAR09 PST8PDT time <br/> (2009MAR08 is DST ON, frequency tick is earlier in UTC) |

For all these examples, the first occurrence of the frequency will be at `08:00Z` (UTC time).

##### 4.4.1.2. The coord:endOfDays(int n) EL function

The `${coord:endOfDays(int n)}` EL function is identical to the `${coord:days(int n)}` except that it shifts the first occurrence to the end of the day for the specified timezone before computing the interval in minutes.

**<font color="#008000"> Examples: </font>**

| **Starting Nominal UTC time** | **Timezone** | **Usage**  | **Value** | **First Occurrence** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2009-01-01T08:00Z` | `UTC` | `${coord:endOfDays(1)}` | 1440 | `2009-01-02T00:00Z` | first occurrence in 2009JAN02 00:00 UTC time, <br/> first occurrence shifted to the end of the UTC day |
| `2009-01-01T08:00Z` | `America/Los_Angeles` | `${coord:endOfDays(1)}` | 1440 | `2009-01-02T08:00Z` | first occurrence in 2009JAN02 08:00 UTC time, <br/> first occurrence shifted to the end of the PST8PDT day |
| `2009-01-01T08:01Z` | `America/Los_Angeles` | `${coord:endOfDays(1)}` | 1440 | `2009-01-02T08:00Z` | first occurrence in 2009JAN02 08:00 UTC time, <br/> first occurrence shifted to the end of the PST8PDT day |
| `2009-01-01T18:00Z` | `America/Los_Angeles` | `${coord:endOfDays(1)}` | 1440 | `2009-01-02T08:00Z` | first occurrence in 2009JAN02 08:00 UTC time, <br/> first occurrence shifted to the end of the PST8PDT day |
| |||||
| `2009-03-07T09:00Z` | `America/Los_Angeles` | `${coord:endOfDays(1)}` | 1380 | `2009-03-08T08:00Z` | first occurrence in 2009MAR08 08:00 UTC time <br/> first occurrence shifted to the end of the PST8PDT day |
| `2009-03-08T07:00Z` | `America/Los_Angeles` | `${coord:endOfDays(1)}` | 1440 | `2009-03-08T08:00Z` | first occurrence in 2009MAR08 08:00 UTC time <br/> first occurrence shifted to the end of the PST8PDT day |
| `2009-03-09T07:00Z` | `America/Los_Angeles` | `${coord:endOfDays(1)}` | 1440 | `2009-03-10T07:00Z` | first occurrence in 2009MAR10 07:00 UTC time <br/> (2009MAR08 is DST switch in the US), <br/> first occurrence shifted to the end of the PST8PDT day |


```
<coordinator-app name="hello-coord" frequency="${coord:days(1)}"
                  start="2009-01-02T08:00Z" end="2009-01-04T08:00Z" timezone="America/Los_Angeles"
                 xmlns="uri:oozie:coordinator:0.5">
      <controls>
        <timeout>10</timeout>
        <concurrency>${concurrency_level}</concurrency>
        <execution>${execution_order}</execution>
        <throttle>${materialization_throttle}</throttle>
      </controls>

      <datasets>
       <dataset name="din" frequency="${coord:endOfDays(1)}"
                initial-instance="2009-01-02T08:00Z" timezone="America/Los_Angeles">
         <uri-template>${baseFsURI}/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
        </dataset>
       <dataset name="dout" frequency="${coord:minutes(30)}"
                initial-instance="2009-01-02T08:00Z" timezone="UTC">
         <uri-template>${baseFsURI}/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
        </dataset>
      </datasets>

      <input-events>
         <data-in name="input" dataset="din">
				<instance>${coord:current(0)}</instance>
         </data-in>
      </input-events>

      <output-events>
         <data-out name="output" dataset="dout">
				<instance>${coord:current(1)}</instance>
         </data-out>
      </output-events>

      <action>
        <workflow>
          <app-path>${wf_app_path}</app-path>
          <configuration>
              <property>
              <name>wfInput</name>
              <value>${coord:dataIn('input')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('output')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
 </coordinator-app>
```

#### 4.4.2. The coord:months(int n) and coord:endOfMonths(int n) EL functions

The `${coord:months(int n)}` and `${coord:endOfMonths(int n)}` EL functions should be used to handle month based frequencies.

Constant values cannot be used to indicate a month based frequency because the number of days in a month changes month to month and on leap years; plus the number of hours in every day of the month are not always the same for timezones that observe daylight-saving time.

##### 4.4.2.1. The coord:months(int n) EL function

The `${coord:months(int n)}` EL function returns the number of minutes for 'n' complete months starting with the month of the current nominal time for which the computation is being done.

The `${coord:months(int n)}` EL function includes **all** the minutes of the current month, regardless of the day of the month of the current nominal time.

**<font color="#008000"> Examples: </font>**

| **Starting Nominal UTC time** | **Timezone** | **Usage**  | **Value** | **First Occurrence** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2009-01-01T08:00Z` | `UTC` | `${coord:months(1)}` | 44640 | `2009-01-01T08:00Z` |total minutes for 2009JAN UTC time |
| `2009-01-01T08:00Z` | `America/Los_Angeles` | `${coord:months(1)}` | 44640 | `2009-01-01T08:00Z` | total minutes in 2009JAN PST8PDT time |
| `2009-01-01T08:00Z` | `America/Los_Angeles` | `${coord:months(2)}` | 84960 | `2009-01-01T08:00Z` | total minutes in 2009JAN and 2009FEB PST8PDT time |
| |||||
| `2009-03-08T08:00Z` | `UTC` | `${coord:months(1)}` | 44640 | `2009-03-08T08:00Z` | total minutes on 2009MAR UTC time |
| `2009-03-08T08:00Z` | `Europe/London` | `${coord:months(1)}` | 44580 | `2009-03-08T08:00Z` | total minutes in 2009MAR BST1BDT time <br/> (2009MAR29 is DST switch in Europe) |
| `2009-03-08T08:00Z` | `America/Los_Angeles` | `${coord:months(1)}` | 44580 | `2009-03-08T08:00Z` | total minutes in 2009MAR PST8PDT time <br/> (2009MAR08 is DST switch in the US) |
| `2009-03-08T08:00Z` | `UTC` | `${coord:months(2)}` | 87840 | `2009-03-08T08:00Z` | total minutes in 2009MAR and 2009APR UTC time |
| `2009-03-08T08:00Z` | `America/Los_Angeles` | `${coord:months(2)}` | 87780 | `2009-03-08T08:00Z` | total minutes in 2009MAR and 2009APR PST8PDT time <br/> (2009MAR08 is DST switch in US) |

##### 4.4.2.2. The coord:endOfMonths(int n) EL function

The `${coord:endOfMonths(int n)}` EL function is identical to the `${coord:months(int n)}` except that it shifts the first occurrence to the end of the month for the specified timezone before computing the interval in minutes.

**<font color="#008000"> Examples: </font>**

| **Starting Nominal UTC time** | **Timezone** | **Usage**  | **Value** | **First Occurrence** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2009-01-01T00:00Z` | `UTC` | `${coord:endOfMonths(1)}` | 40320 | `2009-02-01T00:00Z` | first occurrence in 2009FEB 00:00 UTC time |
| `2009-01-01T08:00Z` | `UTC` | `${coord:endOfMonths(1)}` | 40320 | `2009-02-01T00:00Z` | first occurrence in 2009FEB 00:00 UTC time |
| `2009-01-31T08:00Z` | `UTC` | `${coord:endOfMonths(1)}` | 40320 | `2009-02-01T00:00Z` | first occurrence in 2009FEB 00:00 UTC time |
| `2009-01-01T08:00Z` | `America/Los_Angeles` | `${coord:endOfMonths(1)}` | 40320 | `2009-02-01T08:00Z` | first occurrence in 2009FEB 08:00 UTC time |
| `2009-02-02T08:00Z` | `America/Los_Angeles` | `${coord:endOfMonths(1)}` | 44580  | `2009-03-01T08:00Z` | first occurrence in 2009MAR 08:00 UTC time |
| `2009-02-01T08:00Z` | `America/Los_Angeles` | `${coord:endOfMonths(1)}` | 44580  | `2009-03-01T08:00Z` | first occurrence in 2009MAR 08:00 UTC time |


```
<coordinator-app name="hello-coord" frequency="${coord:months(1)}"
                  start="2009-01-02T08:00Z" end="2009-04-02T08:00Z" timezone="America/Los_Angeles"
                 xmlns="uri:oozie:coordinator:0.5">
      <controls>
        <timeout>10</timeout>
        <concurrency>${concurrency_level}</concurrency>
        <execution>${execution_order}</execution>
        <throttle>${materialization_throttle}</throttle>
      </controls>

      <datasets>
       <dataset name="din" frequency="${coord:endOfMonths(1)}"
                initial-instance="2009-01-02T08:00Z" timezone="America/Los_Angeles">
         <uri-template>${baseFsURI}/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
        </dataset>
       <dataset name="dout" frequency="${coord:minutes(30)}"
                initial-instance="2009-01-02T08:00Z" timezone="UTC">
         <uri-template>${baseFsURI}/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
        </dataset>
      </datasets>

      <input-events>
         <data-in name="input" dataset="din">
				<instance>${coord:current(0)}</instance>
         </data-in>
      </input-events>

      <output-events>
         <data-out name="output" dataset="dout">
				<instance>${coord:current(1)}</instance>
         </data-out>
      </output-events>

      <action>
        <workflow>
          <app-path>${wf_app_path}</app-path>
          <configuration>
              <property>
              <name>wfInput</name>
              <value>${coord:dataIn('input')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('output')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
 </coordinator-app>
```

#### 4.4.3. The coord:endOfWeeks(int n) EL function

The `${coord:endOfWeeks(int n)}`  EL function shifts the first occurrence to the start of the week for the specified
timezone before computing the interval in minutes. The start of the week depends on the Java's implementation of
[Calendar.getFirstDayOfWeek()](https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html#getFirstDayOfWeek--)
 i.e. first day of the week is SUNDAY in the U.S., MONDAY in France.

**<font color="#008000"> Examples: </font>**

| **Starting Nominal UTC time** | **Timezone** | **Usage**  | **Value** | **First Occurrence** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2017-01-04T00:00Z` | `UTC` | `${coord:endOfWeeks(1)}` | 10080 | `2017-01-08T00:00Z` | first occurrence on 2017JAN08 08:00 UTC time |
| `2017-01-04T08:00Z` | `UTC` | `${coord:endOfWeeks(1)}` | 10080 | `2017-01-08T08:00Z` | first occurrence on 2017JAN08 08:00 UTC time |
| `2017-01-06T08:00Z` | `UTC` | `${coord:endOfWeeks(1)}` | 10080 | `2017-01-08T08:00Z` | first occurrence on 2017JAN08 08:00 UTC time |
| `2017-01-04T08:00Z` | `America/Los_Angeles` | `${coord:endOfWeeks(1)}` | 10080 | `2017-01-08T08:00Z` | first occurrence in 2017JAN08 08:00 UTC time |
| `2017-01-06T08:00Z` | `America/Los_Angeles` | `${coord:endOfWeeks(1)}` | 10080 | `2017-01-08T08:00Z` | first occurrence in 2017JAN08 08:00 UTC time |


```
<coordinator-app name="hello-coord" frequency="${coord:endOfWeeks(1)}"
                  start="2017-01-04T08:00Z" end="2017-12-31T08:00Z" timezone="America/Los_Angeles"
                 xmlns="uri:oozie:coordinator:0.5">
      <controls>
        <timeout>10</timeout>
        <concurrency>${concurrency_level}</concurrency>
        <execution>${execution_order}</execution>
        <throttle>${materialization_throttle}</throttle>
      </controls>

      <datasets>
       <dataset name="din" frequency="${coord:endOfWeeks(1)}"
                initial-instance="2017-01-01T08:00Z" timezone="America/Los_Angeles">
         <uri-template>${baseFsURI}/${YEAR}/${MONTH}/${DAY}</uri-template>
        </dataset>
       <dataset name="dout" frequency="${coord:endOfWeeks(1)}"
                initial-instance="2017-01-01T08:00Z" timezone="UTC">
         <uri-template>${baseFsURI}/${YEAR}/${MONTH}/${DAY}</uri-template>
        </dataset>
      </datasets>

      <input-events>
         <data-in name="input" dataset="din">
            <instance>${coord:current(0)}</instance>
         </data-in>
      </input-events>

      <output-events>
         <data-out name="output" dataset="dout">
            <instance>${coord:current(1)}</instance>
         </data-out>
      </output-events>

      <action>
        <workflow>
          <app-path>${wf_app_path}</app-path>
          <configuration>
              <property>
              <name>wfInput</name>
              <value>${coord:dataIn('input')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('output')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
 </coordinator-app>
```

#### 4.4.4. Cron syntax in coordinator frequency

Oozie has historically allowed only very basic forms of scheduling: You could choose
to run jobs separated by a certain number of minutes, hours, days or weeks. That's
all. This works fine for processes that need to run continuously all year like building
a search index to power an online website.

However, there are a lot of cases that don't fit this model. For example, maybe you
want to export data to a reporting system used during the day by business analysts.
It would be wasteful to run the jobs when no analyst is going to take advantage of
the new information, such as overnight. You might want a policy that says "only run
these jobs on weekdays between 6AM and 8PM". Previous versions of Oozie didn't support
this kind of complex scheduling policy without requiring multiple identical coordinators.
Cron-scheduling improves the user experience in this area, allowing for a lot more flexibility.

Cron is a standard time-based job scheduling mechanism in unix-like operating system. It is used extensively by system
administrators to setup jobs and maintain software environment. Cron syntax generally consists of five fields, minutes,
hours, date of month, month, and day of week respectively although multiple variations do exist.


```
<coordinator-app name="cron-coord" frequency="0/10 1/2 ** ** *" start="${start}" end="${end}" timezone="UTC"
                 xmlns="uri:oozie:coordinator:0.2">
        <action>
        <workflow>
            <app-path>${workflowAppUri}</app-path>
            <configuration>
                <property>
                    <name>jobTracker</name>
                    <value>${jobTracker}</value>
                </property>
                <property>
                    <name>nameNode</name>
                    <value>${nameNode}</value>
                </property>
                <property>
                    <name>queueName</name>
                    <value>${queueName}</value>
                </property>
            </configuration>
        </workflow>
    </action>
</coordinator-app>
```

Cron expressions are comprised of 5 required fields. The fields respectively are described as follows:

| **Field name** | **Allowed Values** | **Allowed Special Characters**  |
| --- | --- | --- |
| `Minutes` | `0-59` | , - * / |
| `Hours` | `0-23` | , - * / |
| `Day-of-month` | `1-31` | , - * ? / L W |
| `Month` | `1-12 or JAN-DEC` | , - * / |
| `Day-of-Week` | `1-7 or SUN-SAT` | , - * ? / L #|

The '**' character is used to specify all values. For example, "**" in the minute field means "every minute".

The '?' character is allowed for the day-of-month and day-of-week fields. It is used to specify 'no specific value'.
This is useful when you need to specify something in one of the two fields, but not the other.

The '-' character is used to specify ranges For example "10-12" in the hour field means "the hours 10, 11 and 12".

The ',' character is used to specify additional values. For example "MON,WED,FRI" in the day-of-week field means
"the days Monday, Wednesday, and Friday".

The '/' character is used to specify increments. For example "0/15" in the minutes field means "the minutes 0, 15, 30, and 45".
And "5/15" in the minutes field means "the minutes 5, 20, 35, and 50". Specifying '*' before the '/' is equivalent to
specifying 0 is the value to start with.
Essentially, for each field in the expression, there is a set of numbers that can be turned on or off.
For minutes, the numbers range from 0 to 59. For hours 0 to 23, for days of the month 0 to 31, and for months 1 to 12.
The "/" character simply helps you turn on every "nth" value in the given set. Thus "7/6" in the month field only turns on
month "7", it does NOT mean every 6th month, please note that subtlety.

The 'L' character is allowed for the day-of-month and day-of-week fields. This character is short-hand for "last",
but it has different meaning in each of the two fields.
For example, the value "L" in the day-of-month field means "the last day of the month" - day 31 for January, day 28 for
February on non-leap years.
If used in the day-of-week field by itself, it simply means "7" or "SAT".
But if used in the day-of-week field after another value, it means "the last xxx day of the month" - for example
"6L" means "the last Friday of the month".
You can also specify an offset from the last day of the month, such as "L-3" which would mean the third-to-last day of the
calendar month.
When using the 'L' option, it is important not to specify lists, or ranges of values, as you'll get confusing/unexpected results.

The 'W' character is allowed for the day-of-month field. This character is used to specify the weekday (Monday-Friday)
nearest the given day.
As an example, if you were to specify "15W" as the value for the day-of-month field, the meaning is:
"the nearest weekday to the 15th of the month". So if the 15th is a Saturday, the trigger will fire on Friday the 14th.
If the 15th is a Sunday, the trigger will fire on Monday the 16th. If the 15th is a Tuesday, then it will fire on Tuesday the 15th.
However if you specify "1W" as the value for day-of-month, and the 1st is a Saturday, the trigger will fire on Monday the 3rd,
as it will not 'jump' over the boundary of a month's days.
The 'W' character can only be specified when the day-of-month is a single day, not a range or list of days.

The 'L' and 'W' characters can also be combined for the day-of-month expression to yield 'LW', which translates to
"last weekday of the month".

The '#' character is allowed for the day-of-week field. This character is used to specify "the nth" XXX day of the month.
For example, the value of "6#3" in the day-of-week field means the third Friday of the month (day 6 = Friday and "#3" =
the 3rd one in the month).
Other examples: "2#1" = the first Monday of the month and "4#5" = the fifth Wednesday of the month.
Note that if you specify "#5" and there is not 5 of the given day-of-week in the month, then no firing will occur that month.
If the '#' character is used, there can only be one expression in the day-of-week field ("3#1,6#3" is not valid,
since there are two expressions).

The legal characters and the names of months and days of the week are not case sensitive.

If a user specifies an invalid cron syntax to run something on Feb, 30th for example: "0 10 30 2 *", the coordinator job
will not be created and an invalid coordinator frequency parse exception will be thrown.

If a user has a coordinator job that materializes no action during run time, for example: frequency of "0 10 ** ** *" with
start time of 2013-10-18T21:00Z and end time of 2013-10-18T22:00Z, the coordinator job submission will be rejected and
an invalid coordinator attribute exception will be thrown.

**<font color="#008000"> Examples: </font>**

| **Cron Expression** | **Meaning** |
| --- | --- |
| 10 9 ** ** * | Runs everyday at 9:10am |
| 10,30,45 9 ** ** * | Runs everyday at 9:10am, 9:30am, and 9:45am |
| `0 * 30 JAN 2-6` | Runs at 0 minute of every hour on weekdays and 30th of January |
| `0/20 9-17 ** ** 2-5` | Runs every Mon, Tue, Wed, and Thurs at minutes 0, 20, 40 from 9am to 5pm |
| 1 2 L-3 ** ** | Runs every third-to-last day of month at 2:01am |
| `1 2 6W 3 ?` | Runs on the nearest weekday to March, 6th every year at 2:01am |
| `1 2 * 3 3#2` | Runs every second Tuesday of March at 2:01am every year |
| `0 10,13 ** ** MON-FRI` | Runs every weekday at 10am and 1pm |


NOTES:

    Cron expression and syntax in Oozie are inspired by Quartz:http://quartz-scheduler.org/api/2.0.0/org/quartz/CronExpression.html.
    However, there is a major difference between Quartz cron and Oozie cron in which Oozie cron doesn't have "Seconds" field
    since everything in Oozie functions at the minute granularity at most. Everything related to Oozie cron syntax should be based
    on the documentation in the Oozie documentation.

    Cron expression uses oozie server processing timezone. Since default oozie processing timezone is UTC, if you want to
    run a job on every weekday at 10am in Tokyo, Japan(UTC + 9), your cron expression should be "0 1 * * 2-6" instead of
    the "0 10 * * 2-6" which you might expect.

    Overflowing ranges is supported but strongly discouraged - that is, having a larger number on the left hand side than the right.
    You might do 22-2 to catch 10 o'clock at night until 2 o'clock in the morning, or you might have NOV-FEB.
    It is very important to note that overuse of overflowing ranges creates ranges that don't make sense and
    no effort has been made to determine which interpretation CronExpression chooses.
    An example would be "0 14-6 ? * FRI-MON".

## 5. Dataset

A dataset is a collection of data referred to by a logical name.

A dataset instance is a particular occurrence of a dataset and it is represented by a unique set of URIs. A dataset instance can be individually referred. Dataset instances for datasets containing ranges are identified by a set of unique URIs, otherwise a dataset instance is identified by a single unique URI.

Datasets are typically defined in some central place for a business domain and can be accessed by the coordinator. Because of this, they can be defined once and used many times.

A dataset is a synchronous (produced at regular time intervals, it has an expected frequency) input.

A dataset instance is considered to be immutable while it is being consumed by coordinator jobs.

### 5.1. Synchronous Datasets

Instances of synchronous datasets are produced at regular time intervals, at an expected frequency. They are also referred to as "clocked datasets".

Synchronous dataset instances are identified by their nominal creation time. The nominal creation time is normally specified in the dataset instance URI.

A synchronous dataset definition contains the following information:

   * **<font color="#0000ff"> name: </font>** The dataset name. It must be a valid Java identifier.
   * **<font color="#0000ff"> frequency: </font>*** It represents the rate, in minutes at which data is _periodically_ created. The granularity is in minutes and can be expressed using EL expressions, for example: ${5 ** HOUR}.
   * **<font color="#0000ff"> initial-instance: </font>** The UTC datetime of the initial instance of the dataset. The initial-instance also provides the baseline datetime to compute instances of the dataset using multiples of the frequency.
   * **<font color="#0000ff"> timezone:</font>** The timezone of the dataset.
   * **<font color="#0000ff"> uri-template:</font>** The URI template that identifies the dataset and can be resolved into concrete URIs to identify a particular dataset instance. The URI template is constructed using:
      * **<font color="#0000ff"> constants </font>** See the allowable EL Time Constants below. Ex: ${YEAR}/${MONTH}.
      * **<font color="#0000ff"> variables </font>** Variables must be resolved at the time a coordinator job is submitted to the coordinator engine. They are normally provided a job parameters (configuration properties). Ex: ${market}/${language}
   * **<font color="#0000ff"> done-flag:</font>** This flag denotes when a dataset instance is ready to be consumed.
      * If the done-flag is omitted the coordinator will wait for the presence of a _SUCCESS file in the directory (Note: MapReduce jobs create this on successful completion automatically).
      * If the done-flag is present but empty, then the existence of the directory itself indicates that the dataset is ready.
      * If the done-flag is present but non-empty, Oozie will check for the presence of the named file within the directory, and will be considered ready (done) when the file exists.

The following EL constants can be used within synchronous dataset URI templates:

| **EL Constant** | **Resulting Format** | **Comments**  |
| --- | --- | --- |
| `YEAR` | _YYYY_ | 4 digits representing the year |
| `MONTH` | _MM_ | 2 digits representing the month of the year, January = 1 |
| `DAY` | _DD_ | 2 digits representing the day of the month |
| `HOUR` | _HH_ | 2 digits representing the hour of the day, in 24 hour format, 0 - 23 |
| `MINUTE` | _mm_ | 2 digits representing the minute of the hour, 0 - 59 |

**<font color="#800080">Syntax: </font>**


```
  <dataset name="[NAME]" frequency="[FREQUENCY]"
           initial-instance="[DATETIME]" timezone="[TIMEZONE]">
    <uri-template>[URI TEMPLATE]</uri-template>
    <done-flag>[FILE NAME]</done-flag>
  </dataset>
```

IMPORTANT: The values of the EL constants in the dataset URIs (in HDFS) are expected in UTC. Oozie Coordinator takes care of the timezone conversion when performing calculations.

**<font color="#008000"> Examples: </font>**

1. **A dataset produced once every day at 00:15 PST8PDT and done-flag is set to empty:**


    ```
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-02-15T08:15Z" timezone="America/Los_Angeles">
        <uri-template>
          hdfs://foo:8020/app/logs/${market}/${YEAR}${MONTH}/${DAY}/data
        </uri-template>
        <done-flag></done-flag>
      </dataset>
    ```


    The dataset would resolve to the following URIs and Coordinator looks for the existence of the directory itself:


    ```
      [market] will be replaced with user given property.

      hdfs://foo:8020/usr/app/[market]/2009/02/15/data
      hdfs://foo:8020/usr/app/[market]/2009/02/16/data
      hdfs://foo:8020/usr/app/[market]/2009/02/17/data
      ...
    ```


2. **A dataset available on the 10th of each month and done-flag is default '_SUCCESS':**


    ```
      <dataset name="stats" frequency="${coord:months(1)}"
               initial-instance="2009-01-10T10:00Z" timezone="America/Los_Angeles">
        <uri-template>hdfs://foo:8020/usr/app/stats/${YEAR}/${MONTH}/data</uri-template>
      </dataset>
    ```

    The dataset would resolve to the following URIs:


    ```
      hdfs://foo:8020/usr/app/stats/2009/01/data
      hdfs://foo:8020/usr/app/stats/2009/02/data
      hdfs://foo:8020/usr/app/stats/2009/03/data
      ...
    ```

    The dataset instances are not ready until '_SUCCESS' exists in each path:


    ```
      hdfs://foo:8020/usr/app/stats/2009/01/data/_SUCCESS
      hdfs://foo:8020/usr/app/stats/2009/02/data/_SUCCESS
      hdfs://foo:8020/usr/app/stats/2009/03/data/_SUCCESS
      ...
    ```


3. **A dataset available at the end of every quarter and done-flag is 'trigger.dat':**


    ```
      <dataset name="stats" frequency="${coord:months(3)}"
               initial-instance="2009-01-31T20:00Z" timezone="America/Los_Angeles">
        <uri-template>
          hdfs://foo:8020/usr/app/stats/${YEAR}/${MONTH}/data
        </uri-template>
        <done-flag>trigger.dat</done-flag>
      </dataset>
    ```

    The dataset would resolve to the following URIs:


    ```
      hdfs://foo:8020/usr/app/stats/2009/01/data
      hdfs://foo:8020/usr/app/stats/2009/04/data
      hdfs://foo:8020/usr/app/stats/2009/07/data
      ...
    ```

    The dataset instances are not ready until 'trigger.dat' exists in each path:


    ```
      hdfs://foo:8020/usr/app/stats/2009/01/data/trigger.dat
      hdfs://foo:8020/usr/app/stats/2009/04/data/trigger.dat
      hdfs://foo:8020/usr/app/stats/2009/07/data/trigger.dat
      ...
    ```


4. **Normally the URI template of a dataset has a precision similar to the frequency:**


    ```
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T10:30Z" timezone="America/Los_Angeles">
        <uri-template>
          hdfs://foo:8020/usr/app/logs/${YEAR}/${MONTH}/${DAY}/data
        </uri-template>
      </dataset>
    ```

    The dataset would resolve to the following URIs:


    ```
      hdfs://foo:8020/usr/app/logs/2009/01/01/data
      hdfs://foo:8020/usr/app/logs/2009/01/02/data
      hdfs://foo:8020/usr/app/logs/2009/01/03/data
      ...
    ```

5. **However, if the URI template has a finer precision than the dataset frequency:**


    ```
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T10:30Z" timezone="America/Los_Angeles">
        <uri-template>
          hdfs://foo:8020/usr/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}/data
        </uri-template>
      </dataset>
    ```

    The dataset resolves to the following URIs with fixed values for the finer precision template variables:


    ```
      hdfs://foo:8020/usr/app/logs/2009/01/01/10/30/data
      hdfs://foo:8020/usr/app/logs/2009/01/02/10/30/data
      hdfs://foo:8020/usr/app/logs/2009/01/03/10/30/data
      ...
    ```

### 5.2. Dataset URI-Template types

Each dataset URI could be a HDFS path URI denoting a HDFS directory: `hdfs://foo:8020/usr/logs/20090415` or a
HCatalog partition URI identifying a set of table partitions: `hcat://bar:8020/logsDB/logsTable/dt=20090415;region=US`.

HCatalog enables table and storage management for Pig, Hive and MapReduce. The format to specify a HCatalog table partition URI is
`hcat://[metastore server]:[port]/[database name]/[table name]/[partkey1]=[value];[partkey2]=[value];...`

For example,

```
  <dataset name="logs" frequency="${coord:days(1)}"
           initial-instance="2009-02-15T08:15Z" timezone="America/Los_Angeles">
    <uri-template>
      hcat://myhcatmetastore:9080/database1/table1/myfirstpartitionkey=myfirstvalue;mysecondpartitionkey=mysecondvalue
    </uri-template>
    <done-flag></done-flag>
  </dataset>
```

### 5.3. Asynchronous Datasets
   * TBD

### 5.4. Dataset Definitions

Dataset definitions are grouped in XML files.
**IMPORTANT:** Please note that if an XML namespace version is specified for the coordinator-app element in the coordinator.xml file, no namespace needs to be defined separately for the datasets element (even if the dataset is defined in a separate file). Specifying it at multiple places might result in xml errors while submitting the coordinator job.

**<font color="#800080">Syntax: </font>**


```
 <!-- Synchronous datasets -->
<datasets>
  <include>[SHARED_DATASETS]</include>
  ...
  <dataset name="[NAME]" frequency="[FREQUENCY]"
           initial-instance="[DATETIME]" timezone="[TIMEZONE]">
    <uri-template>[URI TEMPLATE]</uri-template>
  </dataset>
  ...
</datasets>
```

**<font color="#008000"> Example: </font>**


```
<datasets>
.
  <include>hdfs://foo:8020/app/dataset-definitions/globallogs.xml</include>
.
  <dataset name="logs" frequency="${coord:hours(12)}"
           initial-instance="2009-02-15T08:15Z" timezone="Americas/Los_Angeles">
    <uri-template>
    hdfs://foo:8020/app/logs/${market}/${YEAR}${MONTH}/${DAY}/${HOUR}/${MINUTE}/data
    </uri-template>
  </dataset>
.
  <dataset name="stats" frequency="${coord:months(1)}"
           initial-instance="2009-01-10T10:00Z" timezone="Americas/Los_Angeles">
    <uri-template>hdfs://foo:8020/usr/app/stats/${YEAR}/${MONTH}/data</uri-template>
  </dataset>
.
</datasets>
```

## 6. Coordinator Application

### 6.1. Concepts

#### 6.1.1. Coordinator Application

A coordinator application is a program that triggers actions (commonly workflow jobs) when a set of conditions are met. Conditions can be a time frequency, the availability of new dataset instances or other external events.

Types of coordinator applications:

   * **Synchronous:** Its coordinator actions are created at specified time intervals.

Coordinator applications are normally parameterized.

#### 6.1.2. Coordinator Job

To create a coordinator job, a job configuration that resolves all coordinator application parameters must be provided to the coordinator engine.

A coordinator job is a running instance of a coordinator application running from a start time to an end time. The start
time must be earlier than the end time.

At any time, a coordinator job is in one of the following status: **PREP, RUNNING, RUNNINGWITHERROR, PREPSUSPENDED, SUSPENDED, SUSPENDEDWITHERROR, PREPPAUSED, PAUSED, PAUSEDWITHERROR, SUCCEEDED, DONEWITHERROR, KILLED, FAILED**.

Valid coordinator job status transitions are:

   * **PREP --> PREPSUSPENDED | PREPPAUSED | RUNNING | KILLED**
   * **RUNNING --> RUNNINGWITHERROR | SUSPENDED | PAUSED | SUCCEEDED | KILLED**
   * **RUNNINGWITHERROR --> RUNNING | SUSPENDEDWITHERROR | PAUSEDWITHERROR | DONEWITHERROR | KILLED | FAILED**
   * **PREPSUSPENDED --> PREP | KILLED**
   * **SUSPENDED --> RUNNING | KILLED**
   * **SUSPENDEDWITHERROR --> RUNNINGWITHERROR | KILLED**
   * **PREPPAUSED --> PREP | KILLED**
   * **PAUSED --> SUSPENDED | RUNNING | KILLED**
   * **PAUSEDWITHERROR --> SUSPENDEDWITHERROR | RUNNINGWITHERROR | KILLED**
   * **FAILED | KILLED --> IGNORED**
   * **IGNORED --> RUNNING**

When a coordinator job is submitted, oozie parses the coordinator job XML. Oozie then creates a record for the coordinator with status **PREP** and returns a unique ID. The coordinator is also started immediately if pause time is not set.

When a user requests to suspend a coordinator job that is in **PREP** state, oozie puts the job in status **PREPSUSPENDED**. Similarly, when pause time reaches for a coordinator job with **PREP** status, oozie puts the job in status **PREPPAUSED**.

Conversely, when a user requests to resume a **PREPSUSPENDED** coordinator job, oozie puts the job in status **PREP**. And when pause time is reset for a coordinator job and job status is **PREPPAUSED**, oozie puts the job in status **PREP**.

When a coordinator job starts, oozie puts the job in status **RUNNING** and start materializing workflow jobs based on job frequency. If any workflow job goes to **FAILED/KILLED/TIMEDOUT** state, the coordinator job is put in **RUNNINGWITHERROR**

When a user requests to kill a coordinator job, oozie puts the job in status **KILLED** and it sends kill to all submitted workflow jobs.

When a user requests to suspend a coordinator job that is in **RUNNING** status, oozie puts the job in status **SUSPENDED** and it suspends all submitted workflow jobs. Similarly, when a user requests to suspend a coordinator job that is in **RUNNINGWITHERROR** status, oozie puts the job in status **SUSPENDEDWITHERROR** and it suspends all submitted workflow jobs.

When pause time reaches for a coordinator job that is in **RUNNING** status, oozie puts the job in status **PAUSED**. Similarly, when pause time reaches for a coordinator job that is in **RUNNINGWITHERROR** status, oozie puts the job in status **PAUSEDWITHERROR**.

Conversely, when a user requests to resume a **SUSPENDED** coordinator job, oozie puts the job in status **RUNNING**. Also,  when a user requests to resume a **SUSPENDEDWITHERROR** coordinator job, oozie puts the job in status **RUNNINGWITHERROR**. And when pause time is reset for a coordinator job and job status is **PAUSED**, oozie puts the job in status **RUNNING**. Also, when the pause time is reset for a coordinator job and job status is **PAUSEDWITHERROR**, oozie puts the job in status **RUNNINGWITHERROR**

A coordinator job creates workflow jobs (commonly coordinator actions) only for the duration of the coordinator job and only if the coordinator job is in **RUNNING** status. If the coordinator job has been suspended, when resumed it will create all the coordinator actions that should have been created during the time it was suspended, actions will not be lost, they will delayed.

When the coordinator job materialization finishes and all workflow jobs finish, oozie updates the coordinator status accordingly.
For example, if all workflows are **SUCCEEDED**, oozie puts the coordinator job into **SUCCEEDED** status.
If all workflows are **FAILED**, oozie puts the coordinator job into **FAILED** status. If all workflows are **KILLED**, the coordinator
job status changes to KILLED. However, if any workflow job finishes with not **SUCCEEDED** and combination of **KILLED**, **FAILED** or
**TIMEOUT**, oozie puts the coordinator job into **DONEWITHERROR**. If all coordinator actions are **TIMEDOUT**, oozie puts the
coordinator job into **DONEWITHERROR**.

A coordinator job in **FAILED** or **KILLED** status can be changed to **IGNORED** status. A coordinator job in **IGNORED** status can be changed to
 **RUNNING** status.

#### 6.1.3. Coordinator Action

A coordinator job creates and executes coordinator actions.

A coordinator action is normally a workflow job that consumes and produces dataset instances.

Once an coordinator action is created (this is also referred as the action being materialized), the coordinator action will be in waiting until all required inputs for execution are satisfied or until the waiting times out.

##### 6.1.3.1. Coordinator Action Creation (Materialization)

A coordinator job has one driver event that determines the creation (materialization) of its coordinator actions (typically a workflow job).

   * For synchronous coordinator jobs the driver event is the frequency of the coordinator job.

##### 6.1.3.2. Coordinator Action Status

Once a coordinator action has been created (materialized) the coordinator action qualifies for execution. At this point, the action status is **WAITING**.

A coordinator action in **WAITING** status must wait until all its input events are available before is ready for execution. When a coordinator action is ready for execution its status is **READY**.

A coordinator action in **WAITING** status may timeout before it becomes ready for execution. Then the action status is **TIMEDOUT**.

A coordinator action may remain in **READY** status for a while, without starting execution, due to the concurrency execution policies of the coordinator job.

A coordinator action in **READY** or **WAITING** status changes to **SKIPPED** status if the execution strategy is LAST_ONLY and the
current time is past the next action's nominal time.  See section 6.3 for more details.

A coordinator action in **READY** or **WAITING** status changes to **SKIPPED** status if the execution strategy is NONE and the
current time is past the action's nominal time + 1 minute.  See section 6.3 for more details.

A coordinator action in **READY** status changes to **SUBMITTED** status if total current **RUNNING** and **SUBMITTED** actions are less than concurrency execution limit.

A coordinator action in **SUBMITTED** status changes to **RUNNING** status when the workflow engine start execution of the coordinator action.

A coordinator action is in **RUNNING** status until the associated workflow job completes its execution. Depending on the workflow job completion status, the coordinator action will be in **SUCCEEDED**, **KILLED** or **FAILED** status.

A coordinator action in **WAITING**, **READY**, **SUBMITTED** or **RUNNING** status can be killed, changing to **KILLED** status.

A coordinator action in **SUBMITTED** or **RUNNING** status can also fail, changing to **FAILED** status.

A coordinator action in **FAILED**, **KILLED**, or **TIMEDOUT** status can be changed to **IGNORED** status. A coordinator action in **IGNORED** status can be
 rerun, changing to **WAITING** status.

Valid coordinator action status transitions are:

   * **WAITING --> READY | TIMEDOUT | SKIPPED | KILLED**
   * **READY --> SUBMITTED | SKIPPED | KILLED**
   * **SUBMITTED --> RUNNING | KILLED | FAILED**
   * **RUNNING --> SUCCEEDED | KILLED | FAILED**
   * **FAILED | KILLED | TIMEDOUT --> IGNORED**
   * **IGNORED --> WAITING**

#### 6.1.4. Input Events

The Input events of a coordinator application specify the input conditions that are required in order to execute a coordinator action.

In the current specification input events are restricted to dataset instances availability.

All the datasets instances defined as input events must be available for the coordinator action to be ready for execution ( **READY** status).

Input events are normally parameterized. For example, the last 24 hourly instances of the 'searchlogs' dataset.

Input events can be refer to multiple instances of multiple datasets. For example, the last 24 hourly instances of the 'searchlogs' dataset and the last weekly instance of the 'celebrityRumours' dataset.

#### 6.1.5. Output Events

A coordinator action can produce one or more dataset(s) instances as output.

Dataset instances produced as output by one coordinator actions may be consumed as input by another coordinator action(s) of other coordinator job(s).

The chaining of coordinator jobs via the datasets they produce and consume is referred as a **data pipeline.**

In the current specification coordinator job output events are restricted to dataset instances.

#### 6.1.6. Coordinator Action Execution Policies

The execution policies for the actions of a coordinator job can be defined in the coordinator application.

   * Timeout: A coordinator job can specify the timeout for its coordinator actions, this is, how long the coordinator action will be in *WAITING* or *READY* status before giving up on its execution.
   * Concurrency: A coordinator job can specify the concurrency for its coordinator actions, this is, how many coordinator actions are allowed to run concurrently ( **RUNNING** status) before the coordinator engine starts throttling them.
   * Execution strategy: A coordinator job can specify the execution strategy of its coordinator actions when there is backlog of coordinator actions in the coordinator engine. The different execution strategies are 'oldest first', 'newest first', 'none' and 'last one only'. A backlog normally happens because of delayed input data, concurrency control or because manual re-runs of coordinator jobs.
   * Throttle: A coordinator job can specify the materialization or creation throttle value for its coordinator actions, this is, how many maximum coordinator actions are allowed to be in WAITING state concurrently.

#### 6.1.7. Data Pipeline Application

Commonly, multiple workflow applications are chained together to form a more complex application.

Workflow applications are run on regular basis, each of one of them at their own frequency. The data consumed and produced by these workflow applications is relative to the nominal time of workflow job that is processing the data. This is a **coordinator application**.

The output of multiple workflow jobs of a single workflow application is then consumed by a single workflow job of another workflow application, this is done on regular basis as well. These workflow jobs are triggered by recurrent actions of coordinator jobs. This is a set of **coordinator jobs** that inter-depend on each other via the data they produce and consume.

This set of interdependent **coordinator applications** is referred as a **data pipeline application**.

### 6.2. Synchronous Coordinator Application Example

   * The `checkouts` synchronous dataset is created every 15 minutes by an online checkout store.
   * The `hourlyRevenue` synchronous dataset is created every hour and contains the hourly revenue.
   * The `dailyRevenue` synchronous dataset is created every day and contains the daily revenue.
   * The `monthlyRevenue` synchronous dataset is created every month and contains the monthly revenue.

   * The `revenueCalculator-wf` workflow consumes checkout data and produces as output the corresponding revenue.
   * The `rollUpRevenue-wf` workflow consumes revenue data and produces a consolidated output.

   * The `hourlyRevenue-coord` coordinator job triggers, every hour, a `revenueCalculator-wf` workflow. It specifies as input the last 4 `checkouts` dataset instances and it specifies as output a new instance of the `hourlyRevenue` dataset.
   * The `dailyRollUpRevenue-coord` coordinator job triggers, every day, a `rollUpRevenue-wf` workflow. It specifies as input the last 24 `hourlyRevenue` dataset instances and it specifies as output a new instance of the `dailyRevenue` dataset.
   * The `monthlyRollUpRevenue-coord` coordinator job triggers, once a month, a `rollUpRevenue-wf` workflow. It specifies as input all the `dailyRevenue` dataset instance of the month and it specifies as output a new instance of the `monthlyRevenue` dataset.

This example contains describes all the components that conform a data pipeline: datasets, coordinator jobs and coordinator actions (workflows).

The coordinator actions (the workflows) are completely agnostic of datasets and their frequencies, they just use them as input and output data (i.e. HDFS files or directories). Furthermore, as the example shows, the same workflow can be used to process similar datasets of different frequencies.

The frequency of the `hourlyRevenue-coord` coordinator job is 1 hour, this means that every hour a coordinator action is created. A coordinator action will be executed only when the 4 `checkouts` dataset instances for the corresponding last hour are available, until then the coordinator action will remain as created (materialized), in **WAITING** status. Once the 4 dataset instances for the corresponding last hour are available, the coordinator action will be executed and it will start a `revenueCalculator-wf` workflow job.

### 6.3. Synchronous Coordinator Application Definition

A synchronous coordinator definition is defined by a name, start time and end time, the frequency of creation of its coordinator
actions, the input events, the output events and action control information:

   * **<font color="#0000ff"> start: </font>** The start datetime for the job. Starting at this time actions will be materialized. Refer to section #3 'Datetime Representation' for syntax details.
   * **<font color="#0000ff"> end: </font>** The end datetime for the job. When actions will stop being materialized. Refer to section #3 'Datetime Representation' for syntax details.
   * **<font color="#0000ff"> timezone:</font>** The timezone of the coordinator application.
   * **<font color="#0000ff"> frequency: </font>** The frequency, in minutes, to materialize actions. Refer to section #4 'Time Interval Representation' for syntax details.
   * Control information:
      * **<font color="#0000ff"> timeout: </font>** The maximum time, in minutes, that a materialized action will be waiting
      for the additional conditions to be satisfied before being discarded. A timeout of `0` indicates that if all the input
      events are not satisfied at the time of action materialization, the action should timeout immediately. A timeout of
      `-1` indicates no timeout, the materialized action will wait forever for the other conditions to be satisfied. The
      default value is `120` minutes. The timeout can only cause a `WAITING` action to transition to `TIMEDOUT`; once the
      data dependency is satisified, a `WAITING` action transitions to `READY`, and the timeout no longer has any affect,
      even if the action hasn't transitioned to `SUBMITTED` or `RUNNING` when it expires.
      * **<font color="#0000ff"> concurrency: </font>** The maximum number of actions for this job that can be running at the same time. This value allows to materialize and submit multiple instances of the coordinator app, and allows operations to catchup on delayed processing. The default value is `1`.
      * **<font color="#0000ff"> execution: </font>** Specifies the execution order if multiple instances of the coordinator job have satisfied their execution criteria. Valid values are:
         * `FIFO` (oldest first) **default**.
         * `LIFO` (newest first).
         * `LAST_ONLY` (see explanation below).
         * `NONE` (see explanation below).
      * **<font color="#0000ff"> throttle: </font>** The maximum coordinator actions are allowed to be in WAITING state concurrently. The default value is `12`.
   * **<font color="#0000ff"> datasets: </font>** The datasets coordinator application uses.
   * **<font color="#0000ff"> input-events: </font>** The coordinator job input events.
      * **<font color="#0000ff"> data-in: </font>** It defines one job input condition that resolves to one or more instances of a dataset.
         * **<font color="#0000ff"> name: </font>** input condition name.
         * **<font color="#0000ff"> dataset: </font>** dataset name.
         * **<font color="#0000ff"> instance: </font>** refers to a single dataset instance (the time for a synchronous dataset).
         * **<font color="#0000ff"> start-instance: </font>** refers to the beginning of an instance range (the time for a synchronous dataset).
         * **<font color="#0000ff"> end-instance: </font>** refers to the end of an instance range (the time for a synchronous dataset).
   * **<font color="#0000ff"> output-events: </font>** The coordinator job output events.
      * **<font color="#0000ff"> data-out: </font>** It defines one job output that resolves to a dataset instance.
         * **<font color="#0000ff"> name: </font>** output name.
         * **<font color="#0000ff"> dataset: </font>** dataset name.
         * **<font color="#0000ff"> instance: </font>** dataset instance that will be generated by coordinator action.
         * **<font color="#0000ff"> nocleanup: </font>** disable cleanup of the output dataset in rerun if true, even when nocleanup option is not used in CLI command.
   * **<font color="#0000ff"> action: </font>** The coordinator action to execute.
      * **<font color="#0000ff"> workflow: </font>** The workflow job invocation. Workflow job properties can refer to the defined data-in and data-out elements.

**LAST_ONLY:** While `FIFO` and `LIFO` simply specify the order in which READY actions should be executed, `LAST_ONLY` can actually
cause some actions to be SKIPPED and is a little harder to understand.  When `LAST_ONLY` is set, an action that is `WAITING`
or `READY` will be `SKIPPED` when the current time is past the next action's nominal time.  For example, suppose action 1 and 2
are both `READY`, the current time is 5:00pm, and action 2's nominal time is 5:10pm.  In 10 minutes from now, at 5:10pm, action 1
will become SKIPPED, assuming it doesn't transition to `SUBMITTED` (or a terminal state) before then.  This sounds similar to the
timeout control, but there are some important differences:

   * The timeout time is configurable while the `LAST_ONLY` time is effectively the frequency.
   * Reaching the timeout causes an action to transition to `TIMEDOUT`, which will cause the Coordinator Job to become `RUNNINGWITHERROR` and eventually `DONEWITHERROR`.  With `LAST_ONLY`, an action becomes `SKIPPED` and the Coordinator Job remains `RUNNING` and eventually `DONE`.
   * The timeout is looking satisfying the data dependency, while `LAST_ONLY` is looking at the action itself.  This means that the timeout can only cause a transition from `WAITING`, while `LAST_ONLY` can cause a transition from `WAITING` or `READY`.

`LAST_ONLY` is useful if you want a recurring job, but do not actually care about the individual instances and just
always want the latest action.  For example, if you have a coordinator running every 10 minutes and take Oozie down for 1 hour, when
Oozie comes back, there would normally be 6 actions `WAITING` or `READY` to run.  However, with `LAST_ONLY`, only the current one
will go to `SUBMITTED` and then `RUNNING`; the others will go to `SKIPPED`.

**NONE:** Similar to `LAST_ONLY` except instead of looking at the next action's nominal time, it looks
at `oozie.coord.execution.none.tolerance` in oozie-site.xml (default is 1 minute). When `NONE` is set, an action that is `WAITING`
or `READY` will be `SKIPPED` when the current time is more than the configured number of minutes (tolerance) past that action's
nominal time. For example, suppose action 1 and 2 are both `READY`, the current time is 5:20pm, and both actions' nominal times are
before 5:19pm. Both actions will become `SKIPPED`, assuming they don't transition to `SUBMITTED` (or a terminal state) before then.

In case the Coordinator job's execution mode is LAST_ONLY or NONE, then the Coordinator action number to be
materialized can be huge. This can be too much for only one CoordMaterializeTransitionXCommand to handle,
as it would lead to OOM as described in OOZIE-3254. In order to prevent this situation to happen, the current
approach only lets a certain amount of actions to be materialized within a CoordMaterializeTransitionXCommand
with the default value of 10000, which can be configured through Oozie configuration defined in either oozie-default.xml
or oozie-site.xml using the property name `oozie.service.CoordMaterializeTriggerService.action.batch.size`.
NOTE: this "batch mode" can be turned off by setting its value to -1. Once a CoordMaterializeTransitionXCommand
is finished, the CoordMaterializeTriggerService is responsible for materializing the potential remaining
Coordinator actions. NOTE: the CoordMaterializeTriggerService gets triggered in every 5 minutes by default.
This means if the Coordinator job's execution mode is LAST_ONLY or NONE, a maximum number of
`oozie.service.CoordMaterializeTriggerService.action.batch.size` will be materialized in every 5 minutes.

**<font color="#800080">Syntax: </font>**


```
   <coordinator-app name="[NAME]" frequency="[FREQUENCY]"
                    start="[DATETIME]" end="[DATETIME]" timezone="[TIMEZONE]"
                    xmlns="uri:oozie:coordinator:0.1">
      <controls>
        <timeout>[TIME_PERIOD]</timeout>
        <concurrency>[CONCURRENCY]</concurrency>
        <execution>[EXECUTION_STRATEGY]</execution>
      </controls>
.
      <datasets>
        <include>[SHARED_DATASETS]</include>
        ...
.
        <!-- Synchronous datasets -->
	    <dataset name="[NAME]" frequency="[FREQUENCY]"
	             initial-instance="[DATETIME]" timezone="[TIMEZONE]">
	      <uri-template>[URI_TEMPLATE]</uri-template>
        </dataset>
        ...
.
      </datasets>
.
      <input-events>
        <data-in name="[NAME]" dataset="[DATASET]">
          <instance>[INSTANCE]</instance>
          ...
        </data-in>
        ...
        <data-in name="[NAME]" dataset="[DATASET]">
          <start-instance>[INSTANCE]</start-instance>
          <end-instance>[INSTANCE]</end-instance>
        </data-in>
        ...
      </input-events>
      <output-events>
         <data-out name="[NAME]" dataset="[DATASET]">
           <instance>[INSTANCE]</instance>
         </data-out>
         ...
      </output-events>
      <action>
        <workflow>
          <app-path>[WF-APPLICATION-PATH]</app-path>
          <configuration>
            <property>
              <name>[PROPERTY-NAME]</name>
              <value>[PROPERTY-VALUE]</value>
            </property>
            ...
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

**<font color="#008000"> Examples: </font>**

**1. A Coordinator Job that executes a single coordinator action:**

The following example describes a synchronous coordinator application that runs once a day for 1 day at the end of the day. It consumes an instance of a daily 'logs' dataset and produces an instance of a daily 'siteAccessStats' dataset.

**Coordinator application definition:**


```
   <coordinator-app name="hello-coord" frequency="${coord:days(1)}"
                    start="2009-01-02T08:00Z" end="2009-01-02T08:00Z"
                    timezone="America/Los_Angeles"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:days(1)}"
                 initial-instance="2009-01-02T08:00Z" timezone="America/Los_Angeles">
          <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}/data</uri-template>
        </dataset>
        <dataset name="siteAccessStats" frequency="${coord:days(1)}"
                 initial-instance="2009-01-02T08:00Z" timezone="America/Los_Angeles">
          <uri-template>hdfs://bar:8020/app/stats/${YEAR}/${MONTH}/${DAY}/data</uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <instance>2009-01-02T08:00Z</instance>
        </data-in>
      </input-events>
      <output-events>
         <data-out name="output" dataset="siteAccessStats">
           <instance>2009-01-02T08:00Z</instance>
         </data-out>
      </output-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
          <configuration>
            <property>
              <name>wfInput</name>
              <value>${coord:dataIn('input')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('output')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

There are 2 synchronous datasets with a daily frequency and they are expected at the end of each PST8PDT day.

This coordinator job runs for 1 day on January 1st 2009 at 24:00 PST8PDT.

The workflow job invocation for the single coordinator action would resolve to:


```
  <workflow>
    <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
    <configuration>
      <property>
        <name>wfInput</name>
        <value>hdfs://bar:8020/app/logs/200901/02/data</value>
      </property>
      <property>
        <name>wfOutput</name>
        <value>hdfs://bar:8020/app/stats/2009/01/02/data</value>
      </property>
    </configuration>
  </workflow>
```

IMPORTANT: Note Oozie works in UTC datetimes, all URI templates resolve to UTC datetime values. Because of the timezone difference between UTC and PST8PDT, the URIs resolves to `2009-01-02T08:00Z` (UTC) which is equivalent to 2009-01-01T24:00PST8PDT= (PST).

There is single input event, which resolves to January 1st PST8PDT instance of the 'logs' dataset. There is single output event, which resolves to January 1st PST8PDT instance of the 'siteAccessStats' dataset.

The `${coord:dataIn(String name)}` and `${coord:dataOut(String name)}` EL functions resolve to the dataset instance URIs of the corresponding dataset instances. These EL functions are properly defined in a subsequent section.

Because the `${coord:dataIn(String name)}` and `${coord:dataOut(String name)}` EL functions resolve to URIs, which are HDFS URIs, the workflow job itself does not deal with dataset instances, just HDFS URIs.

**2. A Coordinator Job that executes its coordinator action multiple times:**

A more realistic version of the previous example would be a coordinator job that runs for a year creating a daily action an consuming the daily 'logs' dataset instance and producing the daily 'siteAccessStats' dataset instance.

The coordinator application is identical, except for the frequency, 'end' date and parameterization in the input and output events sections:


```
   <coordinator-app name="hello-coord" frequency="${coord:days(1)}"
                    start="2009-01-02T08:00Z" end="2010-01-02T08:00Z"
                    timezone="America/Los_Angeles"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:days(1)}"
                 initial-instance="2009-01-02T08:00Z" timezone="America/Los_Angeles">
          <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}/data</uri-template>
        </dataset>
        <dataset name="siteAccessStats" frequency="${coord:days(1)}"
                 initial-instance="2009-01-02T08:00Z" timezone="America/Los_Angeles">
          <uri-template>hdfs://bar:8020/app/stats/${YEAR}/${MONTH}/${DAY}/data</uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <instance>${coord:current(0)}</instance>
        </data-in>
      </input-events>
      <output-events>
         <data-out name="output" dataset="siteAccessStats">
           <instance>${coord:current(0)}</instance>
         </data-out>
      </output-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
          <configuration>
            <property>
              <name>wfInput</name>
              <value>${coord:dataIn('input')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('output')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

The `${coord:current(int offset)}` EL function resolves to coordinator action creation time, that would be the current day at the time the coordinator action is created: `2009-01-02T08:00 ... 2010-01-01T08:00`. This EL function is properly defined in a subsequent section.

There is single input event, which resolves to the current day instance of the 'logs' dataset.

There is single output event, which resolves to the current day instance of the 'siteAccessStats' dataset.

The workflow job invocation for the first coordinator action would resolve to:


```
  <workflow>
    <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
    <configuration>
      <property>
        <name>wfInput</name>
        <value>hdfs://bar:8020/app/logs/200901/02/data</value>
      </property>
      <property>
        <name>wfOutput</name>
        <value>hdfs://bar:8020/app/stats/2009/01/02/data</value>
      </property>
    </configuration>
  </workflow>
```

For the second coordinator action it would resolve to:


```
  <workflow>
    <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
    <configuration>
      <property>
        <name>wfInput</name>
        <value>hdfs://bar:8020/app/logs/200901/03/data</value>
      </property>
      <property>
        <name>wfOutput</name>
        <value>hdfs://bar:8020/app/stats/2009/01/03/data</value>
      </property>
    </configuration>
  </workflow>
```

And so on.

**3. A Coordinator Job that executes its coordinator action multiple times and as input takes multiple dataset instances:**

The following example is a variation of the example #2 where the synchronous coordinator application runs weekly. It consumes the of the last 7 instances of a daily 'logs' dataset and produces an instance of a weekly 'weeklySiteAccessStats' dataset.

'logs' is a synchronous dataset with a daily frequency and it is expected at the end of each day (24:00).

'weeklystats' is a synchronous dataset with a weekly frequency and it is expected at the end (24:00) of every 7th day.

The coordinator application frequency is weekly and it starts on the 7th day of the year:


```
   <coordinator-app name="hello2-coord" frequency="${coord:days(7)}"
                    start="2009-01-07T24:00Z" end="2009-12-12T24:00Z"
                    timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:days(1)}"
                 initial-instance="2009-01-01T24:00Z" timezone="UTC">
          <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}</uri-template>
        </dataset>
        <dataset name="weeklySiteAccessStats" frequency="${coord:days(7)}"
                 initial-instance="2009-01-07T24:00Z" timezone="UTC">
          <uri-template>hdfs://bar:8020/app/weeklystats/${YEAR}/${MONTH}/${DAY}</uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <start-instance>${coord:current(-6)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <output-events>
         <data-out name="output" dataset="siteAccessStats">
           <instance>${coord:current(0)}</instance>
         </data-out>
      </output-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
          <configuration>
            <property>
              <name>wfInput</name>
              <value>${coord:dataIn('input')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('output')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

The `${coord:current(int offset)}` EL function resolves to coordinator action creation time minus the specified offset multiplied by the dataset frequency. This EL function is properly defined in a subsequent section.

The input event, instead resolving to a single 'logs' dataset instance, it refers to a range of 7 dataset instances - the instance for 6 days ago, 5 days ago, ... and today's instance.

The output event resolves to the current day instance of the 'weeklySiteAccessStats' dataset. As the coordinator job will create a coordinator action every 7 days, dataset instances for the 'weeklySiteAccessStats' dataset will be created every 7 days.

The workflow job invocation for the first coordinator action would resolve to:


```
  <workflow>
    <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
    <configuration>
      <property>
        <name>wfInput</name>
        <value>
               hdfs://bar:8020/app/logs/200901/01,hdfs://bar:8020/app/logs/200901/02,
               hdfs://bar:8020/app/logs/200901/03,hdfs://bar:8020/app/logs/200901/05,
               hdfs://bar:8020/app/logs/200901/05,hdfs://bar:8020/app/logs/200901/06,
               hdfs://bar:8020/app/logs/200901/07
        </value>
      </property>
      <property>
        <name>wfOutput</name>
        <value>hdfs://bar:8020/app/stats/2009/01/07</value>
      </property>
    </configuration>
  </workflow>
```

For the second coordinator action it would resolve to:


```
  <workflow>
    <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
    <configuration>
      <property>
        <name>wfInput</name>
        <value>
               hdfs://bar:8020/app/logs/200901/08,hdfs://bar:8020/app/logs/200901/09,
               hdfs://bar:8020/app/logs/200901/10,hdfs://bar:8020/app/logs/200901/11,
               hdfs://bar:8020/app/logs/200901/12,hdfs://bar:8020/app/logs/200901/13,
               hdfs://bar:8020/app/logs/200901/16
        </value>
      </property>
      <property>
        <name>wfOutput</name>
        <value>hdfs://bar:8020/app/stats/2009/01/16</value>
      </property>
    </configuration>
  </workflow>
```

And so on.

### 6.4. Asynchronous Coordinator Application Definition
   * TBD

### 6.5. Parameterization of Coordinator Applications

When a coordinator job is submitted to Oozie, the submitter may specify as many coordinator job configuration properties as required (similar to Hadoop JobConf properties).

Configuration properties that are a valid Java identifier, [A-Za-z_][0-9A-Za-z_]*, are available as `${NAME}` variables within the coordinator application definition.

Configuration Properties that are not a valid Java identifier, for example `job.tracker`, are available via the `${coord:conf(String name)}` function. Valid Java identifier properties are available via this function as well.

Using properties that are valid Java identifiers result in a more readable and compact definition.

Dataset definitions can be also parameterized, the parameters are resolved using the configuration properties of Job configuration used to submit the coordinator job.

If a configuration property used in the definitions is not provided with the job configuration used to submit a coordinator job, the value of the parameter will be undefined and the job submission will fail.

**<font color="#008000"> Example: </font>**

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="${jobStart}" end="${jobEnd}" timezone="${timezone}"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="${logsInitialInstance}" timezone="${timezone}">
          <uri-template>
            hdfs://bar:8020/app/logs/${market}/${language}/${YEAR}${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

In the above example there are 6 configuration parameters (variables) that have to be provided when submitting a job:

   * `jobStart` : start datetime for the job, in UTC
   * `jobEnd` : end datetime for the job, in UTC
   * `logsInitialInstance` : expected time of the first logs instance, in UTC
   * `timezone` : timezone for the job and the dataset
   * `market` : market to compute by this job, used in the uri-template
   * `language` : language to compute by this job, used in the uri-template

IMPORTANT: Note that this example is not completely correct as it always consumes the last 24 instances of the 'logs' dataset. It is assumed that all days have 24 hours. For timezones that observe daylight saving this application will not work as expected as it will consume the wrong number of dataset instances in DST switch days. To be able to handle these scenarios, the `${coord:hoursInDays(int n)}` and `${coord:daysInMonths(int n)}` EL functions must be used (refer to section #6.6.2 and #6.6.3).

If the above 6 properties are not specified, the job will fail.

As of schema 0.4, a list of formal parameters can be provided which will allow Oozie to verify, at submission time, that said
properties are actually specified (i.e. before the job is executed and fails). Default values can also be provided.

**Example:**

The previous parameterized coordinator application definition with formal parameters:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="${jobStart}" end="${jobEnd}" timezone="${timezone}"
                    xmlns="uri:oozie:coordinator:0.1">
      <parameters>
          <property>
              <name>jobStart</name>
          </property>
          <property>
              <name>jobEnd</name>
              <value>2012-12-01T22:00Z</value>
          </property>
      </parameters>
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="${logsInitialInstance}" timezone="${timezone}">
          <uri-template>
            hdfs://bar:8020/app/logs/${market}/${language}/${YEAR}${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

In the above example, if `jobStart` is not specified, Oozie will print an error message instead of submitting the job. If
`jobEnd` is not specified, Oozie will use the default value, `2012-12-01T22:00Z`.

### 6.6. Parameterization of Dataset Instances in Input and Output Events

A coordinator application job typically launches several coordinator actions during its lifetime. A coordinator action typically uses its creation (materialization) time to resolve the specific datasets instances required for its input and output events.

The following EL functions are the means for binding the coordinator action creation time to the datasets instances of its input and output events.

#### 6.6.1. coord:current(int n) EL Function for Synchronous Datasets

`${coord:current(int n)}` represents the n<sup>th</sup> dataset instance for a **synchronous** dataset, relative to the coordinator action creation (materialization) time. The coordinator action creation (materialization) time is computed based on the coordinator job start time and its frequency. The n<sup>th</sup> dataset instance is computed based on the dataset's initial-instance datetime, its frequency and the (current) coordinator action creation (materialization) time.

`n` can be a negative integer, zero or a positive integer.

`${coord:current(int n)}` returns the nominal datetime for n<sup>th</sup> dataset instance relative to the coordinator action creation (materialization) time.

`${coord:current(int n)}` performs the following calculation:


```
DS_II : dataset initial-instance (datetime)
DS_FREQ: dataset frequency (minutes)
CA_NT: coordinator action creation (materialization) nominal time

coord:current(int n) = DS_II + DS_FREQ * ( (CA_NT - DS_II) div DS_FREQ + n)
```

NOTE: The formula above is not 100% correct, because DST changes the calculation has to account for hour shifts. Oozie Coordinator must make the correct calculation accounting for DST hour shifts.

When a positive integer is used with the `${coord:current(int n)}`, it refers to a dataset instance in the future from the coordinator action creation (materialization) time. This can be useful when creating dataset instances for future use by other systems.

The datetime returned by `${coord:current(int n)}` returns the exact datetime for the computed dataset instance.

**IMPORTANT:** The coordinator engine does use output events to keep track of new dataset instances. Workflow jobs triggered from coordinator actions can leverage the coordinator engine capability to synthesize dataset instances URIs to create output directories.

**<font color="#008000"> Examples: </font>**

1. **`${coord:current(int n)}` datetime calculation:**

    Datasets Definition:


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T24:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}</uri-template>
      </dataset>
    .
      <dataset name="weeklySiteAccessStats" frequency="${coord:days(7)}"
               initial-instance="2009-01-07T24:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/weeklystats/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    For a coordinator action creation time: `2009-05-29T24:00Z` the `${coord:current(int n)}` EL function would resolve to the following datetime values for the 'logs' and 'weeklySiteStats' datasets:

    | **${coord:current(int offset)}** | **Dataset 'logs'** | **Dataset 'weeklySiteAccessStats'** |
| --- | --- | --- |
    | `${coord:current(0)}` | `2009-05-29T24:00Z` | `2009-05-27T24:00Z` |
    | `${coord:current(1)}` | `2009-05-30T24:00Z` | `2009-06-03T24:00Z` |
    | `${coord:current(-1)}` | `2009-05-28T24:00Z` | `2009-05-20T24:00Z` |
    | `${coord:current(-3)}` | `2009-05-26T24:00Z` | `2009-05-06T24:00Z` |

    Note, in the example above, how the datetimes resolved for the 2 datasets differ when the `${coord:current(int n)}` function is invoked with the same argument. This is because the `${coord:current(int n)}` function takes into consideration the initial-time and the frequency for the dataset for which is performing the calculation.

    Datasets Definition file 'datasets.xml':


    ```
    <datasets>

      <dataset name="logs" frequency="${coord:hours(1)}"
               initial-instance="2009-01-01T01:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}/${HOUR}</uri-template>
      </dataset>

    </datasets>
    ```

    a. Coordinator application definition that creates a coordinator action once a day for a year, that is 365 coordinator actions:


    ```
       <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                        start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.1">
          <datasets>
            <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
          </datasets>
          <input-events>
            <data-in name="input" dataset="logs">
              <start-instance>${coord:current(-23)}</start-instance>
              <end-instance>${coord:current(0)}</end-instance>
            </data-in>
          </input-events>
          <action>
            <workflow>
            ...
           </workflow>
          </action>
       </coordinator-app>
    ```

    Each coordinator action will require as input events the last 24 (-23 to 0) dataset instances for the 'logs' dataset. Because the dataset 'logs' is a hourly dataset, it means all its instances for the last 24 hours.

    In this case, the dataset instances are used in a rolling window fashion.

    b. Coordinator application definition that creates a coordinator action once an hour for a year, that is 8760 (24*8760) coordinator actions:


    ```
       <coordinator-app name="app-coord" frequency="${coord:hours(1)}"
                        start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.1">
          <datasets>
            <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
          </datasets>
          <input-events>
            <data-in name="input" dataset="logs">
              <start-instance>${coord:current(-23)}</start-instance>
              <end-instance>${coord:current(0)}</end-instance>
            </data-in>
          </input-events>
          <action>
            <workflow>
            ...
           </workflow>
          </action>
       </coordinator-app>
    ```

    Each coordinator action will require as input events the last 24 (-23 to 0) dataset instances for the 'logs' dataset. Similarly to the previous coordinator application example, it means all its instances for the last 24 hours.

    However, because the frequency is hourly instead of daily, each coordinator action will use the last 23 dataset instances used by the previous coordinator action plus a new one.

    In this case, the dataset instances are used in a sliding window fashion.

3. **Using `${coord:current(int n)}` to specify dataset instances created by a coordinator application:**

    Datasets Definition file 'datasets.xml':


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:hours(1)}"
               initial-instance="2009-01-01T01:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
      </dataset>
    .
      <dataset name="stats" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T24:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    Coordinator application definition:


    ```
       <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                        start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.1">
          <datasets>
            <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
          </datasets>
          <input-events>
            <data-in name="input" dataset="logs">
              <start-instance>${coord:current(-23)}</start-instance>
              <end-instance>${coord:current(0)}</end-instance>
            </data-in>
          </input-events>
          <output-events>
            <data-out name="output" dataset="stats">
              <instance>${coord:current(0)}</instance>
            </data-out>
          </output-events>
          <action>
            <workflow>
            ...
           </workflow>
          </action>
       </coordinator-app>
    ```

    This coordinator application creates a coordinator action once a day for a year, this is 365 coordinator actions.

    Each coordinator action will require as input events the last 24 (-23 to 0) dataset instances for the 'logs' dataset.

    Each coordinator action will create as output event a new dataset instance for the 'stats' dataset.

    Note that the 'stats' dataset initial-instance and frequency match the coordinator application start and frequency.

4. **Using `${coord:current(int n)}` to create a data-pipeline using a coordinator application:**

    This example shows how to chain together coordinator applications to create a data pipeline.

    Dataset definitions file 'datasets.xml':


    ```
       <!--- Dataset A - produced every 15 minutes. -->
    .
      <dataset name="15MinLogs" frequency="${coord:minutes(15)}"
               initial-instance="2009-01-01T00:15:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
      </dataset>
    .
      <dataset name="1HourLogs" frequency="${coord:hours(1)}"
               initial-instance="2009-01-01T01:00:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
      </dataset>
    .
      <dataset name="1DayLogs" frequency="${coord:hours(24)}"
               initial-instance="2009-01-01T24:00:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
    ```

    Coordinator application definitions. A data-pipeline with two coordinator-applications, one scheduled to run every hour, and another scheduled to run every day:


    ```
       <coordinator-app name="app-coord-hourly" frequency="${coord:hours(1)}"
                        start="2009-01-01T01:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.1">
          <datasets>
            <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
          </datasets>
          <input-events>
            <data-in name="input" dataset="15MinLogs">
              <start-instance>${coord:current(-3)}</start-instance>
              <end-instance>${coord:current(0)}</end-instance>
            </data-in>
          </input-events>
          <output-events>
            <data-out name="output" dataset="1HourLogs">
              <instance>${coord:current(0)}</instance>
            </data-out>
          </output-events>
          <action>
            <workflow>
            ...
           </workflow>
          </action>
       </coordinator-app>
    ```


    ```
       <coordinator-app name="app-coord-daily" frequency="${coord:days(1)}"
                        start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.1">
          <datasets>
            <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
          </datasets>
          <input-events>
            <data-in name="input" dataset="1HourLogs">
              <start-instance>${coord:current(-23)}</start-instance>
              <end-instance>${coord:current(0)}</end-instance>
            </data-in>
          </input-events>
          <output-events>
            <data-out name="output" dataset="1DayLogs">
              <instance>${coord:current(0)}</instance>
            </data-out>
          </output-events>
          <action>
            <workflow>
            ...
           </workflow>
          </action>
       </coordinator-app>
    ```

    The 'app-coord-hourly' coordinator application runs every every hour, uses 4 instances of the dataset "15MinLogs" to create one instance of the dataset "1HourLogs"

    The 'app-coord-daily' coordinator application runs every every day, uses 24 instances of "1HourLogs" to create one instance of "1DayLogs"

    The output datasets from the 'app-coord-hourly' coordinator application are the input to the 'app-coord-daily' coordinator application thereby forming a simple data-pipeline application.

#### 6.6.2. coord:offset(int n, String timeUnit) EL Function for Synchronous Datasets

`${coord:offset(int n, String timeUnit)}` represents the n<sup>th</sup> timeUnit, relative to the coordinator action creation
(materialization) time. The coordinator action creation (materialization) time is computed based on the coordinator job start time
and its frequency.

It is an alternative to the `${coord:current(int n)}` command (see previous section) and can be used anywhere
`${coord:current(int n)}` can be used. The difference between the two functions is that `${coord:current(int n)}` computes an offset
based on the n<sup>th</sup> multiple of the frequency, while `${coord:offset(int n, String timeUnit)}` computes an offset based on
the n<sup>th</sup> multiple of `timeUnit`.

`n` can be a negative integer, zero or a positive integer.

`timeUnit` can be any one of the following constants: `"MINUTE"`, `"HOUR"`, `"DAY"`, `"MONTH"`, `"YEAR"`

`${coord:offset(int n, String timeUnit)}` returns the nominal datetime for n<sup>th</sup> timeUnit relative to the coordinator
action creation (materialization) time.

When used directly, `${coord:offset(int n, String timeUnit)}` performs the following calculation:


```
DS_FREQ: dataset frequency (minutes)
CA_NT = coordinator action creation (materialization) nominal time
coord:offset(int n, String timeUnit) = CA_NT + floor(timeUnit ** n div DS_FREQ) ** DS_FREQ
```

NOTE: The formula above is not 100% correct, because DST changes the calculation has to account for hour shifts. Oozie Coordinator
must make the correct calculation accounting for DST hour shifts.

When used in 'instance' or 'end-instance' XML elements, the above equation is used; the effect of the floor function is to
"rewind" the resolved datetime to match the latest instance before the resolved time.
When used in 'start-instance' XML elements, a slight modification to the above equation is used; instead of being "rewinded", the
resolved datetime is "fastforwarded" to match the earliest instance after the resolved time.
See the next two examples for more information.

**<font color="#008000"> Examples: </font>**

1. **`${coord:offset(int n, String timeUnit)}` datetime calculation:**

    Datasets Definition:


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T24:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}</uri-template>
      </dataset>
    .
      <dataset name="weeklySiteAccessStats" frequency="${coord:days(7)}"
               initial-instance="2009-01-07T24:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/weeklystats/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    For a coordinator action creation time: `2009-05-29T24:00Z` the `${coord:offset(int n, String timeUnit)}` EL function would resolve
    to the following datetime values for the 'logs' and 'weeklySiteStats' datasets:

    | **${coord:offset(int n, String timeUnit)}** | **Dataset 'logs'** | **Dataset 'weeklySiteAccessStats'** |
| --- | --- | --- |
    | `${coord:offset(0, "MINUTE")}` <br/> `${coord:offset(0, "HOUR")}` <br/> `${coord:offset(0, "DAY")}` <br/> `${coord:offset(0, "MONTH")}` <br/> `${coord:offset(0, "YEAR")}` | `2009-05-29T24:00Z` | `2009-05-27T24:00Z` |
    | `${coord:offset(1440, "MINUTE")}` <br/> `${coord:offset(24, "HOUR")}` <br/> `${coord:offset(1, "DAY")}` | `2009-05-30T24:00Z` | `2009-05-27T24:00Z` |
    | `${coord:offset(-1440, "MINUTE")}` <br/> `${coord:offset(-24, "HOUR")}` <br/> `${coord:offset(-1, "DAY")}` | `2009-05-28T24:00Z` | `2009-05-20T24:00Z` |
    | `${coord:offset(-4320, "MINUTE")}` <br/> `${coord:offset(-72, "HOUR")}` <br/> `${coord:offset(-3, "DAY")}` | `2009-05-26T24:00Z` | `2009-05-20T24:00Z` |
    | `${coord:offset(11520, "MINUTE")}` <br/> `${coord:offset(192, "HOUR")}` <br/> `${coord:offset(8, "DAY")}` | `2009-06-06T24:00Z` | `2009-06-03T24:00Z` |
    | `${coord:offset(10, "MINUTE")}` | `2009-05-29T24:00Z` | `2009-05-27T24:00Z` |

    Some things to note about the above example:

       1. When `n` is 0, the `timeUnit` doesn't really matter because 0 minutes is the same as 0 hours, 0 days, etc
       2. There are multiple ways to express the same value (e.g. `${coord:offset(24, "HOUR")}` is equivalent to `${coord:offset(1, "DAY")}`)
       3. The datetimes resolved for the 2 datasets differ when the `${coord:offset(int n, String timeUnit)}` function is invoked with the same arguments. This is because the `${coord:offset(int n, String timeUnit)}` function takes into consideration the initial-time and the frequency for the dataset for which is performing the calculation.
       4. As mentioned before, if the resolved time doesn't fall exactly on an instance, it will get "rewinded" to match the latest instance before the resolved time.  For example, `${coord:offset(1, "DAY")}` is resolved to `2009-05-27T24:00Z` for the 'weeklysiteStats' dataset even though this is the same as `${coord:offset(0, "DAY")}`; this is because the frequency is 7 days, so `${coord:offset(1, "DAY")}` had to be "rewinded".

2. **"fastforwarding" in \<start-instance\> `${coord:offset(int n, String timeUnit)}` calculation:**

    When specifying dataset instances, keep in mind that the resolved value of `${coord:offset(int n, String timeUnit)}` must line up
    with an offset of a multiple of the frequency when used in an 'instance' XML element.
    However, when used in `'start-instance'` and `'end-instance'` XML elements, this is not a requirement.  In this case, the function
    will automatically resolve the range of instances to match the offset of a multiple of the frequency that would fall between the
    `'start-instance'` and `'end-instance'` XML elements; in other words, `'start-instance'` XML element is "fastforwarded" while
    `'end-instance'` XML element is "rewinded".  So, in the example below, the frequency of the "logs" dataset is 1 hour while the
    `'start-instance'` XML element is `${coord:offset(-90, "MINUTE")}` (-1.5 hours).  If this were in an `'instance'` XML element, it
    would be "rewinded", but here it is effectively equivalent to `${coord:offset(-60, "MINUTE")}` or `${coord:current(-1)}` as we are
    dealing with a range.

    Datasets Definition file 'datasets.xml':


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:hours(1)}"
               initial-instance="2009-01-01T01:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
      </dataset>
    .
      <dataset name="stats" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T24:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    Coordinator application definition:


    ```
       <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                        start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.1">
          <datasets>
            <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
          </datasets>
          <input-events>
            <data-in name="input" dataset="logs">
              <start-instance>${coord:offset(-90, "MINUTE")}</start-instance>
              <end-instance>${coord:offset(0, "DAY")}</end-instance>
            </data-in>
          </input-events>
          <output-events>
            <data-out name="output" dataset="stats">
              <instance>${coord:offset(0, "DAY")}</instance>
            </data-out>
          </output-events>
          <action>
            <workflow>
            ...
           </workflow>
          </action>
       </coordinator-app>
    ```

#### 6.6.3. coord:hoursInDay(int n) EL Function for Synchronous Datasets

The `${coord:hoursInDay(int n)}` EL function returns the number of hours for the specified day, in a timezone/daylight-saving sensitive way.

`n` is offset (in days) from the current nominal time. A negative value is the n<sup>th</sup> previous day. Zero is the current day. A positive number is the n<sup>th</sup> next day.

The returned value is calculated taking into account timezone daylight-saving information.

Normally it returns `24`, only DST switch days for the timezone in question it will return either `23` or `25`.

**<font color="#008000"> Examples: </font>**

| **Nominal UTC time** | **Timezone** | **EndOfFlag** | **Usage**  | **Value** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2009-01-01T08:00Z` | `UTC` |  `NO` |`${coord:hoursInDay(0)}` | 24 | hours in 2009JAN01 UTC |
| `2009-01-01T08:00Z` | `America/Los_Angeles` |   `NO` |`${coord:hoursInDay(0)}` | 24 | hours in 2009JAN01 PST8PDT time |
| `2009-01-01T08:00Z` | `America/Los_Angeles` |   `NO` |`${coord:hoursInDay(-1)}` | 24 | hours in 2008DEC31 PST8PDT time |
| ||||| |
| `2009-03-08T08:00Z` | `UTC` |  `NO` | `${coord:hoursInDay(0)}` | 24 | hours in 2009MAR08 UTC time |
| `2009-03-08T08:00Z` | `Europe/London` |  `NO` | `${coord:hoursInDay(0)}` | 24 | hours in 2009MAR08 BST1BDT time |
| `2009-03-08T08:00Z` | `America/Los_Angeles` |  `NO` | `${coord:hoursInDay(0)}` | 23 | hours in 2009MAR08 PST8PDT time <br/> (2009MAR08 is DST switch in the US) |
| `2009-03-08T08:00Z` | `America/Los_Angeles` |  `NO` | `${coord:hoursInDay(1)}` | 24 | hours in 2009MAR09 PST8PDT time |
| `2009-03-07T08:00Z` | `America/Los_Angeles` |  `EndOfDay` | `${coord:hoursInDay(0)}` | 24 | hours in 2009MAR07 PST8PDT time |
| `2009-03-07T08:00Z` | `America/Los_Angeles` |  `EndOfDay` | `${coord:hoursInDay(1)}` | 23 | hours in 2009MAR08 PST8PDT time <br/> (2009MAR08 is DST switch in the US) |


Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="${jobStart}" end="${jobEnd}" timezone="${timezone}"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="${logsInitialInstance}" timezone="${timezone}">
          <uri-template>
            hdfs://bar:8020/app/logs/${market}/${language}/${YEAR}${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <start-instance>${coord:current( -(coord:hoursInDay(0) - 1) )}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

This example is the example of section #6.5 but with a minor change. The argument for the `${coord:current(int n)}` function in the 'start-instance' element, instead using `-23`, the example now uses `-(coord:hoursInDay(0) - 1)`.

This simple change fully enables this coordinator application to handle daily data (produced hourly) for any timezone, with timezones observing or not daylight saving.

For timezones observing daylight saving, on the days of DST switch, the function will resolve to `23` or `25`, thus the dataset instances used will be for for the day in the DST sense.

For timezones not observing daylight saving, it always returns `24`.

#### 6.6.4. coord:daysInMonth(int n) EL Function for Synchronous Datasets

The `${coord:daysInMonth(int n)}` EL function returns the number of days for month of the specified day.

`n` is offset (in months) from the current nominal time. A negative value is the n<sup>th</sup> previous month. Zero is the current month. A positive number is the n<sup>th</sup> next month.

The returned value is calculated taking into account leap years information.

The `${coord:daysInMonth(int n)}` EL function can be used to express monthly ranges for dataset instances.

**<font color="#008000"> Examples: </font>**

| **Nominal UTC time** | **Timezone** |**EndOfFlag** | **Usage**  | **Value** | **Comments** |
| --- | --- | --- | --- | --- | --- |
| `2008-02-01T00:00Z` | `UTC` | `NO` | `${coord:daysInMonth(0)}` | 29 | days in 2008FEB UTC time |
| `2009-02-01T00:00Z` | `UTC` | `NO` | `${coord:daysInMonth(0)}` | 28 | days in 2009FEB UTC time |
| `2009-02-01T00:00Z` | `UTC` | `NO` | `${coord:daysInMonth(-1)}` | 31 | days in 2009JAN UTC time |
| `2009-03-01T00:00Z` | `UTC` | `NO` | `${coord:daysInMonth(1)}` | 30 | days in 2009APR UTC time |
| `2009-02-01T00:00Z` | `Americas/Los_Angeles` | `NO` |`${coord:daysInMonth(0)}` | 31 | days in 2009JAN PST8PDT time, note that the nominal time is UTC |
|||||||
| `2008-02-01T00:00Z` | `UTC` | `EndOfMonth` | `${coord:daysInMonth(0)}` | 29 | days in 2008FEB UTC time |
| `2008-02-01T00:00Z` | `UTC` | `EndOfMonth` | `${coord:daysInMonth(-1)}` | 31 | days in 2008JAN UTC time |
| `2009-02-01T00:00Z` | `UTC` | `EndOfMonth` | `${coord:daysInMonth(0)}` | 28 | days in 2009FEB UTC time |
| `2009-02-01T00:00Z` | `UTC` | `EndOfMonth` | `${coord:daysInMonth(-1)}` | 31 | days in 2009JAN UTC time |
| `2009-03-01T00:00Z` | `UTC` | `EndOfMonth` | `${coord:daysInMonth(1)}` | 30 | days in 2009APR UTC time |
| `2009-02-01T00:00Z` | `Americas/Los_Angeles` | `EndOfMonth` |`${coord:daysInMonth(0)}` | 31 | days in 2009JAN PST8PDT time, note that the nominal time is UTC |



Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:months(1)}"
                    start="2009-01-31T24:00Z" end="2009-12-31T24:00" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:days(1)}"
                 initial-instance="2009-01-01T24:00Z" timezone="UTC">
          <uri-template>
            hdfs://bar:8020/app/logs/${market}/${language}/${YEAR}${MONTH}/${DAY}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <start-instance>${coord:current( -(coord:daysInMonth(0) - 1) )}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

This example is a coordinator application that runs monthly, and consumes the daily feeds for the last month.

#### 6.6.5. coord:tzOffset() EL Function for Synchronous Datasets

`${coord:tzOffset()}` EL function returns the difference in **minutes** between a dataset timezone and the coordinator job timezone at the current nominal time. This EL function is useful when dealing with datasets from multiple timezones, but execute in a different timezone.


```
  DS_TZ : dataset TZ offset in minutes at the current nominal time (UTC offset)
  JOB_TZ: coordinator job UTC TZ offset in minutes at the current nominal time (UTC offset).

  coord:tzOffset() = DS_TZ - JOB_TZ
```

For example: Los Angeles Winter offset (no DST) is `-480` (-08:00 hours). India offset is `-330` (+05:30 hours).

The value returned by this function may change because of the daylight saving rules of the 2 timezones. For example, between Continental Europe and The U.S. West coast, most of the year the timezone different is 9 hours, but there are a few day or weeks.

IMPORTANT: While the offset is multiples of 60 for most timezones, it can be multiple of 30 mins when one of the timezones is has a `##:30` offset (i.e. India).

Refer to section #7, 3nd use case for a detailed example.

#### 6.6.6. coord:latest(int n) EL Function for Synchronous Datasets

`${coord:latest(int n)}` represents the n<sup>th</sup> latest currently available instance of a **synchronous** dataset.

`${coord:latest(int n)}` is not relative to the coordinator action creation (materialization) time, it is the n<sup>th</sup> latest instance available when the action is started (when the workflow job is started).

If a coordinator job is suspended, when resumed, all usages of `${coord:latest(int n)}` will be resolved to the currently existent instances.

Finally, it is not possible to represent the latest dataset when execution reaches a node in the workflow job. The resolution of latest dataset instances happens at action start time (workflow job start time).

The parameter `n` can be a negative integer or zero. Where `0` means the latest instance available, `-1` means the second latest instance available, etc.

the `${coord:latest(int n)}` ignores gaps in dataset instances, it just looks for the latest n<sup>th</sup> instance available.

**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:hours(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
            hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <instance>${coord:latest(-2)}</instance>
          <instance>${coord:latest(0)}</instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

If the available dataset instances in HDFS at time of a coordinator action being executed are:


```
  hdfs://bar:8020/app/logs/2009/01/01
  hdfs://bar:8020/app/logs/2009/01/02
  hdfs://bar:8020/app/logs/2009/01/03
  	(missing)
  hdfs://bar:8020/app/logs/2009/01/05
  (missing)
  hdfs://bar:8020/app/logs/2009/01/07
  (missing)
  (missing)
  hdfs://bar:8020/app/logs/2009/01/10
```

Then, the dataset instances for the input events for the coordinator action will be:


```
  hdfs://bar:8020/app/logs/2009/01/05
  hdfs://bar:8020/app/logs/2009/01/10
```

#### 6.6.7. coord:future(int n, int limit) EL Function for Synchronous Datasets

`${coord:future(int n, int limit)}` represents the n<sup>th</sup> currently available future instance of a **synchronous** dataset while looking ahead for 'limit' number of instances.

`${coord:future(int n, int limit)}` is  relative to the coordinator action creation (materialization) time. The coordinator action creation (materialization) time is computed based on the coordinator job start time and its frequency. The n<sup>th</sup> dataset instance is computed based on the dataset's initial-instance datetime, its frequency and the (current) coordinator action creation (materialization) time.

`n` can be a zero or a positive integer. Where `0` means the immediate instance available, `1` means the second next instance available, etc.

`limit` should  be a positive integer.  Where `3` means search for n<sup>th</sup> next instance and should not check beyond `3` instance.

The `${coord:future(int n, int limit)}` ignores gaps in dataset instances, it just looks for the next n<sup>th</sup> instance available.

**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:hours(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
            hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="input" dataset="logs">
          <instance>${coord:future(0, 10)}</instance>
          <instance>${coord:future(2, 10)}</instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

If the available dataset instances in HDFS at time of a coordinator action being executed are:


```
  hdfs://bar:8020/app/logs/2009/02/01
  (missing)
  (missing)
  (missing)
  hdfs://bar:8020/app/logs/2009/02/04
 (missing)
 (missing)
  hdfs://bar:8020/app/logs/2009/02/07
  (missing)
  (missing)
  (missing)
  hdfs://bar:8020/app/logs/2009/02/11
  (missing)
  (missing)
  hdfs://bar:8020/app/logs/2009/02/14
  (missing)
  hdfs://bar:8020/app/logs/2009/02/16
```

Then, the dataset instances for the input events for the coordinator action will be:


```
  hdfs://bar:8020/app/logs/2009/02/01
  hdfs://bar:8020/app/logs/2009/02/07
```


#### 6.6.8. coord:absolute(String timeStamp) EL Function for Synchronous Datasets

`${coord:absolute(String timeStamp)}` represents absolute dataset instance time. coord:absolute is only supported with range
where, start-instance is coord:absolute and end-instance is coord:current. Specifying a fixed date as the start instance is
useful if your processing needs to process all dataset instances from a specific instance to the current instance.


**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
<coordinator-app name="app-coord" frequency="${coord:months(1)}"
                    start="2009-01-01T01:00Z" end="2009-12-31T00:00" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.4">
    <input-events>
        <data-in name="input" dataset="logs">
            <dataset name='a' frequency='7' initial-instance="2009-01-01T01:00Z"
                timezone='UTC' freq_timeunit='DAY' end_of_duration='NONE'>
                <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}
                </uri-template>
            </dataset>
            <start-instance>${coord:absolute("2009-01-01T01:00Z")}</start-instance>
            <end-instance>${coord:current(0)}</end-instance>
        </data-in>
    </input-events>
    <action>
        <workflow>
        .............
        </workflow>
    </action>
</coordinator-app>
```

Then, the dataset instances for the input events for the coordinator action at first run will be:


```
  hdfs://bar:8020/app/logs/2009/02/01
```

The dataset instances for the input events for the coordinator action at second run will be:


```
  hdfs://bar:8020/app/logs/2009/02/01
  hdfs://bar:8020/app/logs/2009/02/07
```


#### 6.6.9. coord:endOfMonths(int n) EL Function for Synchronous Datasets


`${coord:endOfMonths(int n)}` represents dataset instance at start of n <sup>th</sup> month. coord:endOfMonths is only
supported with range, where start-instance is coord:endOfMonths and end-instance is coord:current. Specifying start of
a month is useful if you want to process all the dataset instances from starting of a month to the current instance.


**<font color="#008000"> Examples: </font>**

1. **. `${coord:endOfMonths(int n)}` datetime calculation:**

    Datasets Definition:


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T00:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    For a coordinator action creation time: `2009-05-29T00:00Z` the `${coord:endOfMonths(int n)}` EL function
    would resolve to the following datetime values for the 'logs'  dataset:

    | **${coord:endOfMonths(int offset)}** | **Dataset 'logs'** | **Comments** |
| --- | --- | --- |
    | `${coord:endOfMonths(-1)}` | `2009-05-01T00:00Z` | End of last month i.e. start of current month |
    | `${coord:endOfMonths(0)}` | `2009-06-01T00:00Z` | End of current month i.e. start of next month |
    | `${coord:endOfMonths(-2)}` | `2009-04-01T00:00Z` | |
    | `${coord:endOfMonths(-3)}` | `2009-03-01T00:00Z` | |

    **<font color="#008000"> Example: </font>**:

    Coordinator application definition:


    ```
    <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                        start='2009-08-06T00:00Z' end="2009-12-31T00:00" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.4">
        <input-events>
            <data-in name="input" dataset="logs">
                <dataset name='a' frequency='1' initial-instance='2009-06-06T00:00Z'
                    timezone='UTC' freq_timeunit='DAY' end_of_duration='NONE'>
                    <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}
                    </uri-template>
                </dataset>
                <start-instance>${coord:endOfMonths(-1)}</start-instance>
                <end-instance>${coord:current(0)}</end-instance>
            </data-in>
        </input-events>
        <action>
            <workflow>
            .............
            </workflow>
        </action>
    </coordinator-app>
    ```

    Then, the dataset instances for the input events for the coordinator action at first run will be:


    ```
      hdfs://bar:8020/app/logs/2009/08/01
      hdfs://bar:8020/app/logs/2009/08/02
      hdfs://bar:8020/app/logs/2009/08/02
      hdfs://bar:8020/app/logs/2009/08/03
      hdfs://bar:8020/app/logs/2009/08/04
      hdfs://bar:8020/app/logs/2009/08/05
      hdfs://bar:8020/app/logs/2009/08/06
    ```

    The dataset instances for the input events for the coordinator action at second run will be:


    ```
      hdfs://bar:8020/app/logs/2009/08/01
      hdfs://bar:8020/app/logs/2009/08/02
      hdfs://bar:8020/app/logs/2009/08/02
      hdfs://bar:8020/app/logs/2009/08/03
      hdfs://bar:8020/app/logs/2009/08/04
      hdfs://bar:8020/app/logs/2009/08/05
      hdfs://bar:8020/app/logs/2009/08/06
      hdfs://bar:8020/app/logs/2009/08/07
    ```


#### 6.6.10. coord:endOfWeeks(int n) EL Function for Synchronous Datasets


`${coord:endOfWeeks(int n)}` represents dataset instance at start of n <sup>th</sup> week. The start of the week
calculated similar to mentioned in
[coord:endOfWeeks section above](CoordinatorFunctionalSpec.html#a4.4.3._The_coord:endOfWeeksint_n_EL_function).
coord:endOfWeeks is only supported with range, where start-instance is coord:endOfWeeks and end-instance is
coord:current. Specifying start of a week is useful if you want to process all the dataset instances from
starting of a week to the current instance.

**<font color="#008000"> Examples: </font>**

1. **. `${coord:endOfWeeks(int n)}` datetime calculation:**

    Datasets Definition:


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T00:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    For a coordinator action creation time: `2009-05-29T00:00Z` the `${coord:endOfWeeks(int n)}` EL function
    would resolve to the following datetime values for the 'logs'  dataset:

    | **${coord:endOfWeeks(int offset)}** | **Dataset 'logs'** | **Comments** |
| --- | --- | --- |
    | `${coord:endOfWeeks(-1)}` | `2009-05-24T00:00Z` | End of last week i.e. start of current week |
    | `${coord:endOfWeeks(0)}` | `2009-05-31T00:00Z` | End of current week i.e. start of next week |
    | `${coord:endOfWeeks(-2)}` | `2009-05-17T00:00Z` | |
    | `${coord:endOfWeeks(-4)}` | `2009-05-03T00:00Z` | |


    **<font color="#008000"> Example: </font>**:

    Coordinator application definition:


    ```
    <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                        start='2009-08-06T00:00Z' end="2009-12-31T00:00" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.4">
        <input-events>
            <data-in name="input" dataset="logs">
                <dataset name='a' frequency='1' initial-instance='2009-06-06T00:00Z'
                    timezone='UTC' freq_timeunit='DAY' end_of_duration='NONE'>
                    <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}
                    </uri-template>
                </dataset>
                <start-instance>${coord:endOfWeeks(-1)}</start-instance>
                <end-instance>${coord:current(0)}</end-instance>
            </data-in>
        </input-events>
        <action>
            <workflow>
            .............
            </workflow>
        </action>
    </coordinator-app>
    ```

    Then, the dataset instances for the input events for the coordinator action at first run will be:


    ```
      hdfs://bar:8020/app/logs/2009/08/02
      hdfs://bar:8020/app/logs/2009/08/03
      hdfs://bar:8020/app/logs/2009/08/04
      hdfs://bar:8020/app/logs/2009/08/05
      hdfs://bar:8020/app/logs/2009/08/06
    ```

    The dataset instances for the input events for the coordinator action at second run will be:


    ```
      hdfs://bar:8020/app/logs/2009/08/02
      hdfs://bar:8020/app/logs/2009/08/03
      hdfs://bar:8020/app/logs/2009/08/04
      hdfs://bar:8020/app/logs/2009/08/05
      hdfs://bar:8020/app/logs/2009/08/06
      hdfs://bar:8020/app/logs/2009/08/07
    ```


#### 6.6.11. coord:endOfDays(int n) EL Function for Synchronous Datasets


`${coord:endOfDays(int n)}` represents dataset instance at start of n <sup>th</sup> day. coord:endOfDays is only
supported with range, where start-instance is coord:endOfDays and end-instance is coord:current. Specifying start
of a day is useful if you want to process all the dataset instances from starting of a day to the current instance.

**<font color="#008000"> Examples: </font>**

1. **. `${coord:endOfDays(int n)}` datetime calculation:**

    Datasets Definition:


    ```
    <datasets>
    .
      <dataset name="logs" frequency="${coord:days(1)}"
               initial-instance="2009-01-01T00:00Z" timezone="UTC">
        <uri-template>hdfs://bar:8020/app/logs/${YEAR}${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
      </dataset>
    .
    </datasets>
    ```

    For a coordinator action creation time: `2009-05-25T23:00Z` the `${coord:endOfDays(int n)}` EL function
    would resolve to the following datetime values for the 'logs'  dataset:

    | **${coord:endOfDays(int offset)}** | **Dataset 'logs'** | **Comments** |
| --- | --- | --- |
    | `${coord:endOfDays(-1)}` | `2009-05-25T00:00Z` | End of previous day i.e. start of the current day |
    | `${coord:endOfDays(0)}` | `2009-05-26T00:00Z` | End of current day i.e. start of the next day |
    | `${coord:endOfDays(-2)}` | `2009-05-24T00:00Z` | |
    | `${coord:endOfDays(-4)}` | `2009-05-22T00:00Z` | |

    **<font color="#008000"> Example: </font>**:

    Coordinator application definition:


    ```
    <coordinator-app name="app-coord" frequency='60' freq_timeunit='MINUTE'
                        start='2009-08-06T00:00Z' end="2009-12-31T00:00" timezone="UTC"
                        xmlns="uri:oozie:coordinator:0.4">
        <input-events>
            <data-in name="input" dataset="logs">
                <dataset name='a' frequency='60' initial-instance='2009-06-06T00:00Z'
                    timezone='UTC' freq_timeunit='MINUTE' end_of_duration='NONE'>
                    <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}
                    </uri-template>
                </dataset>
                <start-instance>${coord:endOfDays(-1)}</start-instance>
                <end-instance>${coord:current(0)}</end-instance>
            </data-in>
        </input-events>
        <action>
            <workflow>
            .............
            </workflow>
        </action>
    </coordinator-app>
    ```

    Then, the dataset instances for the input events for the coordinator action at first run will be:


    ```
      hdfs://bar:8020/app/logs/2009/08/06/00/00
      hdfs://bar:8020/app/logs/2009/08/06/01/00
      hdfs://bar:8020/app/logs/2009/08/06/02/00
      hdfs://bar:8020/app/logs/2009/08/06/03/00
      hdfs://bar:8020/app/logs/2009/08/06/04/00
      hdfs://bar:8020/app/logs/2009/08/06/05/00
    ```

    The dataset instances for the input events for the coordinator action at second run will be:


    ```
      hdfs://bar:8020/app/logs/2009/08/06/00/00
      hdfs://bar:8020/app/logs/2009/08/06/01/00
      hdfs://bar:8020/app/logs/2009/08/06/02/00
      hdfs://bar:8020/app/logs/2009/08/06/03/00
      hdfs://bar:8020/app/logs/2009/08/06/04/00
      hdfs://bar:8020/app/logs/2009/08/06/05/00
      hdfs://bar:8020/app/logs/2009/08/06/06/00
    ```


#### 6.6.12. coord:version(int n) EL Function for Asynchronous Datasets
   * TBD


#### 6.6.13. coord:latest(int n) EL Function for Asynchronous Datasets
   * TBD


#### 6.6.14. Dataset Instance Resolution for Instances Before the Initial Instance


When defining input events that refer to dataset instances it may be possible that the resolution of instances is out of it lower bound. This is scenario is likely to happen when the instance resolution is very close to the initial-instance. This is useful for bootstrapping the application.

To address this edge scenario, Oozie Coordinator **silently ignores dataset instances out of bounds.**

**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:hours(1)}"
                    start="2009-01-01T01:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="2009-01-01T00:00Z"  timezone="UTC">
          <uri-template>
            hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="inputLogs" dataset="logs">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
        ...
       </workflow>
      </action>
   </coordinator-app>
```

In the case of the synchronous 'logs' dataset, for the first action of this coordinator job, the instances referred in the input events will resolve to just 1 instance. For the second action it will resolve to 2 instances. And so on. Only after the 24th action, the input events will resolve constantly to 24 instances. In other words, while `${coord:current(-23)}` resolves to datetimes prior to the 'initial-instance' the required range will start from the 'initial-instance', '2009-01-01T00:00Z' in this example.

### 6.7. Parameterization of Coordinator Application Actions

Actions started by a coordinator application normally require access to the dataset instances resolved by the input and output events to be able to propagate them to the workflow job as parameters.

The following EL functions are the mechanism that enables this propagation.

#### 6.7.1. coord:dataIn(String name) EL Function

The `${coord:dataIn(String name)}` EL function resolves to all the URIs for the dataset instances specified in an input event dataset section.

The `${coord:dataIn(String name)}` is commonly used to pass the URIs of dataset instances that will be consumed by a workflow job triggered by a coordinator action.

**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="logs" frequency="${coord:hours(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
             hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="inputLogs" dataset="logs">
          <start-instance>${coord:current( -(coord:hoursInDay(0) - 1) )}</start-instance>
          <end-instance>${coord:current(-1)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
          <configuration>
            <property>
              <name>wfInput</name>
              <value>${coord:dataIn('inputLogs')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

In this example, each coordinator action will use as input events the last day hourly instances of the 'logs' dataset.

The `${coord:dataIn(String name)}` function enables the coordinator application to pass the URIs of all the dataset instances for the last day to the workflow job triggered by the coordinator action. For the "2009-01-02T00:00Z" run, the `${coord:dataIn('inputLogs')}` function will resolve to:


```
  hdfs://bar:8020/app/logs/2009/01/01/01,
  hdfs://bar:8020/app/logs/2009/01/01/02,
  ...
  hdfs://bar:8020/app/logs/2009/01/01/23,
  hdfs://bar:8020/app/logs/2009/02/00/00
```

The `${coord:dataIn('inputLogs')}` is used for workflow job configuration property 'wfInput' for the workflow job that will be submitted by the coordinator action on January 2nd 2009. Thus, when the workflow job gets started, the 'wfInput' workflow job configuration property will contain all the above URIs.

Note that all the URIs form a single string value and the URIs are separated by commas. Multiple HDFS URIs separated by commas can be specified as input data to a Map/Reduce job.

#### 6.7.2. coord:dataOut(String name) EL Function

The `${coord:dataOut(String name)}` EL function resolves to all the URIs for the dataset instance specified in an output event dataset section.

The `${coord:dataOut(String name)}` is commonly used to pass the URIs of a dataset instance that will be produced by a workflow job triggered by a coordinator action.

**<font color="#008000"> Example: </font>**:

Datasets Definition file 'datasets.xml'


```
<datasets>
.
  <dataset name="hourlyLogs" frequency="${coord:hours(1)}"
           initial-instance="2009-01-01T01:00Z" timezone="UTC">
    <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
  </dataset>
.
  <dataset name="dailyLogs" frequency="${coord:days(1)}"
           initial-instance="2009-01-01T24:00Z" timezone="UTC">
    <uri-template>hdfs://bar:8020/app/daily-logs/${YEAR}/${MONTH}/${DAY}</uri-template>
  </dataset>
</datasets>
```

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <include>hdfs://foo:8020/app/dataset-definitions/datasets.xml</include>
      </datasets>
      <input-events>
        <data-in name="inputLogs" dataset="hourlyLogs">
          <start-instance>${coord:current( -(coord:hoursInDay(0) -1) )}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <output-events>
        <data-out name="outputLogs" dataset="dailyLogs">
          <instance>${coord:current(0)}</instance>
        </data-out>
      </output-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsaggretor-wf</app-path>
          <configuration>
            <property>
              <name>wfInput</name>
              <value>${coord:dataIn('inputLogs')}</value>
            </property>
            <property>
              <name>wfOutput</name>
              <value>${coord:dataOut('outputLogs')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

In this example, each coordinator action will use as input events the last 24 hourly instances of the 'hourlyLogs' dataset to create a 'dailyLogs' dataset instance.

The `${coord:dataOut(String name)}` function enables the coordinator application to pass the URIs of the dataset instance that will be created by the workflow job triggered by the coordinator action. For the "2009-01-01T24:00Z" run, the `${coord:dataOut('dailyLogs')}` function will resolve to:


```
  hdfs://bar:8020/app/logs/2009/01/02
```

NOTE: The use of `24:00` as hour is useful for human to denote end of the day, but internally Oozie handles it as the zero hour of the next day.

The `${coord:dataOut('dailyLogs')}` is used for workflow job configuration property 'wfOutput' for the workflow job that will be submitted by the coordinator action on January 2nd 2009. Thus, when the workflow job gets started, the 'wfOutput' workflow job configuration property will contain the above URI.

#### 6.7.3. coord:nominalTime() EL Function

The `${coord:nominalTime()}` EL function resolves to the coordinator action creation (materialization) datetime.

The nominal times is always the coordinator job start datetime plus a multiple of the coordinator job frequency.

This is, when the coordinator action was created based on driver event. For synchronous coordinator applications this would be every tick of the frequency.

**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
     <datasets>
       <dataset name="hourlyLogs" frequency="${coord:hours(1)}"
                initial-instance="2009-01-01T01:00Z" timezone="UTC">
         <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
       </dataset>
     </datasets>
      <input-events>
        <data-in name="inputLogs" dataset="hourlyLogs">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <app-path>${nameNode}/user/${coord:user()}/examples/apps/aggregator</app-path>
        <configuration>
            <property>
              <name>nextInstance</name>
              <value>${coord:dateOffset(coord:nominalTime(), 1, 'DAY')}</value>
            </property>
            <property>
             <name>previousInstance</name>
              <value>${coord:dateOffset(coord:nominalTime(), -1, 'DAY')}</value>
            </property>
         </configuration>
      </action>
   </coordinator-app>
```

The nominal times for the coordinator actions of this coordinator application example are:


```
  2009-01-02T00:00Z
  2009-01-03T00:00Z
  2009-01-04T00:00Z
  ...
  2010-01-01T00:00Z
```

These are the times the action where created (materialized).

#### 6.7.4. coord:actualTime() EL Function

The `${coord:actualTime()}` EL function resolves to the coordinator action actual creation datetime.

When the coordinator action is created based on driver event, the current time is saved to action. An action's
actual time is less than the nominal time if coordinator job is in running in current mode. If job is running
as catch-up mode (job's start time is in the past), the actual time is greater than the nominal time.

**<font color="#008000"> Example: </font>**:

Coordinator application definition:


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2011-05-01T24:00Z" end="2011-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
     <datasets>
       <dataset name="hourlyLogs" frequency="${coord:hours(1)}"
                initial-instance="2011-04-01T01:00Z" timezone="UTC">
         <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
       </dataset>
     </datasets>
      <input-events>
        <data-in name="inputLogs" dataset="hourlyLogs">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
        <app-path>${nameNode}/user/${coord:user()}/examples/apps/aggregator</app-path>
        <configuration>
            <property>
              <name>actualTime</name>
              <value>${coord:formatTime(coord:actualTime(), 'yyyy-MM-dd')}</value>
            </property>
         </configuration>
      </action>
   </coordinator-app>
```

The actual time for the coordinator actions of this coordinator application example will look like:

If coordinator job was started at 2011-05-01, then actions' actualTime is

```
  2011-05-01
  2011-05-02
  2011-05-03
  ...
  2011-12-31
```

#### 6.7.5. coord:user() EL Function (since Oozie 2.3)

The `coord:user()` function returns the user that started the coordinator job.

### 6.8 Using HCatalog data instances in Coordinator Applications (since Oozie 4.x)

This section describes the different EL functions that work with HCatalog data dependencies, in order to write
Coordinator applications that use HCatalog data dependencies.

#### 6.8.1 coord:databaseIn(String name), coord:databaseOut(String name) EL function

The functions `${coord:databaseIn(String name)}` and `${coord:databaseOut(String name)}` are used to pass the database
name of HCat dataset instances, input and output respectively, that will be consumed by a workflow job triggered
by a coordinator action.

For input database, you should pass the "data-in" name attribute of your 'input-events' configured in the coordinator.
Similarly for output database, pass the "data-out" name attribute of your 'output-events'.

To illustrate it better:
If data belongs to 'input-events' and the name attribute of your `<data-in>` is "raw-logs",
use `${coord:databaseIn('raw-logs')}`.
Else if it belongs to 'output-events', and the name attribute of your `<data-out>` is "processed-logs",
use `${coord:databaseOut('processed-logs')}`.
Taking this passed argument as input, the EL functions give as string the 'database' name corresponding to your input or output data events.

Pitfall: Please note NOT to pass the `<dataset>` name itself (as defined under combined set `<datasets>`),
as this function works on the 'data-in' and 'data-out' names.

Refer to the [Example](CoordinatorFunctionalSpec.html#HCatPigExampleOne) below for usage.

#### 6.8.2 coord:tableIn(String name), coord:tableOut(String name) EL function

The functions `${coord:tableIn(String name)}` and `${coord:tableOut(String name)}` are used to pass the table
name of HCat dataset instances, input and output respectively, that will be consumed by a workflow job triggered
by a coordinator action.

For input table, you should pass the "data-in" name attribute of your 'input-events' configured in the coordinator.
Similarly for output table, pass the "data-out" name attribute of your 'output-events'.

To illustrate it better:
If data belongs to 'input-events' and the name attribute of your `<data-in>` is "raw-logs",
use `${coord:tableIn('raw-logs')}`.
Similarly, if it belongs to 'output-events', and the name attribute of your `<data-out>` is "processed-logs",
use `${coord:tableOut('processed-logs')}`.
Taking this passed argument as input, the EL functions give as string the 'table' name corresponding to your input or output data events.

Pitfall: Please note NOT to pass the `<dataset>` name itself (as defined under combined set `<datasets>`),
as this function works on the 'data-in' and 'data-out' names.

Refer to the [Example](CoordinatorFunctionalSpec.html#HCatPigExampleOne) below for usage.

#### 6.8.3 coord:dataInPartitionFilter(String name, String type) EL function

The `${coord:dataInPartitionFilter(String name, String type)}` EL function resolves to a filter clause to filter
all the partitions corresponding to the dataset instances specified in an input event dataset section. This EL function
takes two arguments - the name of the input dataset, and the type of the workflow action which will be consuming this filter.
There are 3 types - 'pig', 'hive' and 'java'. This filter clause from the EL function is to be passed as a parameter in the
respective action in the workflow.

The evaluated value of the filter clause will vary based on the action type passed to the EL function. In case of pig,
the filter will have "``" as the equality operator in the condition. In case of hive and java, the filter will have "="
as the equality operator in the condition. The type java is for java actions, which use HCatInputFormat directly and
launch jobs. The filter clause in that case can be used to construct the InputJobInfo in
`HCatInputFormat.setInput(Job job, InputJobInfo inputJobInfo)`.

Refer to the [Example](CoordinatorFunctionalSpec.html#HCatPigExampleOne) below for usage.

#### 6.8.4 coord:dataOutPartitions(String name) EL function

The `${coord:dataOutPartitions(String name)}` EL function resolves to a comma-separated list of partition key-value
pairs for the output-event dataset. This can be passed as an argument to HCatStorer in Pig scripts or in case of
java actions that directly use HCatOutputFormat and launch jobs, the partitions list can be parsed to construct
partition values map for OutputJobInfo in `HcatOutputFormat.setOutput(Job job, OutputJobInfo outputJobInfo)`.

The example below illustrates a pig job triggered by a coordinator, using the EL functions for HCat database, table,
input partitions filter and output partitions. The example takes as input previous day's hourly data to produce
aggregated daily output.


**<font color="#008000"> Example: </font>**

<a name="HCatPigExampleOne"></a>

**Coordinator application definition:**

```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.3">
      <datasets>
        <dataset name="Click-data" frequency="${coord:hours(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
             hcat://foo:11002/myInputDatabase/myInputTable/datestamp=${YEAR}${MONTH}${DAY}${HOUR};region=USA
          </uri-template>
        </dataset>
        <dataset name="Stats" frequency="${coord:days(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
             hcat://foo:11002/myOutputDatabase/myOutputTable/datestamp=${YEAR}${MONTH}${DAY}
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="raw-logs" dataset="Click-data">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <output-events>
        <data-out name="processed-logs" dataset="Stats">
          <instance>${coord:current(0)}</instance>
        </data-out>
      </output-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
          <configuration>
            <property>
              <name>IN_DB</name>
              <value>${coord:databaseIn('raw-logs')}</value>
            </property>
            <property>
              <name>IN_TABLE</name>
              <value>${coord:tableIn('raw-logs')}</value>
            </property>
            <property>
              <name>FILTER</name>
              <value>${coord:dataInPartitionFilter('raw-logs', 'pig')}</value>
            </property>
            <property>
              <name>OUT_DB</name>
              <value>${coord:databaseOut('processed-logs')}</value>
            </property>
            <property>
              <name>OUT_TABLE</name>
              <value>${coord:tableOut('processed-logs')}</value>
            </property>
            <property>
              <name>OUT_PARTITIONS</name>
              <value>${coord:dataOutPartitions('processed-logs')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```


Parameterizing the input/output databases and tables using the corresponding EL function as shown will make them
available in the pig action of the workflow 'logsprocessor-wf'.

Each coordinator action will use as input events the last 24 hourly instances of the 'Click-data' dataset.
The `${coord:dataInPartitionFilter(String name, String type)}` function enables the coordinator application to pass the
Partition Filter corresponding to all the dataset instances for the last 24 hours to the workflow job triggered
by the coordinator action. The `${coord:dataOutPartitions(String name)}` function enables the coordinator application
to pass the partition key-value string needed by the **HCatStorer** in Pig job when the workflow is triggered by the
coordinator action.

<a name="HCatWorkflow"></a>

**Workflow definition:**

```
<workflow-app xmlns="uri:oozie:workflow:0.3" name="logsprocessor-wf">
    <credentials>
      <credential name='hcatauth' type='hcat'>
        <property>
          <name>hcat.metastore.uri</name>
          <value>${HCAT_URI}</value>
        <property>
        </property>
          <name>hcat.metastore.principal</name>
          <value>${HCAT_PRINCIPAL}</value>
        <property>
      </credential>
    </credentials>
    <start to="pig-node"/>
    <action name="pig-node" cred="hcatauth">
        <pig>
            <job-tracker>${jobTracker}</job-tracker>
            <name-node>${nameNode}</name-node>
            <prepare>
                <delete path="hcat://foo:11002/${OUT_DB}/${OUT_TABLE}/date=${OUT_PARTITION_VAL_DATE}"/>
            </prepare>
            ...
            <script>id.pig</script>
            <param>HCAT_IN_DB=${IN_DB}</param>
            <param>HCAT_IN_TABLE=${IN_TABLE}</param>
            <param>HCAT_OUT_DB=${OUT_DB}</param>
            <param>HCAT_OUT_TABLE=${OUT_TABLE}</param>
            <param>PARTITION_FILTER=${FILTER}</param>
            <param>OUTPUT_PARTITIONS=${OUT_PARTITIONS}</param>
        <file>lib/hive-site.xml</file>
        </pig>
        <ok to="end"/>
        <error to="fail"/>
    </action>
    <kill name="fail">
        <message>Pig failed, error message[${wf:errorMessage(wf:lastErrorNode())}]</message>
    </kill>
    <end name="end"/>
</workflow-app>
```

**Important**: Ensure that the required hcatalog jars and hive-site.xml are in classpath, with versions corresponding to
hcatalog installation. Refer [HCatalog Libraries](DG_HCatalogIntegration.html#HCatalogLibraries) for the different ways
to place them in the hadoop job classpath.

**Important**: See [Action Authentication](DG_ActionAuthentication.html) for more information about how to access a secure HCatalog from
any workflow action.

**Example usage in Pig:**

```
A = load '$HCAT_IN_DB.$HCAT_IN_TABLE' using org.apache.hive.hcatalog.pig.HCatLoader();
B = FILTER A BY $PARTITION_FILTER;
C = foreach B generate foo, bar;
store C into '$HCAT_OUT_DB.$HCAT_OUT_TABLE' using org.apache.hive.hcatalog.pig.HCatStorer('$OUTPUT_PARTITIONS');
```

For the `2009-01-02T00:00Z` run with the given dataset instances, the above Pig script with resolved values would look
like:

```
A = load 'myInputDatabase.myInputTable' using org.apache.hive.hcatalog.pig.HCatLoader();
B = FILTER A BY ((datestamp==2009010101 AND region==USA) OR
    (datestamp==2009010102 AND region==USA) OR
    ...
    (datestamp==2009010123 AND region==USA) OR
    (datestamp==2009010200 AND region==USA));
C = foreach B generate foo, bar;
store C into 'myOutputDatabase.myOutputTable' using org.apache.hive.hcatalog.pig.HCatStorer('datestamp=20090102,region=EUR');
```

#### 6.8.5 coord:dataInPartitionMin(String name, String partition) EL function

The `${coord:dataInPartitionMin(String name, String partition)}` EL function resolves to the **minimum** value of the
specified partition for all the dataset instances specified in an input event dataset section. It can be used to do
range based filtering of partitions in pig scripts together
with [dataInPartitionMax](CoordinatorFunctionalSpec.html#DataInPartitionMax) EL function.

Refer to the [Example](CoordinatorFunctionalSpec.html#HCatPigExampleTwo) below for usage.

<a name="DataInPartitionMax"></a>
#### 6.8.6 coord:dataInPartitionMax(String name, String partition) EL function

The `${coord:dataInPartitionMax(String name, String partition)}` EL function resolves to the **maximum** value of the
specified partition for all the dataset instances specified in an input event dataset section. It is a better practice
to use `dataInPartitionMin` and `dataInPartitionMax` to form a range filter wherever possible instead
of `datainPartitionPigFilter` as it will be more efficient for filtering.

Refer to the [Example](CoordinatorFunctionalSpec.html#HCatPigExampleTwo) below for usage.

#### 6.8.7 coord:dataOutPartitionValue(String name, String partition) EL function

The `${coord:dataOutPartitionValue(String name, String partition)}` EL function resolves to value of the specified
partition for the output-event dataset; that will be consumed by a workflow job, e.g Pig job triggered by a
coordinator action. This is another convenience function to use a single partition-key's value if required, in
addition to `dataoutPartitionsPig()` and either one can be used.

The example below illustrates a pig job triggered by a coordinator, using the aforementioned EL functions for input
partition max/min values, output partition value, and database and table.

**<font color="#008000"> Example: </font>**

<a name="HCatPigExampleTwo"></a>

**Coordinator application definition:**

```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="Click-data" frequency="${coord:hours(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
             hcat://foo:11002/myInputDatabase/myInputTable/datestamp=${YEAR}${MONTH}${DAY}${HOUR};region=USA
          </uri-template>
        </dataset>
        <dataset name="Stats" frequency="${coord:days(1)}"
                 initial-instance="2009-01-01T01:00Z" timezone="UTC">
          <uri-template>
             hcat://foo:11002/myOutputDatabase/myOutputTable/datestamp=${YEAR}${MONTH}${DAY};region=USA
          </uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="raw-logs" dataset="Click-data">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <output-events>
        <data-out name="processed-logs" dataset="Stats">
          <instance>${coord:current(0)}</instance>
        </data-out>
      </output-events>
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsprocessor-wf</app-path>
          <configuration>
            <property>
              <name>IN_DB</name>
              <value>${coord:databaseIn('raw-logs')}</value>
            </property>
            <property>
              <name>IN_TABLE</name>
              <value>${coord:tableIn('raw-logs')}</value>
            </property>
            <property>
              <name>DATE_MIN</name>
              <value>${coord:dataInPartitionMin('raw-logs','datestamp')}</value>
            </property>
            <property>
              <name>DATE_MAX</name>
              <value>${coord:dataInPartitionMax('raw-logs','datestamp')}</value>
            </property>
            <property>
              <name>OUT_DB</name>
              <value>${coord:databaseOut('processed-logs')}</value>
            </property>
            <property>
              <name>OUT_TABLE</name>
              <value>${coord:tableOut('processed-logs')}</value>
            </property>
            <property>
              <name>OUT_PARTITION_VAL_REGION</name>
              <value>${coord:dataOutPartitionValue('processed-logs','region')}</value>
            </property>
            <property>
              <name>OUT_PARTITION_VAL_DATE</name>
              <value>${coord:dataOutPartitionValue('processed-logs','datestamp')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

In this example, each coordinator action will use as input events the last 24 hourly instances of the 'logs' dataset.

For the `2009-01-02T00:00Z` run, the `${coord:dataInPartitionMin('raw-logs','datestamp')}` function will resolve to
the minimum of the 5 dataset instances for partition 'datestamp'
i.e. among 2009010101, 2009010102, ...., 2009010123,  2009010200, the minimum would be "2009010101".

Similarly, the `${coord:dataInPartitionMax('raw-logs','datestamp')}` function will resolve to the maximum of the 5
dataset instances for partition 'datestamp'
i.e. among 2009010120, 2009010121, ...., 2009010123, 2009010200, the maximum would be "2009010200".

Finally, the `${coord:dataOutPartitionValue(String name, String partition)}` function enables the coordinator
application to pass a specified partition's value string needed by the HCatStorer in Pig job.
The `${coord:dataOutPartitionValue('processed-logs','region')}` function will resolve to: "${region}"
and `${coord:dataOutPartitionValue('processed-logs','datestamp')}` function will resolve to: "20090102".

For the workflow definition with \<pig\> action, refer to [previous example](CoordinatorFunctionalSpec.html#HCatWorkflow),
with the following change in pig params in addition to database and table.

```
...
<param>PARTITION_DATE_MIN=${DATE_MIN}</param>
<param>PARTITION_DATE_MAX=${DATE_MAX}</param>
<param>REGION=${region}</param>
<param>OUT_PARTITION_VAL_REGION=${OUT_PARTITION_VAL_REGION}</param>
<param>OUT_PARTITION_VAL_DATE=${OUT_PARTITION_VAL_DATE}</param>
...
```

**Example usage in Pig:**
This illustrates another pig script which filters partitions based on range, with range limits parameterized with the
EL functions

```
A = load '$HCAT_IN_DB.$HCAT_IN_TABLE' using org.apache.hive.hcatalog.pig.HCatLoader();
B = FILTER A BY datestamp >= '$PARTITION_DATE_MIN' AND datestamp < '$PARTITION_DATE_MAX' AND region=='$REGION';
C = foreach B generate foo, bar;
store C into '$HCAT_OUT_DB.$HCAT_OUT_TABLE' using org.apache.hive.hcatalog.pig.HCatStorer('region=$OUT_PARTITION_VAL_REGION,datestamp=$OUT_PARTITION_VAL_DATE');
```

For example,
for the `2009-01-02T00:00Z` run with the given dataset instances, the above Pig script with resolved values would look like:

```
A = load 'myInputDatabase.myInputTable' using org.apache.hive.hcatalog.pig.HCatLoader();
B = FILTER A BY datestamp >= '2009010101' AND datestamp < '2009010200' AND region='APAC';
C = foreach B generate foo, bar;
store C into 'myOutputDatabase.myOutputTable' using org.apache.hive.hcatalog.pig.HCatStorer('region=APAC,datestamp=20090102');
```

#### 6.8.8 coord:dataInPartitions(String name, String type) EL function

The `${coord:dataInPartitions(String name, String type)}` EL function resolves to a list of partition key-value
pairs for the input-event dataset. Currently the only type supported is 'hive-export'. The 'hive-export' type
supports only one partition instance and it can be used to create the complete partition value string that can
be used in a hive query for partition export/import.

The example below illustrates a hive export-import job triggered by a coordinator, using the EL functions for HCat database,
table, input partitions. The example replicates the hourly processed data across hive tables.

**<font color="#008000"> Example: </font>**

<a name="HCatHiveExampleOne"></a>

**Coordinator application definition:**

```
    <coordinator-app xmlns="uri:oozie:coordinator:0.3" name="app-coord"
    frequency="${coord:hours(1)}" start="2014-03-28T08:00Z"
    end="2030-01-01T00:00Z" timezone="UTC">

    <datasets>
        <dataset name="Stats-1" frequency="${coord:hours(1)}"
            initial-instance="2014-03-28T08:00Z" timezone="UTC">
            <uri-template>hcat://foo:11002/myInputDatabase1/myInputTable1/year=${YEAR};month=${MONTH};day=${DAY};hour=${HOUR}
            </uri-template>
        </dataset>
        <dataset name="Stats-2" frequency="${coord:hours(1)}"
            initial-instance="2014-03-28T08:00Z" timezone="UTC">
            <uri-template>hcat://foo:11002/myInputDatabase2/myInputTable2/year=${YEAR};month=${MONTH};day=${DAY};hour=${HOUR}
            </uri-template>
        </dataset>
    </datasets>
    <input-events>
        <data-in name="processed-logs-1" dataset="Stats-1">
            <instance>${coord:current(0)}</instance>
        </data-in>
    </input-events>
    <output-events>
        <data-out name="processed-logs-2" dataset="Stats-2">
            <instance>${coord:current(0)}</instance>
        </data-out>
    </output-events>
    <action>
      <workflow>
        <app-path>hdfs://bar:8020/usr/joe/logsreplicator-wf</app-path>
        <configuration>
          <property>
            <name>EXPORT_DB</name>
            <value>${coord:databaseIn('processed-logs-1')}</value>
          </property>
          <property>
            <name>EXPORT_TABLE</name>
            <value>${coord:tableIn('processed-logs-1')}</value>
          </property>
          <property>
            <name>IMPORT_DB</name>
            <value>${coord:databaseOut('processed-logs-2')}</value>
          </property>
          <property>
            <name>IMPORT_TABLE</name>
            <value>${coord:tableOut('processed-logs-2')}</value>
          </property>
          <property>
            <name>EXPORT_PARTITION</name>
            <value>${coord:dataInPartitions('processed-logs-1', 'hive-export')}</value>
          </property>
          <property>
            <name>EXPORT_PATH</name>
            <value>hdfs://bar:8020/staging/${coord:formatTime(coord:nominalTime(), 'yyyy-MM-dd-HH')}/data</value>
          </property>
        </configuration>
      </workflow>
    </action>
</coordinator-app>
```

Parameterizing the input/output databases and tables using the corresponding EL function as shown will make them
available in the hive action of the workflow 'logsreplicator-wf'.

Each coordinator action will use as input events the hourly instances of the 'processed-logs-1' dataset. The
`${coord:dataInPartitions(String name, String type)}` function enables the coordinator application to pass the
partition corresponding to hourly dataset instances to the workflow job triggered by the coordinator action.
The workflow passes this partition value to the hive export script that exports the hourly partition from source
database to the staging location referred as `EXPORT_PATH`. The hive import script imports the hourly partition from
`EXPORT_PATH` staging location into the target database.

<a name="HCatWorkflow"></a>

**Workflow definition:**

```
<workflow-app xmlns="uri:oozie:workflow:0.3" name="logsreplicator-wf">
    <start to="table-export"/>
    <action name="table-export">
        <hive:hive xmlns:hive="uri:oozie:hive-action:0.2" xmlns="uri:oozie:hive-action:0.2">
            <job-tracker>${jobTracker}</job-tracker>
            <name-node>${nameNode}</name-node>
            <job-xml>${wf:appPath()}/conf/hive-site.xml</job-xml>
            <configuration>
                <property>
                    <name>mapred.job.queue.name</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>oozie.launcher.mapred.job.priority</name>
                    <value>${jobPriority}</value>
                </property>
            </configuration>
            <script>${wf:appPath()}/scripts/table-export.hql</script>
            <param>sourceDatabase=${EXPORT_DB}</param>
            <param>sourceTable=${EXPORT_TABLE}</param>
            <param>sourcePartition=${EXPORT_PARTITION}</param>
            <param>sourceStagingDir=${EXPORT_PATH}</param>
        </hive:hive>
        <ok to="table-import"/>
        <error to="fail"/>
    </action>
    <action name="table-import">
        <hive:hive xmlns:hive="uri:oozie:hive-action:0.2" xmlns="uri:oozie:hive-action:0.2">
            <job-tracker>${jobTracker}</job-tracker>
            <name-node>${nameNode}</name-node>
            <job-xml>${wf:appPath()}/conf/hive-site.xml</job-xml>
            <configuration>
                <property>
                    <name>mapred.job.queue.name</name>
                    <value>${queueName}</value>
                </property>
                <property>
                    <name>oozie.launcher.mapred.job.priority</name>
                    <value>${jobPriority}</value>
                </property>
            </configuration>
            <script>${wf:appPath()}/scripts/table-import.hql</script>
            <param>targetDatabase=${IMPORT_DB}</param>
            <param>targetTable=${IMPORT_TABLE}</param>
            <param>targetPartition=${EXPORT_PARTITION}</param>
            <param>sourceStagingDir=${EXPORT_PATH}</param>
        </hive:hive>
        <ok to="end"/>
        <error to="fail"/>
    </action>
    <kill name="fail">
        <message>
            Workflow failed, error message[${wf:errorMessage(wf:lastErrorNode())}]
        </message>
    </kill>
    <end name="end"/>
</workflow-app>
```

Ensure that the following jars are in classpath, with versions corresponding to hcatalog installation:
hcatalog-core.jar, webhcat-java-client.jar, hive-common.jar, hive-exec.jar, hive-metastore.jar, hive-serde.jar,
 libfb303.jar. The hive-site.xml needs to be present in classpath as well.

**Example Hive Export script:**
The following script exports a particular Hive table partition into staging location, where the partition value
 is computed through `${coord:dataInPartitions(String name, String type)}` EL function.
```
export table ${sourceDatabase}.${sourceTable} partition (${sourcePartition}) to '${sourceStagingDir}';
```

For example, for the 2014-03-28T08:00Z run with the given dataset instances and ${coord:dataInPartitions(
'processed-logs-1', 'hive-export'), the above Hive script with resolved values would look like:
```
export table myInputDatabase1/myInputTable1 partition (year='2014',month='03',day='28',hour='08') to 'hdfs://bar:8020/staging/2014-03-28-08';
```

**Example Hive Import script:**
The following script imports a particular Hive table partition from staging location, where the partition value is computed
 through `${coord:dataInPartitions(String name, String type)}` EL function.
```
use ${targetDatabase};
alter table ${targetTable} drop if exists partition ${targetPartition};
import table ${targetTable} partition (${targetPartition}) from '${sourceStagingDir}';
```

For example, for the 2014-03-28T08:00Z run with the given dataset instances and ${coord:dataInPartitions(
'processed-logs-2', 'hive-export'), the above Hive script with resolved values would look like:

```
use myInputDatabase2;
alter table myInputTable2 drop if exists partition (year='2014',month='03',day='28',hour='08');
import table myInputTable2 partition (year='2014',month='03',day='28',hour='08') from 'hdfs://bar:8020/staging/2014-03-28-08';
```


### 6.9. Parameterization of Coordinator Application

This section describes the EL functions that could be used to parameterized both data-set and coordination application action.

#### 6.9.1. coord:dateOffset(String baseDate, int instance, String timeUnit) EL Function

The `${coord:dateOffset(String baseDate, int instance, String timeUnit)}` EL function calculates the date based on the following
equation : `newDate = baseDate + (instance * timeUnit)`
In other words, it offsets the `baseDate` by the amount specified by `instance` and `timeUnit`.

The `timeUnit` argument accepts one of 'DAY', 'MONTH', 'HOUR', 'MINUTE', 'MONTH'

For example, if `baseDate` is '2009-01-01T00:00Z', `instance` is '2' and `timeUnit` is 'MONTH', the return date will be
'2009-03-01T00:00Z'. If `baseDate` is '2009-01-01T00:00Z', `instance` is '1' and `timeUnit` is 'YEAR', the return date will be
'2010-01-01T00:00Z'.

**<font color="#008000"> Example: </font>**:



```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T23:00Z" end="2009-12-31T23:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      ......
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsaggretor-wf</app-path>
          <configuration>
            <property>
              <name>nextInstance</name>
              <value>${coord:dateOffset(coord:nominalTime(), 1, 'DAY')}</value>
            </property>
            <property>
             <name>previousInstance</name>
              <value>${coord:dateOffset(coord:nominalTime(), -1, 'DAY')}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

In this example, the 'nextInstance' will be '2009-01-02T23:00Z' for the first action. And the value of 'previousInstance' will be
'2008-12-31T23:00Z' for the same instance.

#### 6.9.2. coord:dateTzOffset(String baseDate, String timezone) EL Function

The `${coord:dateTzOffset(String baseDate, String timezone)}` EL function calculates the date based on the following
equation : `newDate = baseDate + (Oozie processing timezone - timezone)`
In other words, it offsets the `baseDate` by the difference from Oozie processing timezone to the given `timezone`.  It will
account for daylight saving time based on the given `baseDate` and `timezone`.

The `timezone` argument accepts any timezone or GMT offset that is returned by the
["info -timezones"](DG_CommandLineTool.html#Getting_a_list_of_time_zones) command.  For example, "America/Los_Angeles".

For example, if `baseDate` is '2012-06-13T00:00Z' and `timezone` is 'America/Los_Angeles', the return date will be
'2012-06-12T17:00Z'. But if `baseDate` is '2012-12-13T00:00Z', then the return date will be '2012-12-12T16:00Z'.  The difference
in return dates occurs because the former occurs during Summer when DST is in effect (UTC-0700) and the latter occurs during Winter
when DST is not in effect (UTC-0800).

**<font color="#008000"> Example: </font>**:



```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2009-01-01T24:00Z" end="2009-12-31T24:00Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      ......
      <action>
        <workflow>
          <app-path>hdfs://bar:8020/usr/joe/logsaggretor-wf</app-path>
          <configuration>
            <property>
              <name>myDate</name>
              <value>${coord:dateTzOffset(coord:nominalTime(), "America/Los_Angeles")}</value>
            </property>
         </configuration>
       </workflow>
      </action>
   </coordinator-app>
```

In this example, the 'myDate' will be '2009-01-01T15:00Z' for the first action.

#### 6.9.3. coord:formatTime(String ts, String format) EL Function (since Oozie 2.3.2)

The `${coord:formatTime(String timeStamp, String format)}` function allows transformation of the standard ISO8601 timestamp strings into other desired formats.

The format string should be in Java's [SimpleDateFormat](http://download.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html) format.

For example, if timeStamp is '2009-01-01T00:00Z' and format is 'yyyy', the returned date string will be '2009'.

#### 6.9.4. coord:epochTime(String ts, String millis) EL Function (since Oozie 4.3)

The `${coord:epochTime(String timeStamp, String millis)}` function allows transformation of the standard ISO8601 timestamp
strings into Unix epoch time (seconds or milliseconds since January 1, 1970).

If millis is 'false', the returned time string will be the number of seconds since the epoch. If 'true', the returned time string
will be the number of milliseconds since the epoch.

For example, if timeStamp is '2009-01-01T00:00Z' and millis is 'false', the returned date string will be '1230768000'. If millis
is 'true', the returned date string will be '1230768000000'.

### 6.10. Conditional coordinator input logic
By default, all input dependencies are "AND", which means all dependencies has to be available before the action starts running.

With conditional input logic, one should able to specify conditional operations among multiple datasets.

Supported operators are OR, AND, COMBINE. OR and AND operators are nested, one can form multiple nested expressions using them.

   * OR: Logical OR, where an expression will evaluate to true if one of the datasets is available.
   * AND: Logical AND, where an expression will evaluate to true when all of the datasets are available.
   * COMBINE :  With combine, instances of A and B can be interleaved to get the final "combined" set of total instances. All datasets in combine should have the same range defined with the current EL function. Combine does not support latest and future EL functions. Combine cannot also be nested.

Additional options

   * **<font color="#0000ff"> MIN: </font>** Minimum number of input instances that should be available. This can be used in cases where inputs are optional and the processing can be done on a subset of input instances.
   * <font color="#0000ff"> WAIT (in minutes): </font> Wait is used to specify the amount of time to continue checking for availability of instances of a particular dataset before moving on to the next dataset defined in a OR condition. The wait time is calculated from the beginning of the nominal time of the action or the action creation time whichever is later. The main purpose of this is to give preference to the primary datasource before checking the secondary datasource.

Wait when used with min option has a totally different purpose. It is used to specify the additional amount of time to wait and check for more instances after the required minimum set of instances become available. Any additional instances that become available during the wait time are then included.

The conditional logic can be specified using the \<input-logic\> tag in the coordinator.xml using the [Oozie Coordinator Schema 0.5](CoordinatorFunctionalSpec.html#Oozie_Coordinator_Schema_0.5) and above. If not specified, the default behavior of "AND" of all defined input dependencies is applied.

Order of definition of the dataset matters. Availability of inputs is checked in that order. Only if input instances of the first dataset is not available, then the input instances of the second dataset will be checked and so on. In the case of AND or OR, the second dataset is picked only if the first dataset does not meet all the input dependencies first. In the case of COMBINE, only the input instances missing on the first dataset are checked for availability on the other datasets in order and then included.

coord:dataIn() function can be used to get the comma separated list of evaluated hdfs paths given the name of the conditional operator.

**<font color="#008000"> Example: </font>**:

```
<input-logic>
    <or name="AorB">
        <data-in dataset="A"/>
        <data-in dataset="B"/>
    </or>
</input-logic>
```
With above expression one can specify the dataset as AorB. Action will start running as soon dataset A or B is available. Dataset "A" has higher precedence over "B" because it is defined first. Oozie will first check for availability of dataset A and only if A is not available, availability of dataset B will be checked.

**<font color="#008000"> Example: </font>**:

```
<input-logic>
    <or name="AorBorC">
        <data-in dataset="A" wait = "60"/>
        <data-in dataset="B" wait = "90"/>
        <data-in dataset="C"/>
    </or>
</input-logic>
```
With the above expression, it will wait for 60 mins from the nominal time of the action or the action creation time whichever is later for all the instances of dataset A to be available. If it is not available in 60 minutes, then it will start checking for instances of dataset B. If instances of B are not available in another 30 minutes, then it will start checking for dataset C.

**<font color="#008000"> Example: </font>**:

```
<datasets>
       <dataset name="dataset_a" frequency="${coord:minutes(20)}" initial-instance="2010-01-01T00:00Z" timezone="UTC">
            <uri-template>${nameNode}/user/${coord:user()}/${examplesRoot}/input-data/rawLogs/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
       </dataset>
       <dataset name="dataset_b" frequency="${coord:minutes(20)}" initial-instance="2010-01-01T00:00Z" timezone="UTC">
            <uri-template>${nameNode}/user/${coord:user()}/${examplesRoot}/input-data/rawLogs-2/${YEAR}/${MONTH}/${DAY}/${HOUR}/${MINUTE}</uri-template>
       </dataset>
</datasets>
<data-in name="A" dataset="dataset_a">
       <start-instance>${coord:current(-5)}</start-instance>
       <end-instance>${coord:current(-1)}</end-instance>
</data-in>
<data-in name="B" dataset="dataset_b">
       <start-instance>${coord:current(-5)}</start-instance>
       <end-instance>${coord:current(-1)}</end-instance>
</data-in>
<input-logic>
    <or>
        <and name="AorB">
              <data-in dataset="A"/>
              <data-in dataset="B"/>
        </and>
        <and name="CorD">
              <data-in dataset="C"/>
              <data-in dataset="D"/>
        </and>
    </or>
</input-logic>
```
Action will start running as soon as dependency A and B or C and D are available.

**<font color="#008000"> Example: </font>**:

```
<input-logic>
       <combine name="AorB">
            <data-in dataset="A"/>
            <data-in dataset="B"/>
       </combine>
</input-logic>
```
Combine function will first check instances from A and whatever is missing it will check from B.

**<font color="#008000"> Example: </font>**:

```
<input-logic>
    <data-in dataset="A" min=2/>
</input-logic>
```
Action will start running if available dependencies >= 2.

**<font color="#008000"> Example: </font>**:

```
<input-logic>
    <or name="AorB" min=2>
         <data-in dataset="A"/>
         <data-in dataset="B"/>
    </or>
</input-logic>
```
Action will start running if A has available dependencies >= 2 or B has available dependencies >= 2

**<font color="#008000"> Example: </font>**:

```
<input-logic>
    <or name="AorB" min="2">
        <data-in dataset="A" wait="10"/>
        <data-in dataset="B"/>
    </or>
</input-logic>
```
After the mininum two dependencies are available, processing will wait for additional 10 minutes to include any dependencies that become available during that period.


```
<input-logic>
    <or name="AorB" min="5" wait="10">
        <data-in dataset="A"/>
        <data-in dataset="B"/>
    </or>
</input-logic>
```
MIN and WAIT can be used at parent level, which will get propagated to child node. Above expression is equivalent to dataset A with min = 2 and wait = 10 minutes and dataset B with min = 2 and wait = 10 minutes.

**<font color="#008000"> Example: </font>**:

```
<input-logic>
    <or>
         <and name="AorB">
              <data-in dataset="A"/>
              <data-in dataset="B"/>
         </and>
         <and name="CorD">
              <data-in dataset="C"/>
              <data-in dataset="D"/>
         </and>
    </or>
</input-logic>
<action>
        <workflow>
            <app-path>hdfs:///tmp/workflows</app-path>
            <configuration>
            <property>
                    <name>inputCheckDataAorB</name>
                    <value>${coord:dataIn(AorB)}</value>
            </property>
            <property>
                    <name>inputCheckDataCorD</name>
                    <value>${coord:dataIn(CorD)}</value>
            </property>
            </configuration>
        </workflow>
</action>
```
Each nested operation can be named and passed into the workflow using coord:dataIn().


## 7. Handling Timezones and Daylight Saving Time

As mentioned in section #4.1.1 'Timezones and Daylight-Saving', the coordinator engine works exclusively in UTC, and dataset and application definitions are always expressed in UTC.


**<font color="#008000"> Example of nominal times in case of DST change: </font>**

| **Frequency** | **Timezone** | **Nominal times in local time** | **Comments** |
| --- | --- | --- | --- |
| `${coord:months(1)}` or `${10 23 1 1-12 *}` | America/Los_Angeles | 2016-03-01T15:10 <br/> 2016-04-01T15:10 <br/> 2016-05-01T15:10  <br/> ...  <br/> 2016-11-01T15:10 <br/> 2016-12-01T15:10 | <br/>DST Start on March 13, 2:00 am <br/><br/><br/><br/> DST End on November 6, 2:00 am|
| `${coord:month(3)} or `${10 13 1 **/3 **}= | America/Los_Angeles |2016-01-01T05:10 <br/> 2016-04-01T05:10 <br/> 2016-07-01T05:10 <br/> 2016-10-01T05:10 <br/> 2017-01-01T05:10 <br/> 2017-04-01T05:10 <br/> 2017-07-01T05:10 | <br/> DST Start on 2016 March 13, 2:00 am <br/><br/><br/>DST End on 2016 November 6, 2:00 am <br/> DST Start on 2017 March 12, 2:00 am|
| `${coord:days(20)}`| America/Los_Angeles | 2016-03-12T05:10 <br/> 2016-04-01T05:10 <br/> 2016-04-21T05:10 <br/> ... <br/> 2016-11-07T05:10 <br/> 2016-11-27T05:10 |<br/> DST Start on March 13, 2:00 am <br/><br/><br/> DST End on November 6, 2:00 am|
| `${10 13 **/20 ** *}` | America/Los_Angeles | 2016-03-01T05:10 <br/> 2016-03-21T05:10 <br/> 2016-11-01T05:10 <br/> 2016-11-21T05:10 <br/> 2016-12-01T05:10 | <br/> DST Start on March 13, 2:00 am <br/><br/> DST End on November 6, 2:00|
| `${coord:days(1)}` or `${10 23 ** ** *}` | America/Los_Angeles | 2016-03-11T15:10 <br/> 2016-03-12T15:10 <br/> 2016-03-13T15:10 <br/> 2016-03-14T15:10 | <br/> DST Start on March 13, 2:00 am|
| `${coord:hours(24)}`| America/Los_Angeles | 2016-03-11T15:10 <br/> 2016-03-12T15:10 <br/> 2016-03-13T16:10 <br/> 2016-03-14T16:10 | <br/><br/> DST Start on March 13, 2:00 am, but since the time unit is in hours, there will be a shift in local time|
| `${coord:hours(1)}` or `${10 ** ** ** **}` | America/Los_Angeles | 2017-03-12T00:10 <br/> 2017-03-12T01:10 <br/> 2017-03-12T03:10 <br/> 2017-03-12T04:10 | <br/><br/> DST Start on March 12, 2:00 am, so hour 2 will be skipped|
| `${coord:hours(1)}` or `${10 ** ** ** **}` | America/Los_Angeles | 2017-11-05T00:10 <br/> 2017-11-05T01:10 <br/> 2017-11-05T01:10 <br/> 2017-11-05T02:10 <br/> 2017-11-05T03:10 | <br/><br/> DST End on November 5, 2:00 am, so hour 1 will be doubled|
| `${10 **/20 12-14 3 **}` | America/Los_Angeles | 2016-03-12T12:10  <br/> 2016-03-12T16:10 <br/> 2016-03-13T13:10 <br/> 2016-03-13T17:10 <br/> 2016-03-14T13:10 <br/> ... <br/> 2016-11-05T17:10 <br/> 2016-11-06T12:10 <br/> 2016-11-06T16:10 | <br/> <br/> DST Start on March 13, 2:00 am, so after this time the nominal times will be shifted <br/><br/><br/> <br/> DST End on November 6, 2:00 am|
| `${coord:hours(20)}` | America/Los_Angeles |2016-03-12T05:10 <br/> 2016-03-13T01:10 <br/> 2016-03-13T22:10 <br/> 2016-03-14T18:10 <br/> 2016-03-15T14:10 <br/> ... <br/> 2016-11-05T12:10 <br/> 2016-11-06T07:10 <br/> 2016-11-07T03:10 <br/> 2016-11-07T23:10 | <br/><br/> DST Start on March 13, 2:00, so here will be 21 hours in local time between the two materialization times <br/><br/><br/><br/><br/> DST End on November 6, 2:00 am, so here will be a 19 hour difference in local time|
| `${coord:minutes(30)}` or `${**/30 ** ** ** *}` | America/Los_Angeles | 2016-03-13T01:00 <br/> 2016-03-13T01:30 <br/> 2016-03-13T02:00 <br/> 2016-03-13T02:30 <br/> 2016-03-13T04:00 <br/> 2016-03-13T04:30 | <br/><br/><br/> DST Start on March 13, 2:00 am|

**IMPORTANT:** Please note, that in the actual implementation, DST corrections are not applied in case of higher frequencies than one day, so for this frequencies, some shifting in nominal times are expected.

### 7.1. Handling Timezones with No Day Light Saving Time

For timezones that don't observe day light saving time, handling timezones offsets is trivial.

For these timezones, dataset and application definitions, it suffices to express datetimes taking into account the timezone offset.

**<font color="#008000"> Example: </font>**:

Coordinator application definition: A daily coordinator job for India timezone (+05:30) that consumes 24 hourly dataset instances from the previous day starting at the beginning of 2009 for a full year.


```
   <coordinator-app name="app-coord" frequency="${coord:days(1)}"
                    start="2008-12-31T19:30Z" end="2009-12-30T19:30Z" timezone="UTC"
                    xmlns="uri:oozie:coordinator:0.1">
      <datasets>
        <dataset name="hourlyLogs" frequency="${coord:hours(1)}"
                 initial-instance="2008-12-31T19:30Z"  timezone="UTC">
          <uri-template>hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}</uri-template>
        </dataset>
      </datasets>
      <input-events>
        <data-in name="inputLogs" dataset="hourlyLogs">
          <start-instance>${coord:current(-23)}</start-instance>
          <end-instance>${coord:current(0)}</end-instance>
        </data-in>
      </input-events>
      <action>
      ...
      </action>
   </coordinator-app>
```

### 7.2. Handling Timezones with Daylight Saving Time

Oozie Coordinator provides all the necessary functionality to write coordinator applications that work properly when data and processing spans across multiple timezones and different daylight saving rules.

The following 2 use cases will be used to show how Oozie Coordinator built-in functionality can be used to handle such cases:

   1 Process logs hourly data from the last day from US East-coast
   1 Process logs hourly data from the last day from US East-coast and Continental Europe

**1. Process logs hourly data from the last day from US East-coast:**


```
<coordinator-app name="eastcoast-processing" frequency="${coord:days(1)}"
                 start="2009-01-02T05:00Z" end="2010-01-02T05:00Z" timezone="America/New_York"
                 xmlns="uri:oozie:coordinator:0.1">
  <datasets>
    <dataset name="eastlogs" frequency="${coord:hours(1)}"
             initial-instance="2009-01-01T06:00Z" timezone="America/New_York">
      <uri-template>
         hdfs://bar:8020/app/logs/eastcoast/${YEAR}/${MONTH}/${DAY}/${HOUR}
      </uri-template>
    </dataset>
  </datasets>
  <input-events>
    <data-in name="EC" dataset="eastlogs">
      <start-instance>${coord:current( -(coord:hoursInDay(0) - 1) )}</start-instance>
      <end-instance>${coord:current(0)}</end-instance>
    </data-in>
  </input-events>
  <action>
   <workflow>
     <app-path>hdfs://bar:8020/usr/joe/logsaggretor-wf</app-path>
     <configuration>
       <property>
         <name>wfInput</name>
         <value>${coord:dataIn('EC')}</value>
       </property>
    </configuration>
  </workflow>
  </action>
</coordinator-app>
```

Because the `${coord:days(1)}` EL function is used to specify the job frequency, each coordinator action will be materialized (created) at 00:00 EST5EDT regardless of timezone daylight-saving adjustments (05:00 UTC in Winter and 04:00 UTC in Summer)

The `${coord:hoursInDay(-1)}` EL function will resolve to number of hours of the previous day taking into account daylight-saving changes if any. It will resolve to `24` (on regular days), `23` (on spring forward day) or `25` (on fall backward day).

Because of the use of the `${coord:hoursInDay(-1)}` EL function, the dataset instances range resolves [-24 .. -1], [-23 .. -1] or [-25 .. -1]. Thus, they will resolve into the exact number of dataset instances for the day taking daylight-saving adjustments into account.

Note that because the coordinator application and the dataset are in the same timezone, there is no need to do any hour offset corrections in the dataset instances being used as input for each coordinator action.

**2. Process logs hourly data from the last day from US East-coast and the US West-coast:**


```
<coordinator-app name="eastcoast-europe-processing" frequency="${coord:days(1)}"
                 start="2009-01-02T09:00Z" end="2010-01-02T09:00Z" timezone="America/Los_Angeles"
                 xmlns="uri:oozie:coordinator:0.1">
  <datasets>
    <dataset name="eastlogs" frequency="${coord:hours(1)}"
             initial-instance="2009-01-01T06:00Z" timezone="America/New_York">
      <uri-template>
         hdfs://bar:8020/app/logs/eastcoast/${YEAR}/${MONTH}/${DAY}/${HOUR}
      </uri-template>
    </dataset>
    <dataset name="estlogs" frequency="${coord:hours(1)}"
             initial-instance="2009-01-01T09:00Z" timezone="America/Los_Angeles">
      <uri-template>
         hdfs://bar:8020/app/logs/westcoast/${YEAR}/${MONTH}/${DAY}/${HOUR}
      </uri-template>
    </dataset>
  </datasets>
  <input-events>
    <data-in name="EC" dataset="eastlogs">
      <start-instance>${coord:current( -(coord:hoursInDay(0) - 1) -3)}</start-instance>
      <end-instance>${coord:current(-3)}</end-instance>
    </data-in>
    <data-in name="WC" dataset="westlogs">
      <start-instance>$coord:{current(- (coord:hoursInDay(0) - 1) )}</start-instance>
      <end-instance>${coord:current(0)}</end-instance>
    </data-in>
  </input-events>
  <action>
   <workflow>
     <app-path>hdfs://bar:8020/usr/joe/logsaggretor-wf</app-path>
     <configuration>
       <property>
         <name>wfInput</name>
         <value>${coord:dataIn('EC')},${coord:dataIn('WC')}</value>
       </property>
    </configuration>
  </workflow>
  </action>
</coordinator-app>
```

The additional complexity of this use case over the first use case is because the job and the datasets are not all in the same timezone. The corresponding timezone offset has to accounted for.

As the use care requires to process all the daily data for the East coast and the West coast, the processing has to be adjusted to the West coast end of the day because the day there finished 3 hours later and processing will have to wait until then.

The data input range for the East coast dataset must be adjusted (with -3) in order to take the data for the previous EST5EDT day.

**3. Process logs hourly data from the last day from US East-coast and Continental Europe:**


```
<coordinator-app name="eastcoast-europe-processing" frequency="${coord:days(1)}"
                 start="2009-01-02T05:00Z" end="2010-01-02T05:00Z" timezone="America/New_York"
                 xmlns="uri:oozie:coordinator:0.1">
  <datasets>
    <dataset name="eastlogs" frequency="${coord:hours(1)}"
             initial-instance="2009-01-01T06:00Z" timezone="America/New_York">
      <uri-template>
         hdfs://bar:8020/app/logs/eastcoast/${YEAR}/${MONTH}/${DAY}/${HOUR}
      </uri-template>
    </dataset>
    <dataset name="europelogs" frequency="${coord:hours(1)}"
             initial-instance="2009-01-01T01:00Z" timezone="Europe/Berlin">
      <uri-template>
         hdfs://bar:8020/app/logs/europe/${YEAR}/${MONTH}/${DAY}/${HOUR}
      </uri-template>
    </dataset>
  </datasets>
  <input-events>
    <data-in name="EC" dataset="eastlogs">
      <start-instance>${coord:current( -(coord:hoursInDay(0) - 1) )}</start-instance>
      <end-instance>${coord:current(-1)}</end-instance>
    </data-in>
    <data-in name="EU" dataset="eastlogs">
      <start-instance>${coord:current( -(coord:hoursInDay(0) -1) - coord:tzOffset()/60)}</start-instance>
      <end-instance>${coord:current( - coord:tzOffset()/60)}</end-instance>
    </data-in>
  </input-events>
  <action>
   <workflow>
     <app-path>hdfs://bar:8020/usr/joe/logsaggretor-wf</app-path>
     <configuration>
       <property>
         <name>wfInput</name>
         <value>${coord:dataIn('EC')}</value>
       </property>
    </configuration>
  </workflow>
  </action>
</coordinator-app>
```

The additional complexity of this use case over the second use case is because the timezones used for the job and the datasets do not follow the same daylight saving rules (Europe and the US apply the DST changes on different days).

Because of this, the timezone offset between Europe and the US is not constant. To obtain the current timezone offset between the coordinator job and a dataset, the `${coord:tzOffset()}` EL function must be used.

As the use care requires to process all the daily data for the East coast and the continental Europe, the processing happens on East coast time (thus having daily data already available for both Europe and the East coast).

The data input range for the Europe dataset must be adjusted with the `${coord:tzOffset()}` EL function in order to take the data for the previous EST5EDT day.

IMPORTANT: The `${coord:tzOffset()}` function returns the offset in minutes, and the datasets in the example are hourly datasets. Because of this, the offset must be divided by `60` to compute the instance offset.

### 7.3. Timezone and Daylight Saving Tools

The Coordinator engine should provide tools to help developers convert and compute UTC datetimes to timezone datetimes and to daylight saving aware timezones.

## 8. Operational Considerations

### 8.1. Reprocessing
   * TBD

## 9. User Propagation

When submitting a coordinator job, the configuration must contain a `user.name` property. If security is enabled, Oozie must ensure that the value of the `user.name` property in the configuration match the user credentials present in the protocol (web services) request.

When submitting a coordinator job, the configuration may contain the `oozie.job.acl` property (the `group.name` property
has been deprecated). If authorization is enabled, this property is treated as as the ACL for the job, it can contain
user and group IDs separated by commas.

The specified user and ACL are assigned to the created coordinator job.

Oozie must propagate the specified user and ACL to the system executing the actions (workflow jobs).

## 10. Coordinator Application Deployment

Coordinator applications consist exclusively of dataset definitions and coordinator application definitions. They must be installed in an HDFS directory. To submit a job for a coordinator application, the full HDFS path to coordinator application definition must be specified.

### 10.1. Organizing Coordinator Applications

The usage of Oozie Coordinator can be categorized in 3 different segments:

   * *Small:* consisting of a single coordinator application with embedded dataset definitions
   * *Medium:* consisting of a single shared dataset definitions and a few coordinator applications
   * *Large:* consisting of a single or multiple shared dataset definitions and several coordinator applications

Systems that fall in the **medium** and (specially) in the **large** categories are usually referred as data pipeline systems.

Oozie Coordinator definition XML schemas provide a convenient and flexible mechanism for all 3 systems categorization define above.

For **small** systems: All dataset definitions and the coordinator application definition can be defined in a single XML file. The XML definition file is commonly in its own HDFS directory.

For **medium** systems: A single datasets XML file defines all shared/public datasets. Each coordinator application has its own definition file, they may have embedded/private datasets and they may refer, via inclusion, to the shared datasets XML file. All the XML definition files are grouped in a single HDFS directory.

For **large** systems: Multiple datasets XML file define all shared/public datasets. Each coordinator application has its own definition file, they may have embedded/private datasets and they may refer, via inclusion, to multiple shared datasets XML files. XML definition files are logically grouped in different HDFS directories.

NOTE: Oozie Coordinator does not enforce any specific organization, grouping or naming for datasets and coordinator application definition files.

The fact that each coordinator application is in a separate XML definition file simplifies coordinator job submission, monitoring and managing of jobs. Tools to support groups of jobs can be built on of the basic, per job, commands provided by the Oozie coordinator engine.

#### 10.1.1. Dataset Names Collision Resolution

Embedded dataset definitions within a coordinator application cannot have the same name.

Dataset definitions within a dataset definition XML file cannot have the same name.

If a coordinator application includes one or more dataset definition XML files, there cannot be datasets with the same names in the 2 dataset definition XML files.

If any of the dataset name collisions occurs the coordinator job submission must fail.

If a coordinator application includes one or more dataset definition XML files and it has embedded dataset definitions, in case of dataset name collision between the included and the embedded definition files, the embedded dataset takes precedence over the included dataset.

## 11. Coordinator Job Submission

When a coordinator job is submitted to Oozie Coordinator, the submitter must specified all the required job properties plus the HDFS path to the coordinator application definition for the job.

The coordinator application definition HDFS path must be specified in the 'oozie.coord.application.path' job property.

All the coordinator job properties, the HDFS path for the coordinator application, the 'user.name' and 'oozie.job.acl'
must be submitted to the Oozie coordinator engine using an XML configuration file (Hadoop XML configuration file).

**<font color="#008000"> Example: </font>**:


```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property>
        <name>user.name</name>
        <value>joe</value>
    </property>
    <property>
        <name>oozie.coord.application.path</name>
        <value>hdfs://foo:8020/user/joe/myapps/hello-coord.xml</value>
    </property>
    ...
</configuration>
```

## 12. SLA Handling

Oozie 2.0 is integrated with GMS (Grid Monitoring System).

If you add **sla** tags to the Coordinator or Workflow XML files, then the SLA information will be propagated to the GMS system.

### Coordinator SLA Example

```
<coordinator-app name="hello-coord" frequency="${coord:days(1)}"
                 start="2009-01-02T08:01Z" end="2010-01-01T08:01Z"
                 timezone="America/Los_Angeles"
                 xmlns="uri:oozie:coordinator:0.1"
                 xmlns:sla="uri:oozie:sla:0.1">

    <datasets>
        <dataset name="logs" frequency="${1 * HOURS}"
                 initial-instance="2009-01-01T09:00Z"
                 timezone="America/Los_Angeles">
            <uri-template>
                hdfs://bar:8020/app/logs/${YEAR}/${MONTH}/${DAY}/${HOUR}/data
            </uri-template>
        </dataset>
    </datasets>
    <input-events>
        <data-in name="input" dataset="logs">
            <start-instance>${coord:current( -(coord:hoursInDay(0) - 1) )}</start-instance>
            <end-instance>${coord:current(0)}</end-instance>
        </data-in>
    </input-events>
    <action>
        <workflow>
            <app-path>hdfs://bar:8020/usr/joe/hello-wf</app-path>
            <configuration>
                <property>
                    <name>input</name>
                    <value>${coord:dataIn('input')}</value>
                </property>
            </configuration>
        </workflow>
        <sla:info>
            <sla:nominal-time>${coord:nominalTime()}</sla:nominal-time>
            <sla:should-start>${5 * MINUTES}</sla:should-start>
            <sla:should-end>${55 * MINUTES}</sla:should-end>
            <sla:message>log processor run for: ${coord:nominalTime()}</sla:message>
            <sla:alert-contact>joe@example.com</sla:alert-contact>
            <sla:dev-contact>abc@example.com</sla:dev-contact>
            <sla:qa-contact>abc@example.com</sla:qa-contact>
            <sla:se-contact>abc@example.com</sla:se-contact>
            <sla:upstream-apps>application-a,application-b</sla:upstream-apps>
            <sla:alert-percentage>99</sla:alert-percentage>
            <sla:alert-frequency>${24 * LAST_HOUR}</sla:alert-frequency>
        </sla:info>
    </action>
</coordinator-app>
```


### Workflow SLA Example

```
<workflow-app name="hello-wf"
              xmlns="uri:oozie:workflow:0.2"
              xmlns:sla="uri:oozie:sla:0.1">
    <start to="grouper"/>

    <action name="grouper">
        <map-reduce>
            <job-tracker>${jobtracker}</job-tracker>
            <name-node>${namenode}</name-node>
            <configuration>
                <property>
                    <name>mapred.input.dir</name>
                    <value>${input}</value>
                </property>
                <property>
                    <name>mapred.output.dir</name>
                    <value>/usr/foo/${wf:id()}/temp1</value>
                </property>
            </configuration>
        </map-reduce>

        <ok to="end"/>
        <error to="end"/>
    </action>

    <sla:info>
        <sla:nominal-time>${nominal-time}</sla:nominal-time>
        <sla:should-start>${10 * MINUTES}</sla:should-start>
        <sla:should-end>${30 * MINUTES}</sla:should-end>
        <sla:message>abc.grouper for input ${input}</sla:message>
        <sla:alert-contact>joe@example.com</sla:alert-contact>
        <sla:dev-contact>abc@example.com</sla:dev-contact>
        <sla:qa-contact>abc@example.com</sla:qa-contact>
        <sla:se-contact>abc@example.com</sla:se-contact>
        <sla:upstream-apps>application-a,application-b</sla:upstream-apps>
        <sla:alert-percentage>99</sla:alert-percentage>
        <sla:alert-frequency>${24 * LAST_HOUR}</sla:alert-frequency>
    </sla:info>

    <end name="end"/>
</workflow-app>
```
* TBD

## 13. Web Services API
`
See the [Web Services API](WebServicesAPI.html) page.

## 14. Coordinator Rerun
### Rerunning a Coordinator Action or Multiple Actions

Example:


```
$oozie job -rerun <coord_Job_id> [-nocleanup] [-refresh] [-failed]
[-config <arg> (job configuration file '.xml' or '.properties', this file can used to supply properties, which can be used for workflow)]
[-action 1, 3-4, 7-40] (-action or -date is required to rerun.)
[-date 2009-01-01T01:00Z::2009-05-31T23:59Z, 2009-11-10T01:00Z, 2009-12-31T22:00Z]
(if neither -action nor -date is given, the exception will be thrown.)
```

The `rerun` option reruns a terminated (`TIMEDOUT`, `SUCCEEDED`, `KILLED`, `FAILED`) coordinator action when coordinator job
is not in `FAILED` or `KILLED` state.

After the command is executed the rerun coordinator action will be in `WAITING` status.

Refer to the [Rerunning Coordinator Actions](DG_CoordinatorRerun.html) for details on rerun.

<a name="CoordinatorNotifications"></a>
## 15. Coordinator Notifications

Coordinator jobs can be configured to make an HTTP GET notification upon whenever a coordinator action changes its status.

Oozie will make a best effort to deliver the notifications, in case of failure it will retry the notification a
pre-configured number of times at a pre-configured interval before giving up.

See also [Workflow Notifications](WorkflowFunctionalSpec.html#WorkflowNotifications)

### 15.1 Coordinator Action Status Notification

If the `oozie.coord.action.notification.url` property is present in the coordinator job properties when submitting the job,
Oozie will make a notification to the provided URL when any of the coordinator's actions changes its status.
`oozie.coord.action.notification.proxy` property can be used to configure either a http or socks proxy.
The format is proxyHostname:port or proxyType@proxyHostname:port. If proxy type is not specified, it defaults to http.
For eg: myhttpproxyhost.mydomain.com:80 or socks@mysockshost.mydomain.com:1080.

If the URL contains any of the following tokens, they will be replaced with the actual values by Oozie before making
the notification:

   * `$actionId` : The coordinator action ID
   * `$status` : The coordinator action's current status


## Appendixes

### Appendix A, Oozie Coordinator XML-Schema


#### Oozie Coordinator Schema 0.5


```
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:coordinator="uri:oozie:coordinator:0.5"
           elementFormDefault="qualified" targetNamespace="uri:oozie:coordinator:0.5">

    <xs:element name="coordinator-app" type="coordinator:COORDINATOR-APP"/>
    <xs:element name="datasets" type="coordinator:DATASETS"/>
    <xs:simpleType name="IDENTIFIER">
        <xs:restriction base="xs:string">
            <xs:pattern value="([a-zA-Z]([\-_a-zA-Z0-9])*){1,39}"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:complexType name="COORDINATOR-APP">
        <xs:sequence>
            <xs:element name="parameters" type="coordinator:PARAMETERS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="controls" type="coordinator:CONTROLS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="datasets" type="coordinator:DATASETS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-events" type="coordinator:INPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-logic" type="coordinator:INPUTLOGIC" minOccurs="0" maxOccurs="1"/>
            <xs:element name="output-events" type="coordinator:OUTPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="action" type="coordinator:ACTION" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="xs:string" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="start" type="xs:string" use="required"/>
        <xs:attribute name="end" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="PARAMETERS">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="0" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="CONTROLS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="timeout" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="concurrency" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="execution" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="throttle" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATASETS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="include" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:choice minOccurs="0" maxOccurs="unbounded">
                <xs:element name="dataset" type="coordinator:SYNCDATASET" minOccurs="0" maxOccurs="1"/>
                <xs:element name="async-dataset" type="coordinator:ASYNCDATASET" minOccurs="0" maxOccurs="1"/>
            </xs:choice>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="SYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="done-flag" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="initial-instance" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="ASYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="sequence-type" type="xs:string" use="required"/>
        <xs:attribute name="initial-version" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="INPUTEVENTS">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="1"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="1"/>
            <xs:element name="data-in" type="coordinator:DATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:choice>
    </xs:complexType>
    <xs:complexType name="INPUTLOGIC">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="combine" type="coordinator:COMBINE" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:choice>
    </xs:complexType>
    <xs:complexType name="LOGICALAND">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="1" maxOccurs="unbounded"/>
            <xs:element name="combine" type="coordinator:COMBINE" minOccurs="0" maxOccurs="unbounded"/>
        </xs:choice>
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
    </xs:complexType>
    <xs:complexType name="LOGICALOR">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="and" type="coordinator:LOGICALAND" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="or" type="coordinator:LOGICALOR" minOccurs="0" maxOccurs="unbounded"/>
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="1" maxOccurs="unbounded"/>
            <xs:element name="combine" type="coordinator:COMBINE" minOccurs="0" maxOccurs="unbounded"/>
        </xs:choice>
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
    </xs:complexType>
    <xs:complexType name="COMBINE">
        <xs:choice minOccurs="0" maxOccurs="unbounded">
            <xs:element name="data-in" type="coordinator:LOGICALDATAIN" minOccurs="2" maxOccurs="unbounded"/>
        </xs:choice>
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
    </xs:complexType>
    <xs:complexType name="LOGICALDATAIN">
        <xs:attribute name="name" type="xs:string" use="optional"/>
        <xs:attribute name="min" type="xs:string" use="optional"/>
        <xs:attribute name="wait" type="xs:string" use="optional"/>
        <xs:attribute name="dataset" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="DATAIN">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="unbounded"/>
            <xs:sequence minOccurs="1" maxOccurs="1">
                <xs:element name="start-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="end-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:sequence>
        </xs:choice>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="OUTPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-out" type="coordinator:DATAOUT" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAOUT">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="xs:string" use="required"/>
        <xs:attribute name="nocleanup" type="xs:boolean" use="optional"/>
    </xs:complexType>
    <xs:complexType name="ACTION">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="workflow" type="coordinator:WORKFLOW" minOccurs="1" maxOccurs="1"/>
            <xs:any namespace="uri:oozie:sla:0.1 uri:oozie:sla:0.2" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="WORKFLOW">
        <xs:sequence>
            <xs:element name="app-path" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="configuration" type="coordinator:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="FLAG"/>
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
```

#### Oozie Coordinator Schema 0.4


```
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:coordinator="uri:oozie:coordinator:0.2"
           elementFormDefault="qualified" targetNamespace="uri:oozie:coordinator:0.2">

    <xs:element name="coordinator-app" type="coordinator:COORDINATOR-APP"/>
    <xs:element name="datasets" type="coordinator:DATASETS"/>
    <xs:simpleType name="IDENTIFIER">
        <xs:restriction base="xs:string">
            <xs:pattern value="([a-zA-Z]([\-_a-zA-Z0-9])*){1,39})"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:complexType name="COORDINATOR-APP">
        <xs:sequence>
            <xs:element name="parameters" type="coordinator:PARAMETERS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="controls" type="coordinator:CONTROLS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="datasets" type="coordinator:DATASETS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-events" type="coordinator:INPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="output-events" type="coordinator:OUTPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="action" type="coordinator:ACTION" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="start" type="xs:string" use="required"/>
        <xs:attribute name="end" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="PARAMETERS">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="0" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="CONTROLS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="timeout" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="concurrency" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="execution" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="throttle" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATASETS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="include" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:choice minOccurs="0" maxOccurs="unbounded">
                <xs:element name="dataset" type="coordinator:SYNCDATASET" minOccurs="0" maxOccurs="1"/>
            </xs:choice>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="SYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="done-flag" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="initial-instance" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="INPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-in" type="coordinator:DATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAIN">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="unbounded"/>
            <xs:sequence minOccurs="1" maxOccurs="1">
                <xs:element name="start-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="end-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:sequence>
        </xs:choice>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="coordinator:IDENTIFIER" use="required"/>
    </xs:complexType>
    <xs:complexType name="OUTPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-out" type="coordinator:DATAOUT" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAOUT">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="nocleanup" type="xs:boolean" use="optional"/>
    </xs:complexType>
    <xs:complexType name="ACTION">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="workflow" type="coordinator:WORKFLOW" minOccurs="1" maxOccurs="1"/>
            <xs:any namespace="uri:oozie:sla:0.1" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="WORKFLOW">
        <xs:sequence>
            <xs:element name="app-path" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="configuration" type="coordinator:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="FLAG"/>
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
```

#### Oozie Coordinator Schema 0.2


```
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:coordinator="uri:oozie:coordinator:0.2"
           elementFormDefault="qualified" targetNamespace="uri:oozie:coordinator:0.2">

    <xs:element name="coordinator-app" type="coordinator:COORDINATOR-APP"/>
    <xs:element name="datasets" type="coordinator:DATASETS"/>
    <xs:simpleType name="IDENTIFIER">
        <xs:restriction base="xs:string">
            <xs:pattern value="([a-zA-Z]([\-_a-zA-Z0-9])*){1,39})"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:complexType name="COORDINATOR-APP">
        <xs:sequence>
            <xs:element name="controls" type="coordinator:CONTROLS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="datasets" type="coordinator:DATASETS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-events" type="coordinator:INPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="output-events" type="coordinator:OUTPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="action" type="coordinator:ACTION" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="start" type="xs:string" use="required"/>
        <xs:attribute name="end" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="CONTROLS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="timeout" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="concurrency" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="execution" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="throttle" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATASETS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="include" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:choice minOccurs="0" maxOccurs="unbounded">
                <xs:element name="dataset" type="coordinator:SYNCDATASET" minOccurs="0" maxOccurs="1"/>
            </xs:choice>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="SYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="done-flag" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="initial-instance" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="INPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-in" type="coordinator:DATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAIN">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="unbounded"/>
            <xs:sequence minOccurs="1" maxOccurs="1">
                <xs:element name="start-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="end-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:sequence>
        </xs:choice>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="coordinator:IDENTIFIER" use="required"/>
    </xs:complexType>
    <xs:complexType name="OUTPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-out" type="coordinator:DATAOUT" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAOUT">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="coordinator:IDENTIFIER" use="required"/>
    </xs:complexType>
    <xs:complexType name="ACTION">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="workflow" type="coordinator:WORKFLOW" minOccurs="1" maxOccurs="1"/>
            <xs:any namespace="uri:oozie:sla:0.1" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="WORKFLOW">
        <xs:sequence>
            <xs:element name="app-path" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="configuration" type="coordinator:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="FLAG"/>
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
```

#### Oozie Coordinator Schema 0.1

```
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"  xmlns:coordinator="uri:oozie:coordinator:0.1"
    elementFormDefault="qualified" targetNamespace="uri:oozie:coordinator:0.1">

    <xs:element name="coordinator-app" type="coordinator:COORDINATOR-APP"/>
    <xs:element name="datasets" type="coordinator:DATASETS"/>
    <xs:simpleType name="IDENTIFIER">
        <xs:restriction base="xs:string">
            <xs:pattern value="([a-zA-Z]([\-_a-zA-Z0-9])*){1,39})"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:complexType name="COORDINATOR-APP">
        <xs:sequence>
            <xs:element name="controls" type="coordinator:CONTROLS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="datasets" type="coordinator:DATASETS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="input-events" type="coordinator:INPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="output-events" type="coordinator:OUTPUTEVENTS" minOccurs="0" maxOccurs="1"/>
            <xs:element name="action" type="coordinator:ACTION" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="start" type="xs:string" use="required"/>
        <xs:attribute name="end" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="CONTROLS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="timeout" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="concurrency" type="xs:string" minOccurs="0" maxOccurs="1"/>
            <xs:element name="execution" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATASETS">
        <xs:sequence minOccurs="0" maxOccurs="1">
            <xs:element name="include" type="xs:string" minOccurs="0" maxOccurs="unbounded"/>
            <xs:choice minOccurs="0" maxOccurs="unbounded">
                <xs:element name="dataset" type="coordinator:SYNCDATASET" minOccurs="0" maxOccurs="1"/>
            </xs:choice>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="SYNCDATASET">
        <xs:sequence>
            <xs:element name="uri-template" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="done-flag" type="xs:string" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="frequency" type="xs:string" use="required"/>
        <xs:attribute name="initial-instance" type="xs:string" use="required"/>
        <xs:attribute name="timezone" type="xs:string" use="required"/>
    </xs:complexType>
    <xs:complexType name="INPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-in" type="coordinator:DATAIN" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAIN">
        <xs:choice minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="unbounded"/>
            <xs:sequence minOccurs="1" maxOccurs="1">
                <xs:element name="start-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
                <xs:element name="end-instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
            </xs:sequence>
        </xs:choice>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="coordinator:IDENTIFIER" use="required"/>
    </xs:complexType>
    <xs:complexType name="OUTPUTEVENTS">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="data-out" type="coordinator:DATAOUT" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="DATAOUT">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="instance" type="xs:string" minOccurs="1" maxOccurs="1"/>
        </xs:sequence>
        <xs:attribute name="name" type="coordinator:IDENTIFIER" use="required"/>
        <xs:attribute name="dataset" type="coordinator:IDENTIFIER" use="required"/>
    </xs:complexType>
    <xs:complexType name="ACTION">
        <xs:sequence minOccurs="1" maxOccurs="1">
            <xs:element name="workflow" type="coordinator:WORKFLOW" minOccurs="1" maxOccurs="1"/>
            <xs:any namespace="uri:oozie:sla:0.1" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="WORKFLOW">
        <xs:sequence>
            <xs:element name="app-path" type="xs:string" minOccurs="1" maxOccurs="1"/>
            <xs:element name="configuration" type="coordinator:CONFIGURATION" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>

    <xs:complexType name="FLAG"/>
    <xs:complexType name="CONFIGURATION">
        <xs:sequence>
            <xs:element name="property" minOccurs="1" maxOccurs="unbounded">
                <xs:complexType>
                    <xs:sequence>
                        <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:string"/>
                        <xs:element name="description" minOccurs="0" maxOccurs="1" type="xs:string"/>
                    </xs:sequence>
                </xs:complexType>
            </xs:element>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
```


#### Oozie SLA Schemas

##### Oozie SLA Version 0.2
   * Supported in Oozie coordinator schema version 0.4


```
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:sla="uri:oozie:sla:0.2" elementFormDefault="qualified"
           targetNamespace="uri:oozie:sla:0.2">

    <xs:element name="info" type="sla:SLA-INFO"/>

    <xs:complexType name="SLA-INFO">
        <xs:sequence>
            <xs:element name="nominal-time" type="xs:string" minOccurs="1"
                        maxOccurs="1"/>
            <xs:element name="should-start" type="xs:string" minOccurs="0"
                        maxOccurs="1"/>
            <xs:element name="should-end" type="xs:string" minOccurs="1"
                        maxOccurs="1"/>
            <xs:element name="max-duration" type="xs:string" minOccurs="0"
                        maxOccurs="1"/>

            <xs:element name="alert-events" type="xs:string" minOccurs="0"
                        maxOccurs="1"/>
            <xs:element name="alert-contact" type="xs:string" minOccurs="0"
                        maxOccurs="1"/>
            <xs:element name="notification-msg" type="xs:string" minOccurs="0"
                        maxOccurs="1"/>
            <xs:element name="upstream-apps" type="xs:string" minOccurs="0"
                        maxOccurs="1"/>
        </xs:sequence>
    </xs:complexType>

</xs:schema>
```

##### Oozie SLA Version 0.1


```
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:sla="uri:oozie:sla:0.1" elementFormDefault="qualified"
	targetNamespace="uri:oozie:sla:0.1">

        <xs:element name="info" type="sla:SLA-INFO" />

	<xs:complexType name="SLA-INFO">
		<xs:sequence>
			<xs:element name="app-name" type="xs:string" minOccurs="1"
				maxOccurs="1" />

			<xs:element name="nominal-time" type="xs:string"
				minOccurs="1" maxOccurs="1" />
			<xs:element name="should-start" type="xs:string"
				minOccurs="1" maxOccurs="1" />
			<xs:element name="should-end" type="xs:string" minOccurs="1"
				maxOccurs="1" />

			<xs:element name="parent-client-id" type="xs:string"
				minOccurs="0" maxOccurs="1" />
			<xs:element name="parent-sla-id" type="xs:string"
				minOccurs="0" maxOccurs="1" />

			<xs:element name="notification-msg" type="xs:string"
				minOccurs="0" maxOccurs="1" />
			<xs:element name="alert-contact" type="xs:string"
				minOccurs="1" maxOccurs="1" />
			<xs:element name="dev-contact" type="xs:string" minOccurs="1"
				maxOccurs="1" />
			<xs:element name="qa-contact" type="xs:string" minOccurs="1"
				maxOccurs="1" />
			<xs:element name="se-contact" type="xs:string" minOccurs="1"
				maxOccurs="1" />
			<xs:element name="alert-frequency" type="sla:alert-frequencyType"
				minOccurs="0" maxOccurs="1" />
			<xs:element name="alert-percentage" type="sla:alert-percentageType"
				minOccurs="0" maxOccurs="1" />

			<xs:element name="upstream-apps" type="xs:string"
				minOccurs="0" maxOccurs="1" />

		</xs:sequence>
	</xs:complexType>
    <xs:simpleType name="alert-percentageType">
         <xs:restriction base="xs:integer">
              <xs:minInclusive value="0"/>
              <xs:maxInclusive value="100"/>
         </xs:restriction>
    </xs:simpleType>

    <xs:simpleType name="alert-frequencyType">
        <xs:restriction base="xs:string">
            <xs:enumeration value="NONE"></xs:enumeration>
            <xs:enumeration value="LAST_HOUR"></xs:enumeration>
            <xs:enumeration value="LAST_DAY"></xs:enumeration>
            <xs:enumeration value="LAST_MONTH"></xs:enumeration>
        </xs:restriction>
    </xs:simpleType>
</xs:schema>
```


[::Go back to Oozie Documentation Index::](index.html)


