package gpbench

import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.SessionFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletRequest

import javax.servlet.http.HttpServletRequest

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

	def batchSize = 50 //this should match the hibernate.jdbc.batch_size in datasources

	void runBenchMark() {
		println "Running benchmark"

		runImport('GPars_batched_transactions_per_thread', true, true, true) //batched - databinding, typeless map
		runImport('GPars_batched_transactions_per_thread', true, true, true) //batched - databinding, typeless map


		println "############"
		runImport('GPars_batched_transactions_per_thread', true, true, true) //batched - databinding, typeless map
		runImport('GPars_batched_transactions_per_thread', false, true, true) //batched - databinding, typed map
		runImport('GPars_batched_transactions_per_thread', false, false, true) //batched - without databinding, typed map

		runImport('GPars_single_rec_per_thread_transaction', true, true) //databinding, typeless map
		runImport('GPars_single_rec_per_thread_transaction', false, true) //databinding, typed map
		runImport('GPars_single_rec_per_thread_transaction', false, false) //without databinding, typed map

		runImport('single_transaction', true, true) //databinding, typeless map
		runImport('single_transaction', false, true) //databinding, typed map
		runImport('single_transaction', false, false) //without databinding, typed map

		runImport('commit_each_save', true, true) //databinding, typeless map
		runImport('commit_each_save', false, true)  //databinding, typed map
		runImport('commit_each_save', false, false) //without databinding, typed map

		runImport('batched_transactions', true, true, true) //batched - databinding, typeless map
		runImport('batched_transactions', false, true, true) //batched - databinding, typed map
		runImport('batched_transactions', false, false, true) //batched - without databinding, typed map


	}

	def runImport(String method, boolean csv, boolean databinding, boolean batched = false) {
		String extension = csv ? 'csv' : 'json'

		List countries = loadRecordsFromFile("Country.${extension}")
		List regions = loadRecordsFromFile("Region.${extension}")
		List cities = loadRecordsFromFile("City.${extension}")

		if(batched) {
			countries = batchChunks(countries, batchSize)
			regions = batchChunks(regions, batchSize)
			cities = batchChunks(cities, batchSize)
		}

		String desc = method + ':' + (databinding ? "with-databinding" : 'without-databinding')
		desc = desc +  ':' + (csv ? 'typeless-map' : 'typed-map')

		try {
			Long startTime = logBenchStart(desc)

			"${method}"("Country", countries, databinding)
			"${method}"("Region", regions, databinding)
			"${method}"("City", cities, databinding)

			logBenchEnd(desc, startTime)
			println '---------------------'

		} catch (Exception e) {
			e.printStackTrace()
		} finally {
			truncateTables()
		}

	}


	@Transactional
	void single_transaction(String name, List<Map> rows, boolean useDataBinding) {
		def service = getService(name)
		rows.eachWithIndex { Map row, index ->
			insertRecord(service, row, useDataBinding)
			if (index % batchSize == 0) cleanUpGorm()
		}
	}

	void commit_each_save(String name, List<Map> rows, boolean dataBinding) {
		def service = getService(name)
		rows.eachWithIndex { Map row, index ->
			insertRecord(service, row, dataBinding)
			if (index % batchSize == 0) cleanUpGorm()
		}
	}


	void batched_transactions(String name, List<List<Map>> rows, boolean useDataBinding) {
		def service = getService(name)

		rows.each { List rowSet ->
			City.withTransaction {
				rowSet.each{ Map row ->
					insertRecord(service, row, useDataBinding)
				}
			}
			cleanUpGorm()
		}
	}

	def GPars_single_rec_per_thread_transaction(String name, List<Map> rows, boolean useDataBinding) {
		def service = getService(name)
		
		withPool(4){
			rows.eachWithIndexParallel { Map row, int index ->
				withPersistence {
					insertRecord(service, row, useDataBinding)
					if (index % batchSize == 0) cleanUpGorm()
				}

			}
		}
	}
	
	def GPars_batched_transactions_per_thread(String name, List<List<Map>> rows, boolean useDataBinding) {
		def service = getService(name)
		
		withPool(4){
			rows.eachParallel { List batchList ->
				City.withTransaction {
					batchList.each{ Map row ->
						insertRecord(service, row, useDataBinding)
					}
					cleanUpGorm()
				}

			}
		}

	}

	List<Map> loadRecordsFromFile(String fileName) {
		List<Map> result = []
		File file = new File("resources/${fileName}")
		if(fileName.endsWith("csv")) {
			def reader = file.toCsvMapReader()
			reader.each { Map m ->
				//need to convert to grails parameter map, so that it can be binded. because csv is all string:string
				m = toGrailsParamsMap(m)
				result.add(m)
			}
		}
		 else {
			String line
			file.withReader { Reader reader ->
				while (line = reader.readLine()) {
					JSONObject json = JSON.parse(line)
					result.add json
				}
			}
		}

		return result
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

	def getService(String name) {
		name = name.substring(0,1).toLowerCase() + name.substring(1);
		return this."${name}Service"
	}

	void insertRecord(def service, Map row, boolean useDataBinding) {
		if(useDataBinding) {
			service.insertWithDataBinding(row)
		} else {
			service.insertWithSetter(row)
		}
	}

	@GrailsCompileStatic
	GrailsParameterMap toGrailsParamsMap(Map<String, String> map) {
		HttpServletRequest request = new MockHttpServletRequest()
		request.addParameters(map)
		GrailsParameterMap gmap = new GrailsParameterMap(request)
		return gmap
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
