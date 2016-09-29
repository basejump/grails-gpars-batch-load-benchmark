package gpbench

import grails.transaction.Transactional
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.hibernate.SessionFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.init.ScriptUtils

import java.sql.Connection

import static groovyx.gpars.GParsPool.withPool

class LoaderService {
	static transactional = false 
	
	SessionFactory sessionFactory

	def persistenceInterceptor

	def dataSource
	JdbcTemplate jdbcTemplate

	RegionService regionService
	CityService cityService
	CountryService countryService

	def batchSize = 100 //this should match the hibernate.jdbc.batch_size in datasources

	void runBenchMark() {
		println "Running benchmark"


		loadAllFiles('load_GPars_batched_transactions_per_thread_databinding')
		loadAllFiles('load_GPars_SimpleJdbcInsert')
		loadAllFiles('load_GPars_single_rec_per_thread_transaction')
		loadAllFiles('load_commit_each_save')
		loadAllFiles('load_SimpleJdbcInsert')
		loadAllFiles('load_batched_transactions')
		loadAllFiles('load_batched_transactions_databinding')
		
		loadAllFiles('load_from_table')
		loadAllFiles('load_from_table_gparse')

		/*
		def r = benchmark {
			//'GPars_batched_transactions_per_threadns_databinding' { loadAllFiles('load_GPars_batched_transactions_per_threadns_databinding') }
			//'SimpleJdbcInsert' { loadAllFiles('load_SimpleJdbcInsert')}
			//'batched_transactions' { loadAllFiles('load_batched_transactions')}
			//'batched_transactions_databinding' { loadAllFiles('load_batched_transactions_databinding')}
			//'GPars_SimpleJdbcInsert' { loadAllFiles('load_GPars_SimpleJdbcInsert')}
			//'GPars_batched_transactions_per_threadns_databinding' { loadAllFiles('load_GPars_batched_transactions_per_threadns_databinding')}
		}

		r.prettyPrint()
		*/

	}

	def loadAllFiles(String methodName) {
		//sessionFactory.getStatistics().setStatisticsEnabled(true)

		try {
			Long startTime = logBenchStart(methodName)

			"${methodName}"("Country")
			"${methodName}"("Region")
			"${methodName}"("City", "City100k")

			logBenchEnd(methodName, startTime)

		}catch (Exception e) {
			e.printStackTrace()
		} finally {
			truncateTables()
		}

	}

	def getService(String name) {
		name = name.substring(0,1).toLowerCase() + name.substring(1);
		return this."${name}Service"
	}

	@Transactional
	def load_single_transaction(name, csvFile = null) {
		def reader = new File("resources/${csvFile ?: name}.csv").toCsvMapReader()

		def service = getService(name)

		reader.eachWithIndex { Map row, index ->
			service.insertWithoutDataBinding(row)
			if (index % batchSize == 0) cleanUpGorm()
		}
	}
	
	def load_commit_each_save(name, csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()

		def service = getService(name)
		reader.eachWithIndex { Map row, index ->
			service.insertWithoutDataBinding(row)
			if (index % batchSize == 0) cleanUpGorm()
		}
	}
	
	def load_batched_transactions(name, csvFile=null) {
		def batchedList = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		def service = getService(name)

		batchedList.each {List rowSet ->
			City.withTransaction {
				rowSet.each{ Map row->
					service.insertWithoutDataBinding(row)
				}
			}
			cleanUpGorm()
		}
	}

	def load_batched_transactions_databinding(name, csvFile = null) {
		def batchedList = new File("resources/${csvFile ?: name}.csv").toCsvMapReader([batchSize: batchSize])
		def service = getService(name)

		batchedList.each { rowSet ->
			City.withTransaction {
				rowSet.each { row ->
					service.insertWithDataBinding(row)
				}
			}

			cleanUpGorm()
		}
	}
		
	def load_GPars_single_rec_per_thread_transaction(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		def service = getService(name)
		
		withPool(4){
			reader.eachParallel { Map row ->
				withPersistence {
					service.insertWithoutDataBinding(row)
				}
			}
		}
	}
	
	def load_GPars_batched_transactions_per_thread(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		def service = getService(name)
		
		withPool(4){
			reader.eachParallel { List batchList ->
				City.withTransaction {
					batchList.each{ Map row ->
						service.insertWithoutDataBinding(row)
					}
					cleanUpGorm()
				}
			}
		}

	}
	
