package gpbench

import groovyx.gpars.GParsPool;
import static groovyx.gpars.GParsPool.withPool
import org.springframework.transaction.annotation.*
import groovy.sql.Sql
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.grails.plugins.csv.CSVMapReader

class LoaderService {
	static transactional = false 
	
	//inject beans
	def dataSource
	def csvService
	def sessionFactory
	def saverService
	def saveUsingPropsService
	def saveWithSimpleJdbcService

	def batchSize = 50 //this should match the hibernate.jdbc.batch_size in datasources
	
	def loadAllFiles(String methodName) {	
		def startTime = logBenchStart(methodName)
		
		"${methodName}"("Country")
		"${methodName}"("Region")
		"${methodName}"("City","City100k")
		
		logBenchEnd(methodName,startTime)
		
		truncateTables()
		
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
	
	def load_batched_transactions_databinding(name,csvFile=null) {
		def batchedList = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		batchedList.each { rowSet ->
			City.withTransaction{
				rowSet.each{row->
					saveUsingPropsService."save$name"(row)
				}
				cleanUpGorm()
			} //end transaction
		}
	}
		
	def load_GPars_single_rec_per_thread_transaction(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader()
		
		withPool(8){
			reader.eachParallel { map ->
				saverService."save$name"(map)
			}
		}
	}
	
	def load_GPars_batched_transactions_per_thread(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		
		withPool(8){
			reader.eachParallel { batchList ->
				City.withTransaction{
					batchList.each{ map ->
						saverService."save$name"(map)
					}
					cleanUpGorm()
				} //end transaction
			}
		}
	}
	
	def load_GPars_batched_transactions_per_threadns_databinding(name,csvFile=null) {
		def reader = new File("resources/${csvFile?:name}.csv").toCsvMapReader([batchSize:batchSize])
		
		withPool(8){
			reader.eachParallel { batchList ->
				City.withTransaction{
					batchList.each{ map ->
						saveUsingPropsService."save$name"(map)
					}
					cleanUpGorm()
				} //end transaction
			}
		}
	}

	//@Transactional
	def load_SimpleJdbcInsert(name,csvFile=null) {	
		def rows = csvService.importFromCsv(new File("resources/${csvFile?:name}.csv"))
		
		def sji = new SimpleJdbcInsert(dataSource)
		sji.withTableName(name.toLowerCase())
		rows.each{ row ->
			saveWithSimpleJdbcService."save$name"(row,sji)
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
		Sql sql = new Sql(dataSource)
      	sql.execute("truncate table city")
		sql.execute("truncate table region")
		sql.execute("truncate table country")
		sql.execute("RESET QUERY CACHE") //reset mysql query cache to try and be fair

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

	
}
