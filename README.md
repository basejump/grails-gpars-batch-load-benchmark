
GORM : batch importing large datasets and a performance benchmarking app
===================================	 

Summary
--------

We have a new client that will be importing large payment data files into out application and it appears that this topic ([Zach][]) has some common mind share right now. Over the years I have seem many recommendations for hibernate performance tuning with large sets of data and most recently for GORM as well. What I haven't seen are any sample straight forward benchmark apps to fiddle with.
So I jammed the beginnings of one together and here it is. 

[Zach](http://grails.1312388.n4.nabble.com/Grails-Hang-with-Bulk-Data-Import-Using-GPars-td3410441.html)
posted a link to a project he was having problems with using [GPars][] to do a bulk data import with GPars and I ran with that as a good base.

GPars
-------

The GPars is the Groovy way to do parallel processing so you can take advantage of all those cores for longer running areas of your code. 
Its theoretically perfect for splitting up batch process to take advantage of validation and processing in your cores will keeping the statments 
firing into your database.

Key conclusions
-------

The 4 key factors I have discovered so far to really speed things up are..

1. Use GPars so you are not firing on the proverbial 1 cylinder. 
2. follow the concepts in the hibernate suggestions here in http://docs.jboss.org/hibernate/core/3.3/reference/en/html/batch.html for chaps 13.1 and 13.2 and set your jdbc.batch_size then go to Ted's article here http://naleid.com/blog/2009/10/01/batch-import-performance-with-grails-and-mysql/
3. use small transaction batches and keep them the same size as the jdbc.batch_size. DO NOT (auto)commit on every insert
4. Don't use GORM data binding if you can avoid it.
  * DON"T do this -> new SomeGormClass(yourPropSettings) or someGormInstance.properties = [name:'jim',color:'red'] or use bindData()
  * DO explicitly set the values on the fields or your gorm object -> someGormInstance.name = 'jim' ...etc


My Bench Mark Results and details
-------
LoaderService has the different benchmarks to run and its called form Bootstrap using when I do run-war

* 110k+ CSV records on a macbook pro 2.8 dual core. 1024mb ram was given to the VM and these were run using run-war
* I'm using MySql as the DB and its installed on my mac too so GPars can't really get all the cores
* all of these have jdbc.batch_size = 50 and use the principals from #2 above and flush/clear every 50 rows
* The test where the batch insert happen in a single transaction can't be tested with GPars since a single transaction can't span multiple threads
* Databinding is NOT used on the first 3 tests,except for the last test. You can see that using Databinding pretty much doubles the time for both the normal way and using GPars
* the winner seems to be gpars and batched (smaller chunks) transactions

|                            | Normal  | with GPars|
|----------------------------|---------|------------
| Single transaction         | 91 s    | Not applicable			
| | |
| Commit Tran (auto) each record  | 298 s   | 98 s
|  |  |
| Batched Transactions       | 92 s    | **49 s**
| Commit every 50 records |  |
|  |  |
| Batch Tran every 50        | 190 s    | 94 s
| Using Databinding	|  |


TODOs
--------
* My hunch is that GPars difference will be even more significant on a quad with HT. Could use some help here
* 

More background and reading
---------------

Here are a 2 links you should read that will give you some background information on processing large bulk data batches.
read up through 13.2
<http://docs.jboss.org/hibernate/core/3.3/reference/en/html/batch.html>
and read this entire post
<http://naleid.com/blog/2009/10/01/batch-import-performance-with-grails-and-mysql/>

Thomas Lin setup a test for some processing for GPars
<http://fbflex.wordpress.com/2010/06/11/writing-batch-import-scripts-with-grails-gsql-and-gpars/>

and the gpars docs
<http://gpars.org/guide/index.html>

[GPars]: http://gpars.org/guide/index.html
[SimpleJdbc Example]: http://www.brucephillips.name/blog/index.cfm/2010/10/28/Example-Of-Using-Spring-JDBC-Execute-Batch-To-Insert-Multiple-Rows-Into-A-Database-Table
[Zach]:http://grails.1312388.n4.nabble.com/Grails-Hang-with-Bulk-Data-Import-Using-GPars-td3410441.html