	def load_GPars_batched_transactions_per_thread_databinding(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		def service = getService(name)
		
		withPool(4){
			reader.eachParallel {List batchList ->
				City.withTransaction {
					batchList.each{ Map row ->
						service.insertWithDataBinding(row)
					}
					cleanUpGorm()
				}
			}
		}
	}


	def load_SimpleJdbcInsert(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		def service = getService(name)
		reader.eachWithIndex { Map row, index ->
			service.insertWithJdbc(row)
		}
	}

	def load_from_table(name, csvFile=null) {
		Sql sql = new Sql(dataSource)
		def service = getService(name)
		List rows = sql.rows("select * from " + "${name}_test".toLowerCase())

		rows.eachWithIndex{ GroovyRowResult row, int index ->
			service.insertWithSetter(row)
			if (index % batchSize == 0) cleanUpGorm()
		}
	}

	def load_from_table_gparse(name, csvFile=null) {
		Sql sql = new Sql(dataSource)
		def service = getService(name)
		List rows = sql.rows("select * from " + "${name}_test".toLowerCase())

		withPool(4){
			rows.eachWithIndexParallel {def row, int index ->
				withPersistence {
					service.insertWithSetter(row)
					if (index % batchSize == 0) cleanUpGorm()
				}
			}
		}

	}

	def insertToTestTable(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		def service = getService(name)
		reader.eachWithIndex { Map row, index ->
			service.insetToTestTable(row)
		}
	}


	def load_GPars_SimpleJdbcInsert(name, csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		def service = getService(name)

		withPool(4){
			reader.eachParallel { List batchList ->
				withPersistence {
					batchList.each { Map row ->
						service.insertWithJdbc(row)
					}
				}
			}
		}

	}

	//in order to test importing from other table.,
	// we need a table from which we can load data and insert to domains.
	//this method created the test talbes and loads seed data in.
	void loadTestDataForGsqlTest() {
		try {
			Resource resource = new FileSystemResource("resources/test-tables.sql");
			Connection connection = sessionFactory.currentSession.connection()
			ScriptUtils.executeSqlScript(connection, resource)

			insertToTestTable("Country")
			insertToTestTable("Region")
			insertToTestTable("City", "City100k")

		}catch (Exception e) {
			//e.printStackTrace()
			//nothing, probaly tables already exist
		}


	}
	
	def cleanUpGorm() {
		def session = sessionFactory.currentSession
		session.flush()
		session.clear()
		def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP
		propertyInstanceMap.get().clear()
	}

	def truncateTables() {
		//println "Truncating tables"
		jdbcTemplate.update("DELETE FROM origin")
		jdbcTemplate.update("DELETE FROM city")
		jdbcTemplate.update("DELETE FROM region")
		jdbcTemplate.update("DELETE FROM country")
		jdbcTemplate.update("RESET QUERY CACHE") //reset mysql query cache to try and be fair
		//println "Truncating tables complete"

		//do the following if using HSQLDB
/*		City.executeUpdate("delete from City")
		Region.executeUpdate("delete from Region")
		Country.executeUpdate("delete from Country")
		def session = sessionFactory.currentSession
		session.flush()*/
	}

	//creates an array list of lists with max size of batchSize. 
	//If I pass in a batch size of 3 it will convert [1,2,3,4,5,6,7,8] into [[1,2,3],[4,5,6],[7,8]]
	//see http://stackoverflow.com/questions/2924395/groovy-built-in-to-split-an-array-into-equal-sized-subarrays
	//and http://stackoverflow.com/questions/3147537/split-collection-into-sub-collections-in-groovy
	def batchChunks(theList, batchSize) {
		if (!theList) return [] //return and empty list if its already empty
		
		def batchedList = []
		int chunkCount = theList.size() / batchSize

		chunkCount.times { chunkNum ->
			def start = chunkNum * batchSize 
			def end = start + batchSize - 1
			batchedList << theList[start..end]    
		}

		if (theList.size() % batchSize){
			batchedList << theList[chunkCount * batchSize..-1]
		}
		return batchedList    
	}
	
	def logBenchStart(desc) {	
		def msg = "***** Starting $desc"
		log.info(msg)
		println msg
		return new Long(System.currentTimeMillis())
	}
	
	def logBenchEnd(desc,startTime){
		def elapsed = (System.currentTimeMillis() - startTime)/1000
		def msg = "***** Finshed $desc in $elapsed seconds"
		log.info(msg)
		println msg
	}

	void withPersistence(closure){
		persistenceInterceptor.init()
		try {
			closure()
		}
		finally {
			persistenceInterceptor.flush()
			persistenceInterceptor.destroy()
		}
	}

}
