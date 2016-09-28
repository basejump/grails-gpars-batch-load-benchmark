package gpbench

import grails.transaction.Transactional
import org.springframework.jdbc.core.JdbcTemplate

import static groovyx.gpars.GParsPool.withPool

class LoaderService {
	static transactional = false 
	
	//inject beans
	def dataSource
	def csvService
	def sessionFactory
	SaverService saverService
	SaveWithBindDataService saveWithBindDataService
	SaveWithSimpleJdbcService saveWithSimpleJdbcService

	def persistenceInterceptor

	JdbcTemplate jdbcTemplate

	def batchSize = 100 //this should match the hibernate.jdbc.batch_size in datasources

	void runBenchMark() {
		println "Running benchmark"

		loadAllFiles('load_GPars_batched_transactions_per_threadns_databinding')
		loadAllFiles('load_SimpleJdbcInsert')
		loadAllFiles('load_GPars_SimpleJdbcInsert')
		loadAllFiles('load_batched_transactions')
		loadAllFiles('load_batched_transactions_databinding')
		loadAllFiles('load_GPars_single_rec_per_thread_transaction')
		loadAllFiles('load_commit_each_save')

		/*
		def r = benchmark {
			//'GPars_batched_transactions_per_threadns_databinding' { loadAllFiles('load_GPars_batched_transactions_per_threadns_databinding') }
			'SimpleJdbcInsert' { loadAllFiles('load_SimpleJdbcInsert')}
			'batched_transactions' { loadAllFiles('load_batched_transactions')}
			'batched_transactions_databinding' { loadAllFiles('load_batched_transactions_databinding')}
			'GPars_SimpleJdbcInsert' { loadAllFiles('load_GPars_SimpleJdbcInsert')}
			'GPars_batched_transactions_per_threadns_databinding' { loadAllFiles('load_GPars_batched_transactions_per_threadns_databinding')}
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
	
	def load_single_transaction(name,csvFile=null) {	
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		City.withTransaction{
			reader.eachWithIndex { Map row, index ->
				saverService."save$name"(row)
				cleanUpGorm()
				if (index % batchSize == 0) cleanUpGorm()
			}
		}
	}
	
	def load_commit_each_save(name,csvFile=null) {	
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		reader.eachWithIndex { Map row, index ->
			saverService."save$name"(row)
			cleanUpGorm()
			if (index % batchSize == 0) cleanUpGorm()
		}
	}
	
	def load_batched_transactions(name,csvFile=null) {
		def batchedList = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		batchedList.each { rowSet ->
			City.withTransaction{
				rowSet.each{row->
					saverService."save$name"(row)
				}
				cleanUpGorm()
			} //end transaction
		}
	}

	def load_batched_transactions_databinding(name, csvFile = null) {
		def batchedList = new File("resources/${csvFile ?: name}.csv").toCsvMapReader([batchSize: batchSize])
		batchedList.each { rowSet ->
			City.withTransaction {
				rowSet.each { row ->
					saveWithBindDataService."save$name"(row)
				}
			} //end transaction
			cleanUpGorm()
		}
	}
		
	def load_GPars_single_rec_per_thread_transaction(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		
		withPool(4){
			reader.eachParallel { map ->
				saverService."save$name"(map)
			}
		}
	}
	
	def load_GPars_batched_transactions_per_thread(name,csvFile=null) {
		println "processing $name"
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		
		withPool(4){
			reader.eachParallel { batchList ->
				City.withTransaction{
					batchList.each{ map ->
						saverService."save$name"(map)
					}
					cleanUpGorm()
				} //end transaction
			}
		}
		
		//int queryCacheHitCount  = sessionFactory.getStatistics().getQueryCacheHitCount();
		//int queryCacheMissCount = sessionFactory.getStatistics().getQueryCacheMissCount();
		//println "Querycache hit $queryCacheHitCount and missed $queryCacheMissCount"
		
		//int new2MissCount = sessionFactory.getStatistics().getSecondLevelCacheStatistics("org.hibernate.cache.StandardQueryCache").getMissCount();
		//int new2HitCount = sessionFactory.getStatistics().getSecondLevelCacheStatistics("org.hibernate.cache.StandardQueryCache").getHitCount();
		//println "Second levelCache hit $new2HitCount and missed $new2MissCount"
	}
	
	def load_GPars_batched_transactions_per_threadns_databinding(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		
		withPool(4){
			reader.eachParallel { batchList ->
				City.withTransaction {
					batchList.each{ map ->
						saveWithBindDataService."save$name"(map)
					}
					cleanUpGorm()
				} //end transaction
			}
		}
	}


	def load_SimpleJdbcInsert(name,csvFile=null) {	
		saveWithSimpleJdbcService.load_SimpleJdbcInsert(name,csvFile)
	}
	
	def load_GPars_SimpleJdbcInsert(name,csvFile=null) {			
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		withPool(4){
			reader.eachParallel { batchList ->
				saveWithSimpleJdbcService.loadList(name,batchList)
			}
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
